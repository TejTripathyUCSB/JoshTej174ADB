# SYSTEM DESIGN — eMART + eDEPOT (defense / review doc)

A study sheet for the "why did you design it this way?" portion of the demo. Everything here is grounded
in the actual `sql/01_schema.sql` DDL and the service code.

---

## 1. The one-paragraph pitch

We built **two cooperating systems over one Oracle database**: **eMART** (the online store — catalog,
customers, carts, orders, pricing rules) and **eDEPOT** (the warehouse — physical inventory, shipping
notices, replenishment). They meet at a single shared spine table, **`item`**, which is the *only* place a
product's manufacturer, model, physical quantity, and warehouse location live. eMART's catalog
(`emart_products`) is a **1:1 extension** of `item` that adds retail attributes (price, category, warranty).
A customer places an order in eMART (status `PENDING`, **no stock moved**); eDEPOT later *fills* it
(`fillOrder` decrements `item.quantity`, flips it to `FILLED`). The Java layer is a thin JDBC service per
concern; a Swing GUI wraps each service in a tab, gated by a manager flag. Business policy (discounts,
shipping, status thresholds) is **data, not code** — it lives in the `emart_rules` table.

---

## 2. Table catalog (what each table is for)

### Shared lookup tables (no prefix — used by both systems)
- **`manufacturer(name)`**, **`shipping_company(name)`**, **`category(name)`** — single-column reference
  tables. Their job is **referential integrity**: every `item.manufacturer_name`, every notice's shipping
  company, every product category must already exist here. Prevents free-text typos and lets us enumerate
  valid values.

### The bridge
- **`item`** — *one row per product TYPE*, conceptually owned by eDEPOT (eDEPOT assigns the
  `stock_number` on first receipt). Columns: `stock_number` (PK, `CHAR(7)`), `manufacturer_name`,
  `model_number`, `quantity`, `min_stock`, `max_stock`, `location`, `replenishment`.
  **This is the only place manufacturer + model live.** Everything that needs them JOINs here.

### eMART catalog
- **`emart_products`** — sales-side info **only**: `category`, `warranty` (months), `price NUMBER(10,2)`.
  PK `stock_number` is *also* a FK to `item` → strict **1:1** (a product is a sellable `item`).
- **`emart_product_attributes(stock_number, attribute_name, attribute_value)`** — multi-valued descriptive
  attributes (RAM, weight, processor speed…). One row per attribute → keeps the catalog in **1NF**.
- **`emart_compatibility(stock_number, compatible_stock_number)`** — *directional*: "`stock_number` **can
  replace** `compatible_stock_number`." A self-relationship on products.

### eMART customers / carts / orders
- **`emart_customers`** — login + profile + `status` (`NEW/GREEN/SILVER/GOLD`) + `is_manager` (`T/F`).
- **`emart_cart_items(customer_id, stock_number, quantity)`** — the live cart; PK is the pair, so one row
  per (customer, product); re-adding accumulates quantity.
- **`emart_orders`** — order header with **snapshotted money**: `subtotal, discount, shipping_fee, total`,
  plus `fulfillment_status` (`PENDING/FILLED`) and `filled_date`. `order_id` is an **IDENTITY** column.
- **`emart_order_items(order_id, stock_number, quantity, price_each)`** — order lines, with
  **`price_each` snapshotted** at checkout time.

### eMART rules
- **`emart_rules(rule_name, rule_value)`** — 8 tunable business constants (seeded in the schema file itself):
  `NEW_CUSTOMER_DISCOUNT_PERCENT=10`, `GOLD_DISCOUNT_PERCENT=10`, `SILVER_DISCOUNT_PERCENT=5`,
  `GREEN_DISCOUNT_PERCENT=0`, `SHIPPING_PERCENT=10`, `FREE_SHIPPING_THRESHOLD=100`,
  `SILVER_STATUS_THRESHOLD=100`, `GOLD_STATUS_THRESHOLD=500`.

### eDEPOT
- **`edepot_shipping_notices`** — inbound shipment headers from a manufacturer: `status` (`PENDING/RECEIVED`),
  `notice_date`, `received_date`.
- **`edepot_shipping_notice_items(notice_id, stock_number, quantity)`** — what's on the way (FK → `item`).
- **`edepot_replenishment_orders(order_id, manufacturer_name, order_date)`** — outbound restock orders we
  send to a manufacturer; `order_id` like `RO00001`.
- **`edepot_replenishment_items(order_id, stock_number, quantity)`** — restock lines (FK → `item`).

### Sequences
`stock_number_seq` (new `ED#####` items), `replenishment_order_seq` (`RO#####`), `shipping_notice_seq`.

---

## 3. Why the `item` ↔ `emart_products` split? (the most likely question)

**Answer:** separation of ownership and avoidance of update anomalies.

- `item` is **warehouse truth** (how many do we physically have, where, reorder thresholds). eDEPOT writes it.
- `emart_products` is **store truth** (what we sell it as, for how much, with what warranty/category). eMART writes it.
- Manufacturer + model are stored **once**, in `item`. If they lived in both tables we'd risk an *update
  anomaly* (rename a model in one place, forget the other). eMART queries that need manufacturer/model JOIN
  to `item` — e.g. `displayCart`, `displayOrder`, and the monthly report all `JOIN item i`.
- The relationship is **1:1 enforced by making `emart_products.stock_number` both its PK and a FK to
  `item`** — you cannot sell something the warehouse doesn't know about, and a product can't exist twice.
- It also models reality: the warehouse may stock an `item` that isn't (yet) listed for sale; eDEPOT can
  create items (`ED#####`) via shipping notices before eMART ever lists them.

---

## 4. Normalization & key design (BCNF talking points)

- **`item`** has *three* candidate keys: `stock_number` (PK), `(manufacturer_name, model_number)` (UNIQUE),
  and `location` (UNIQUE). Every non-key attribute depends on the whole key → no partial/transitive
  dependencies → **BCNF**.
- **Multi-valued attributes are decomposed** (`emart_product_attributes`) instead of stuffing
  "RAM=512Mb;Weight=24.7lb" into one column → satisfies **1NF** and makes attribute search a clean
  equality query.
- **Compatibility is its own relation** (a many-to-many self-relationship on products) rather than a repeating
  column → 1NF, and lets us add/remove pairs without touching product rows.
- **`emart_rules`** externalizes policy: discount %, shipping %, and tier thresholds are *data*. Changing the
  GOLD threshold is an `UPDATE`, not a recompile. (Trade-off: rules are untyped name/value pairs, so a
  missing rule is a runtime error — `getRule` throws `Rule not found`.)
- **Deliberate denormalization for correctness, not laziness:**
  - `emart_order_items.price_each` snapshots the price at purchase. A later `changePrice` must **not**
    rewrite history; past orders keep the price the customer actually paid.
  - `emart_orders` stores `subtotal/discount/shipping_fee/total` rather than recomputing on display. The
    financial record is immutable even if rules later change.

---

## 5. Integrity = constraints, not application code

We pushed correctness into the schema so the DB rejects bad states regardless of which client writes:

| Constraint | Table | Guarantees |
|---|---|---|
| `chk_stock_num_format` | item | stock # is exactly `^[A-Z]{2}[0-9]{5}$` (2 letters + 5 digits = `CHAR(7)`) |
| `chk_location_format` | item | location is letter + non-zero-leading number |
| `chk_qty_leq_max`, `chk_qty_nonneg` | item | `0 ≤ quantity ≤ max_stock` (fill can't oversell; receive can't exceed capacity) |
| `chk_max_geq_min` | item | thresholds are sane |
| `UNIQUE(location)` | item | one item per bin |
| `UNIQUE(manufacturer_name, model_number)` | item | no duplicate product types |
| `chk_customer_status` | emart_customers | status ∈ {NEW,GREEN,SILVER,GOLD} |
| `chk_cart_qty_pos`, `chk_oi_qty_pos` | cart / order items | quantity > 0 |
| `chk_fulfillment_status` | emart_orders | status ∈ {PENDING,FILLED} |
| `chk_notice_status` | shipping notices | status ∈ {PENDING,RECEIVED} |
| FKs everywhere | all | can't reference a non-existent customer / product / manufacturer |
| `ON DELETE CASCADE` | attributes, compat, cart, order_items, notice_items, replen_items | deleting a parent cleans up children |

**Demo tie-in:** these CHECKs are *why* two of our "silent failures" happen — adding an unknown stock #
violates the cart→product FK, and setting status `PLATINUM` violates `chk_customer_status`. The DB refuses;
the app swallows the exception (see §8).

---

## 6. Transactions & concurrency

Every multi-step write turns **autocommit off** and is **atomic** (commit at the end, rollback on any error):

- **`checkout`** (OrderService): one transaction does — compute subtotal → read status & rules → insert
  order header → copy cart rows into `order_items` → clear cart → recompute customer status → **commit**.
  If anything fails, the whole thing rolls back: no half-created order, no half-cleared cart.
- **`fillOrder`** (InventoryService): reads order status (must be `PENDING`); locks each needed item row
  with **`SELECT … FOR UPDATE`** (pessimistic lock); validates **all** lines have enough stock *before*
  decrementing *any*; decrements; flips order to `FILLED`; runs replenishment; **commits**. The
  validate-all-then-write-all order means a shortfall on line 2 doesn't leave line 1 already decremented.
- **`receiveShipment`** (ShippingNoticeService): locks the notice and its items `FOR UPDATE`, checks
  `max_stock` ceiling and replenishment underflow, increments quantity / decrements replenishment, marks
  `RECEIVED`. Idempotent via the status guard.

**Idempotency by status guard:** `fillOrder` refuses anything not `PENDING` ("already FILLED");
`receiveShipment` refuses an already-`RECEIVED` notice. Clicking twice is safe.

**Isolation story:** `FOR UPDATE` serializes concurrent fills/receives on the same rows, so two managers
can't double-decrement the same stock.

---

## 7. The two signature algorithms

### 7a. Customer status state machine (recomputed every checkout)
After inserting an order, `updateCustomerStatus` recomputes from scratch:
```
recentTotal = SUM(subtotal) of this customer's LAST 3 orders   -- by order_date DESC, order_id DESC
orderCount  = COUNT(*) of this customer's orders
if orderCount == 0:          NEW
elif recentTotal > 500:      GOLD     -- GOLD_STATUS_THRESHOLD
elif recentTotal > 100:      SILVER   -- SILVER_STATUS_THRESHOLD
elif recentTotal > 0:        GREEN
else:                        NEW
```
- **Recent-activity-based, not lifetime.** A SILVER customer who buys one cheap thing can be **demoted** to
  GREEN (demo R12). This is intentional: status reflects current spending behavior.
- `NEW` is both the initial value and the "no spend" floor.
- **Discount tiers are separate** (`*_DISCOUNT_PERCENT`): NEW 10, GOLD 10, SILVER 5, GREEN 0. NEW and GOLD
  share 10%, but **NEW also always ships free** → a NEW customer can pay *less* than GOLD on a small order.

### 7b. Auto-replenishment (runs inside `fillOrder`, same transaction)
```
-- Trigger: a manufacturer with 3+ SKUs currently below their min
SELECT manufacturer_name FROM item
 WHERE quantity < min_stock
 GROUP BY manufacturer_name HAVING COUNT(*) >= 3;

-- For each such manufacturer, order every SKU below capacity:
SELECT ... FROM item WHERE manufacturer_name = ? AND quantity < max_stock;
amount_to_order = max_stock - (quantity + replenishment);   -- only insert if > 0
-- write edepot_replenishment_orders (RO#####) + edepot_replenishment_items
```
- The trigger is **per-manufacturer, count-based** (3+ low SKUs), not per-item — it models "this supplier is
  running us dry, do one consolidated PO." In this seed **only HP has 3 SKUs** (AA00101/AA00501/AA00601),
  so auto-replenishment can only ever fire for HP.
- Running it **inside** `fillOrder`'s transaction means the restock PO and the fill that caused the dip
  commit together (or roll back together).
- **Manual path:** `ManagerService.sendManufacturerOrder` writes the same two tables (and shares
  `replenishment_order_seq`), but is manager-initiated and validates every line belongs to the stated
  manufacturer.

---

## 8. Honest trade-offs / known limitations (own these before the grader points them out)

1. **No stock reservation at checkout.** Checkout creates a `PENDING` order without touching inventory; only
   `fillOrder` decrements. *Pro:* checkout is simple and never blocks on warehouse locks; multiple customers
   can order freely. *Con:* you can place an order that can't be filled — the fill then fails and rolls back
   atomically (demo R17). A production system would reserve stock or use optimistic checks.
2. **Swallowed exceptions → silent GUI "Done.".** `CartService.addItem` and `ManagerService.changePrice` /
   `updateCustomerStatusManually` catch exceptions and `printStackTrace()` to **stderr**. With our
   stdout-capturing GUI, a constraint violation shows `Done.` with no error (demo R8/B3). *Why it's like
   this:* those methods return `void` and were written console-first. *Fix:* propagate the exception (like
   the eDEPOT services do) so the GUI shows `ERROR:`.
3. **Replenishment doesn't bump `item.replenishment`.** `generateReplenishment` writes only the
   `edepot_replenishment_*` tables, so the on-order amount isn't reflected on the item, and the trigger can
   re-fire on the next fill. Only the inbound shipping-notice flow adjusts `item.replenishment`. *Known gap;*
   the clean fix is to increment `replenishment` when the PO is created and decrement on receipt.
4. **Inconsistent search ordering.** `searchCombined` and the product listing have `ORDER BY stock_number`;
   attribute and compatible-item searches don't, so their row order is Oracle's choice.
5. **Plaintext passwords** in `emart_customers.password` — fine for a course prototype, not production.
6. **Untyped rules table.** Flexible, but a missing/misnamed rule is a runtime `Rule not found`, not a
   compile error.

---

## 9. Money / formatting model (so a rounding question doesn't trip you)

- Money columns are `NUMBER(10,2)`. The app computes discount/shipping in Java `double`, then `setDouble`s
  into those columns → Oracle stores 2-dp (HALF_UP).
- **Checkout / displayOrder / report** print with Java `printf("%.2f")`, which is **HALF_UP** — matches the
  column rounding, so display and stored value agree (e.g. discount 3.4995 → `3.50`).
- **Search / cart display / inventory** print the **raw `getDouble`** value (shortest round-trip double):
  `$1630.0`, `$239.0`, `$839.97`. No `%.2f` on these paths. Same number, two representations — know which
  path produced the string. (`emart_products.price` is `NUMBER(10,2)`; read as a double, `1630.00` becomes
  the double `1630.0` and prints `1630.0`.)

---

## 10. Application architecture (Java/Swing layer)

- **One service class per concern**, each owning its own JDBC: `ProductService`, `CartService`,
  `OrderService`, `InventoryService`, `ShippingNoticeService`, `ReplenishmentService`, `ManagerService`,
  `AuthService`. `DB.getConnection()` centralizes the Oracle wallet connection.
- **GUI = thin wrappers.** Each tab calls a service inside `GuiTaskRunner.run(...)` (runs on a SwingWorker so
  the UI doesn't freeze) and renders captured stdout via `ConsoleCapture`. Thrown exceptions become
  `ERROR: <msg>`; blank stdout becomes `Done.`.
- **Authorization:** `AuthService.login` returns a session; `AppFrame` always shows **Products** and
  **Cart/Checkout**, and adds **Orders/eDEPOT**, **Shipping**, and **Manager** tabs only when
  `is_manager = 'T'`. In the seed, that's **Swong** and **Tcodd**.
- **Two entry points:** `gui.GuiMain` (the demo app, `exec.mainClass`) and `ProjectName.java` (a console
  integration test that runs the full eMART→eDEPOT flow end-to-end).

---

## 11. Entity-relationship cheat sheet

```
manufacturer ─1:N─ item ─1:N─ edepot_shipping_notice_items
            └─1:N─ edepot_shipping_notices                 (also ─FK→ shipping_company)
            └─1:N─ edepot_replenishment_orders ─1:N─ edepot_replenishment_items ─FK→ item

item ─1:1─ emart_products ─1:N─ emart_product_attributes
                          └─M:N (directional)─ emart_products   (emart_compatibility)
                          └─FK← emart_cart_items, emart_order_items
category ─1:N─ emart_products

emart_customers ─1:N─ emart_cart_items
                └─1:N─ emart_orders ─1:N─ emart_order_items
```
- **PENDING→FILLED** is the only state transition on an order; **PENDING→RECEIVED** on a notice.
- The eMART/eDEPOT boundary is exactly the `item ↔ emart_products` 1:1 edge plus the `fillOrder` action that
  reads an eMART order and writes eDEPOT inventory.

---

## 12. Rapid-fire Q&A (likely grilling)

- **Q: Why is `order_id` IDENTITY but `RO#####`/`ED#####` use sequences + formatting?**
  Orders need a simple surrogate PK; replenishment orders and warehouse items need a *human-readable typed
  code* (prefix + zero-padded sequence), so we format `seq.NEXTVAL` into `RO%05d` / `ED%05d`.
- **Q: Where does manufacturer/model live and why?** Only in `item`, to avoid update anomalies; eMART JOINs.
- **Q: Why doesn't checkout reduce inventory?** Store vs warehouse separation — ordering and fulfillment are
  distinct steps; `fillOrder` is the warehouse action and is idempotent.
- **Q: How do you prevent double-filling / double-receiving?** Status guards (`PENDING`/`RECEIVED`) plus
  `SELECT … FOR UPDATE` row locks inside a transaction.
- **Q: How is a customer's tier decided?** Recompute on each checkout from the **last 3 orders' subtotal sum**
  against `SILVER_STATUS_THRESHOLD (100)` / `GOLD_STATUS_THRESHOLD (500)`; can promote *or* demote.
- **Q: Why a rules table?** Business policy as data → change discounts/shipping/thresholds without redeploying.
- **Q: Why snapshot `price_each`?** So changing a product's price later doesn't alter historical orders.
- **Q: What makes the schema BCNF?** Every determinant is a candidate key; multi-valued and many-to-many facts
  are split into their own relations (attributes, compatibility).
- **Q: Biggest known weakness?** No reservation at checkout + silently-swallowed constraint errors on the
  eMART write paths (we surface them on the eDEPOT side). Both have clear fixes (§8).
