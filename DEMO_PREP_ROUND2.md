# DEMO PREP — ROUND 2 (Hard / Trick Cases)

**Purpose:** 20 harder edge-case questions to drill before the live demo, each with the
**exact** expected output (hand-traced against the source + verified with HALF_UP / IEEE-754).
Plus a bonus quick-hit set, a "gotchas to flag" section, and a running-state appendix.

> Every dollar figure here was verified. `%.2f` paths use **HALF_UP** rounding (Java `printf`).
> Cart/product/inventory paths print **raw Java doubles** (e.g. `$1630.0`, `$239.0`, `$839.97`).

---

## 0. Setup & ground rules (READ FIRST)

1. **Start from a FRESH seed reset.** Re-run `sql/01_schema.sql` then `sql/02_seed.sql` so the DB
   matches the sample data exactly. The numbers below assume the seed state with **no prior orders/notices**.
2. **Run the questions IN ORDER.** Order IDs (`#1..#7`) and inventory are **cumulative** —
   skipping a step changes later results.
3. **Logins:** password = customer_id (e.g. user `Swong`, pass `Swong`).
   **Managers (see Orders / eDEPOT / Shipping / Manager tabs): `Swong` and `Tcodd` ONLY.**
   Everyone else (Lkim, Djones, Mramirez, Tpatel, Bford, Pchen, Jgray, Dknuth) is a plain customer
   and only sees **Products** + **Cart/Checkout**.
4. **Today's date is 2026-05-28** (matters for the Manager report's default Year=2026, Month=5).
5. **GUI output conventions** (so you can diff precisely):
   - Each action prints a header line `>>> <Action Title>` first, then the service output.
   - If a service throws, the area shows **`ERROR: <message>`** (Manager "Send Order" shows **`Failed: <message>`** instead).
   - If a service prints **nothing** to stdout, the area shows **`Done.`** ← this is the silent-failure tell.
   - The expected blocks below show the **service output** (the diff-worthy part).

---

## PART A1 — Product Search traps (no DB changes; log in as any customer, e.g. `Bford`)

### R1. Category that doesn't exist
**Do:** Products tab → Category field = `Tablet` → **Search Category**.
**Expected:**
```
No products found.
```
**Why:** Empty result set → friendly fallback, not a blank/error.

---

### R2. Manufacturer with exactly one product
**Do:** Manufacturer field = `Canon` → **Search Manufacturer**.
**Expected:**
```
AA00602 | Camera | Canon | L738 | $329.99 | warranty 1 mo
```
**Why:** Confirms the product-line format and that warranty months print bare (`1 mo`, not `1.0`).

---

### R3. Model search (exact model string)
**Do:** Model field = `C3958` → **Search Model**.
**Expected:**
```
AA00202 | Desktop | eMachines | C3958 | $369.99 | warranty 12 mo
```

---

### R4. Attribute search returns two rows, **unordered**
**Do:** Attr field = `Weight`, Value field = `24.7 lb` → **Search Attribute**.
**Expected (set — order is NOT guaranteed, no `ORDER BY` in this query):**
```
AA00601 | Camera | HP | K435 | $119.99 | warranty 3 mo
AA00602 | Camera | Canon | L738 | $329.99 | warranty 1 mo
```
**Why / trick:** The attribute query has **no `ORDER BY`**, so row order is whatever Oracle returns
(typically insertion order). Grade on the **set**, not the order. (If a grader expects sorted output,
this is a known design choice — see Gotchas.)

---

### R5. Compatible-items DIRECTIONALITY trap (returns nothing)
**Do:** Compat. Mfr = `Dell`, Model = `B420` → **Search Compatible**.
**Expected:**
```
No products found.
```
**Why / trick:** Compatibility is **directional**. `AA00201` (Dell B420) is only ever a *target*
("can be replaced by"), never a *source* ("can replace"). Searching its compatible items finds nothing.
This LOOKS like a bug but is correct given the seed data.

---

### R6. Compatible-items that DO resolve (raw doubles, unordered)
**Do:** Compat. Mfr = `McAfee`, Model = `G2005` → **Search Compatible**.
**Expected (set; no `ORDER BY` — order may vary):**
```
AA00101 | Laptop | HP | A6111 | $1630.0 | warranty 12 mo
AA00201 | Desktop | Dell | B420 | $239.0 | warranty 12 mo
AA00202 | Desktop | eMachines | C3958 | $369.99 | warranty 12 mo
```
**Why / trick:** `AA00402` (McAfee G2005) *can replace* the three machines above. Note the **raw doubles**:
`$1630.0` and `$239.0` (NOT `$1630.00`). Search/listing paths do **not** use `%.2f`.

---

### R7. Combined search: AND semantics → empty, then single criterion
**Do (a):** Combined Search with Category = `Camera` AND Manufacturer = `Samsung`.
**Expected (a):**
```
No products found.
```
**Do (b):** Combined Search with Category = `Camera` only (clear the Manufacturer field).
**Expected (b) — this query DOES have `ORDER BY stock_number`:**
```
AA00601 | Camera | HP | K435 | $119.99 | warranty 3 mo
AA00602 | Camera | Canon | L738 | $329.99 | warranty 1 mo
```
**Why / trick:** Combined search ANDs the criteria — no item is both Camera *and* Samsung, so (a) is empty.
Unlike R4/R6, **Combined Search is sorted** by stock number. (If all fields are blank it instead prints
`Please provide at least one search criterion.`)

---

## PART A2 — Cart traps (log in as the named customer)

### R8. Add an invalid stock number → SILENT failure (the big gotcha)
**Do:** Log in as `Bford`. Cart tab → Stock # = `ZZ99999`, Qty = `1` → **Add**. Then **Display Cart**.
**Expected (Add):**
```
Done.
```
**Expected (Display Cart):**
```
Cart for customer Bford:
Cart is empty.
```
**Why / trick:** `ZZ99999` is a *well-formed* stock number but doesn't exist → the cart insert fails a
**foreign-key** constraint → the exception is caught and the stack trace goes to the **terminal (stderr)**,
NOT the GUI. stdout stays blank → GUI shows `Done.` with **no error**. The item was **never added**.
Always confirm with **Display Cart**. (See Gotchas — this is the #1 thing to call out.)

---

### R9. MERGE accumulates quantity on repeat add
**Do:** Log in as `Pchen`. Add `AA00302` Qty `1` → **Add**. Then add `AA00302` Qty `2` → **Add**. Then **Display Cart**.
**Expected (Display Cart):**
```
Cart for customer Pchen:
AA00302 | Samsung E712 | qty=3 | price=$279.99 | line=$839.97
Subtotal: $839.97
```
**Why / trick:** Adding the same SKU again does a `MERGE` **UPDATE** (`quantity = quantity + new`), so
1 + 2 = **3**, not two separate rows. `line` is computed in SQL (`qty*price`) → exact `839.97`.

---

### R10. Remove an item that isn't in the cart
**Do:** Still `Bford` (empty cart). Stock # = `AA00101` → **Remove**.
**Expected:**
```
Item was not in cart.
```
**Why:** `DELETE` affected 0 rows → distinct message (not an error, not silent).

---

### R11. Checkout an empty cart
**Do:** Still `Bford` (empty cart) → **Checkout**.
**Expected:**
```
Cart is empty. Cannot checkout.
```
**Why:** Subtotal computes to 0 → checkout aborts and **rolls back**; no order row is created.

---

## PART A3 — Checkout math + status state machine (THE money section)

> **Headline trick (same item, three customers, three totals):** one `AA00301` ($69.99) costs
> **$62.99** for a NEW customer, **$69.99** for GOLD, **$73.49** for SILVER. The NEW free-shipping perk
> beats GOLD's bigger discount. Prove it with R12 / R13 / R14.

### R12. SILVER checkout → also DEMOTES to GREEN
**Do:** Log in as `Djones` (SILVER). Add `AA00301` Qty `1` → **Checkout**.
**Expected (this creates order `#1`):**
```
Checkout complete.
Order ID: 1
Subtotal: $69.99
Discount: $3.50
Shipping: $7.00
Total:    $73.49
```
**Status side-effect:** Djones **SILVER → GREEN**. (Status = sum of last-3 order *subtotals* = `69.99`,
which is `>0` but `≤100` → GREEN. A small purchase can DEMOTE you — status is recent-activity-based, not lifetime.)
- Discount = 69.99 × 5% = 3.4995 → **3.50** (HALF_UP).
- Shipping charged: subtotal 69.99 ≤ 100 and status ≠ NEW → 69.99 × 10% = 6.999 → **7.00**.
- Total = 69.99 − 3.4995 + 6.999 = 73.4895 → **73.49**.

---

### R13. NEW customer → free shipping
**Do:** Log in as `Tpatel` (NEW). Add `AA00301` Qty `1` → **Checkout**.
**Expected (order `#2`):**
```
Checkout complete.
Order ID: 2
Subtotal: $69.99
Discount: $7.00
Shipping: $0.00
Total:    $62.99
```
**Status side-effect:** Tpatel **NEW → GREEN** (last-3 subtotal 69.99).
- NEW discount = 10% → 6.999 → **7.00**. NEW always ships free → **0.00**. Total 62.991 → **62.99**.

---

### R14. NEW → GOLD in one order, then GOLD pays shipping on a cheap item
**Do (a):** Log in as `Mramirez` (NEW). Add `AA00101` Qty `1` → **Checkout**.
**Expected (order `#3`):**
```
Checkout complete.
Order ID: 3
Subtotal: $1630.00
Discount: $163.00
Shipping: $0.00
Total:    $1467.00
```
**Status:** Mramirez **NEW → GOLD** (last-3 subtotal 1630 > 500). Shipping free here because subtotal > 100.

**Do (b):** Still `Mramirez` (now GOLD). Add `AA00301` Qty `1` → **Checkout**.
**Expected (order `#4`):**
```
Checkout complete.
Order ID: 4
Subtotal: $69.99
Discount: $7.00
Shipping: $7.00
Total:    $69.99
```
**Why / trick:** Now GOLD with a sub-$100 cart → **GOLD pays shipping** (only NEW or subtotal>100 waives it).
GOLD discount 10% ($7.00) is exactly cancelled by shipping ($7.00) → **Total = Subtotal = $69.99**.
Compare to R13: identical item, NEW paid only **$62.99**. Status stays GOLD (last-3 = 1699.99).

---

### R15. GOLD bulk order + raw-double cart display
**Do:** Log in as `Lkim` (GOLD). Add `AA00101` Qty `3`. **Display Cart**, then **Checkout**.
**Expected (Display Cart — raw doubles!):**
```
Cart for customer Lkim:
AA00101 | HP A6111 | qty=3 | price=$1630.0 | line=$4890.0
Subtotal: $4890.0
```
**Expected (Checkout — `%.2f`! order `#5`):**
```
Checkout complete.
Order ID: 5
Subtotal: $4890.00
Discount: $489.00
Shipping: $0.00
Total:    $4401.00
```
**Why / trick:** Same numbers, two formats. Cart display = raw double `$4890.0`; checkout = `$4890.00`.
Be ready to explain *why the same value prints differently* (search/cart use `getDouble` directly; checkout/report use `printf %.2f`).
Subtotal > 100 → free shipping. Status stays GOLD.

---

## PART A4 — eDEPOT fill + replenishment (log in as `Swong`, a manager)

> First confirm `Swong` sees the manager-only tabs (**Orders/eDEPOT**, **Shipping**, **Manager**).
> A plain customer like `Bford` does NOT — good thing to show the grader.

### R16. Fill a non-existent order
**Do:** Orders/eDEPOT tab → Order ID = `999` → **Fill Order**.
**Expected:**
```
ERROR: Order 999 does not exist.
```
**Why:** Thrown `IllegalArgumentException` → surfaced as `ERROR: ...`.

---

### R17. Fill blocked by insufficient inventory (atomic rollback)
**Do:** Order ID = `5` (Lkim's `AA00101` ×3) → **Fill Order**.
**Expected:**
```
ERROR: Insufficient inventory for AA00101. Needed: 3, Available: 2.
```
**Why / trick:** Only **2** `AA00101` in stock; the order wants 3. The whole fill **rolls back** — order #5
stays PENDING and inventory is unchanged. **Checkout never reserved stock**, so you can over-sell into a
PENDING order that can't be filled. (Great design-discussion hook.)

---

### R18. Fill success + idempotency (double-fill refused)
**Do (a):** Order ID = `1` (Djones's `AA00301` ×1) → **Fill Order**.
**Expected (a):**
```
Filled order #1.
```
*(Inventory: `AA00301` 4 → 3.)*
**Do (b):** Order ID = `1` → **Fill Order** again.
**Expected (b):**
```
ERROR: Order 1 is already FILLED.
```
**Why:** `fillOrder` only acts on `PENDING` orders → safe to click twice; the second is refused.

---

### R19. Fill that does NOT trigger replenishment
**Do (a):** Log in as `Dknuth` (GOLD), add `AA00501` Qty `2` → **Checkout** (order `#6`).
**Expected (a):**
```
Checkout complete.
Order ID: 6
Subtotal: $599.98
Discount: $60.00
Shipping: $0.00
Total:    $539.98
```
**Do (b):** Log back in as `Swong` → Order ID = `6` → **Fill Order**.
**Expected (b):**
```
Filled order #6.
```
*(Inventory: `AA00501` 3 → 1, which is below its min_stock of 2.)*
**Why / trick:** `AA00501` is now below min, but replenishment needs a manufacturer with **≥3 items below
min**. Only **1** HP item is below min so far → **no replenishment order is created.** (Verify: no new row in
`edepot_replenishment_orders`.)

---

### R20. The fill that DOES trigger auto-replenishment (HP)
**Do (a):** Log in as `Dknuth`, add `AA00101` Qty `2` **and** `AA00601` Qty `2` → **Checkout** (order `#7`).
**Expected (a):**
```
Checkout complete.
Order ID: 7
Subtotal: $3499.98
Discount: $350.00
Shipping: $0.00
Total:    $3149.98
```
**Do (b):** Log back in as `Swong` → Order ID = `7` → **Fill Order**.
**Expected (b) — console shows ONLY this:**
```
Filled order #7.
```
*(Inventory: `AA00101` 2 → 0, `AA00601` 3 → 1.)*
**Why / trick:** After this fill, **three** HP items are below min — `AA00101` (0<1), `AA00501` (1<2),
`AA00601` (1<2) — so HP crosses the "≥3 below min" trigger and an auto-replenishment order fires
**silently** (nothing prints; only "Filled order #7." shows).

**Verify the replenishment via SQL (it is NOT visible in the GUI output):**
```sql
SELECT order_id, manufacturer_name FROM edepot_replenishment_orders ORDER BY order_id;
SELECT order_id, stock_number, quantity FROM edepot_replenishment_items ORDER BY order_id, stock_number;
```
**Expected:** one order `RO00001` for `HP`, with items:
```
RO00001 | AA00101 | 2     (max 2 − qty 0 − repl 0)
RO00001 | AA00501 | 3     (max 4 − qty 1 − repl 0)
RO00001 | AA00601 | 4     (max 5 − qty 1 − repl 0)
```
**Two sub-traps to flag:**
- Replenishment orders **every HP item below max_stock**, not just the ones below min (so `AA00501` is
  included even though it dipped below min back in R19).
- Auto-replenishment **does NOT bump `item.replenishment`** — "Check Inventory" on these SKUs still shows
  `replenishment = 0`. The on-order amount lives only in `edepot_replenishment_items`. (See Gotchas.)

---

## PART B2 — Bonus quick-hit trick cases (after R1–R20; minor/no state impact)

| # | Do (tab → action) | Exact expected output | Point |
|---|---|---|---|
| B1 | Manager → Stock# `ZZ99999`, Price `9.99` → **Change Price** | `Product not found.` | 0-row UPDATE → friendly message, no error |
| B2 | Manager → Stock# `AA00302`, Price `259.99` → **Change Price** | `Price changed for AA00302 to $259.99` | `%.2f`; updates `emart_products.price` |
| B3 | Manager → Customer `Lkim`, Status `PLATINUM` → **Set Status** | `Done.` | **SILENT FAIL** — CHECK constraint rejects PLATINUM; trace → terminal; status unchanged |
| B4 | Manager → Customer `Lkim`, Status `silver` → **Set Status** | `Customer Lkim status changed to SILVER` | input is upper-cased before write |
| B5 | Manager → Customer `Zzz`, Status `GOLD` → **Set Status** | `Customer not found.` | 0-row UPDATE |
| B6 | Manager → **Send Order** with no lines added | `No pending lines. Use Add Line first.` | guarded before service call |
| B7 | Manager → Mfr `HP`, Stock# `AA00101`, Qty `0` → **Add Line** | `Qty must be > 0.` | client-side validation in the tab |
| B8 | Manager → add line Mfr `HP`/`AA00101`/`1`, then add line Mfr `Dell`/`AA00201`/`1` → **Add Line** | `All lines must be for the same manufacturer (current: HP). Use Clear Lines to start a new order.` | one order = one manufacturer |
| B9 | Manager → Year `2026`, Month `4` → **Monthly Report** | see block ↓ | empty month → three fallbacks |
| B10 | Cart tab as `Tpatel` → Order ID `1` → **Display Order** | `Order not found.` | **ownership** — #1 is Djones's order, not Tpatel's |
| B11 | Cart tab as `Djones` → Order ID `1` → **Display Order** | full order #1 detail (he owns it) | positive ownership case |

**B9 expected (empty month):**
```

=== Monthly Sales Report: 2026-04 ===

Sales by product:
No product sales for this month.

Sales by category:
No category sales for this month.

Top customer:
No customer purchases for this month.
```

**B10 — ownership trick (the TA's requested restriction):** `displayOrder` (and `rerunOrder`) now match on
`order_id AND customer_id`, so a customer can only see/re-run **their own** orders. Tpatel asking for order #1
gets `Order not found.` even though the order exists. This is intentional and worth demoing both ways (B10 fail, B11 success).

> ⚠️ **B2/B3/B4 mutate state.** If you intend to also show the populated report (below) or re-diff inventory,
> run these *after* you've captured everything else — or just reset the seed afterward.

---

## STRETCH — populated Monthly Report for 2026-05
> Valid **only** if you ran exactly R1–R20 and did **not** run B2/B3/B4 first (those change prices/status).
> The report counts **all** orders in the month regardless of fill status (#1–#7 all count).

**Do:** Log in as `Swong` → Manager → Year `2026`, Month `5` → **Monthly Report**.
**Expected:**
```

=== Monthly Sales Report: 2026-05 ===

Sales by product:
AA00101 | HP A6111 | qty=6 | sales=$9780.00
AA00501 | HP J1320 | qty=2 | sales=$599.98
AA00601 | HP K435 | qty=2 | sales=$239.98
AA00301 | Envision D720 | qty=3 | sales=$209.97

Sales by category:
Laptop | qty=6 | sales=$9780.00
Printer | qty=2 | sales=$599.98
Camera | qty=2 | sales=$239.98
Monitor | qty=3 | sales=$209.97

Top customer:
Lkim | Linda Kim | spent=$4401.00
```
- AA00101 qty = 1(#3)+3(#5)+2(#7)=6 → 6×$1630 = $9780.00. AA00301 = 1(#1)+1(#2)+1(#4)=3 → $209.97.
- Top customer by `SUM(total)`: Lkim $4401.00 > Dknuth $3689.96 > Mramirez $1536.99. Both report sections sort by **sales DESC**.

---

## GOTCHAS TO FLAG (say these out loud before the grader finds them)

1. **Silent add-to-cart failure (R8).** Adding a non-existent stock # shows `Done.` with no error (FK
   violation → stack trace to terminal, not GUI). Mitigation: always confirm with **Display Cart**.
2. **Silent invalid-status failure (B3).** Setting a status not in `{NEW,GREEN,SILVER,GOLD}` shows `Done.`
   (CHECK constraint rejects it; trace to terminal). Status is unchanged.
3. **Raw doubles vs `%.2f` (R6/R15).** Search, cart display, and inventory print **raw `getDouble`**
   (`$1630.0`, `$839.97`); checkout, `displayOrder`, and the report use **`printf %.2f`** (`$1630.00`).
   Same value, two strings — know which path you're on.
4. **No stock reservation at checkout (R17).** Checkout creates a PENDING order but does **not** decrement
   inventory. Stock only moves on eDEPOT `fillOrder` / `receiveShipment`. So you can create an order you
   can't fill; the fill rolls back atomically.
5. **Compatibility is directional (R5).** Searching compatible items for a SKU that's only ever a *target*
   returns nothing. Not a bug.
6. **Search ordering is inconsistent (R4/R6 vs R7).** Attribute and compatible searches have **no
   `ORDER BY`**; combined/listing do. Grade unordered searches on the set.
7. **Auto-replenishment can only ever be HP (R20).** The "≥3 items below min for one manufacturer" trigger
   can only be met by HP in this seed (HP is the only mfr with 3 items: AA00101, AA00501, AA00601).
8. **Replenishment doesn't update `item.replenishment` (R20).** `generateReplenishment` only writes the
   `edepot_replenishment_*` tables; it never increments the item's `replenishment` column. Consequence:
   "Check Inventory" shows `replenishment = 0` after an auto-order, and the trigger can **re-fire** on the
   next fill because nothing records that an order is already outstanding. (Only the manual
   shipping-notice flow touches `item.replenishment`.) Be ready to explain this as a known limitation.
9. **Status is recent-activity-based, not lifetime (R12).** Status = sum of the **last 3** orders' subtotals,
   so a SILVER customer making a tiny purchase can be **demoted** to GREEN.
10. **Manager "Send Order" failures print `Failed:` not `ERROR:`** (the tab catches and re-prints). All other
    thrown errors show `ERROR:`.

---

## STATE APPENDIX (after running R1–R20 in order, before Part B2)

**Orders:**
| Order | Customer | Items | Total | Status |
|------|----------|-------|-------|--------|
| #1 | Djones | AA00301×1 | $73.49 | **FILLED** (R18) |
| #2 | Tpatel | AA00301×1 | $62.99 | PENDING |
| #3 | Mramirez | AA00101×1 | $1467.00 | PENDING |
| #4 | Mramirez | AA00301×1 | $69.99 | PENDING |
| #5 | Lkim | AA00101×3 | $4401.00 | PENDING (fill failed R17) |
| #6 | Dknuth | AA00501×2 | $539.98 | **FILLED** (R19) |
| #7 | Dknuth | AA00101×2, AA00601×2 | $3149.98 | **FILLED** (R20) |

**Inventory (`item.quantity`), seed → after R20:**
| Stock | min/max | Seed | After | Changed by |
|------|---------|------|-------|------------|
| AA00101 | 1/2 | 2 | **0** | fill #7 |
| AA00301 | 3/6 | 4 | **3** | fill #1 |
| AA00501 | 2/4 | 3 | **1** | fill #6 |
| AA00601 | 2/5 | 3 | **1** | fill #7 |
| (all others) | — | — | unchanged | — |

**Replenishment:** `RO00001` (HP) → AA00101×2, AA00501×3, AA00601×4. All `item.replenishment` columns still **0**.

**Customer status, seed → after R20:**
| Customer | Seed | After | Note |
|----------|------|-------|------|
| Djones | SILVER | **GREEN** | demoted (R12) |
| Tpatel | NEW | **GREEN** | R13 |
| Mramirez | NEW | **GOLD** | promoted (R14a), stays (R14b) |
| Lkim | GOLD | GOLD | unchanged |
| Dknuth | GOLD | GOLD | unchanged |
| Swong / Bford / Tcodd / Pchen / Jgray | — | unchanged | Pchen still has `AA00302×3` in cart (never checked out) |
