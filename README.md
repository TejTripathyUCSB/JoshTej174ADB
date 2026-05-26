# CS 174A Project — eMART + eDEPOT

Java + JDBC project for CS 174A, Spring 2026.

## One-time setup per developer

1. Clone this repo.
2. Get the wallet folder from a teammate (don't commit it). Place it somewhere outside the repo, or inside but `.gitignore`'d.
3. Create `db.properties` in the project root (gitignored — see template below).
4. Make sure you have Java 21+ and Maven installed.

### `db.properties` template

```
wallet.path=/absolute/path/to/Wallet_JoshTej174A
db.name=joshtej174a_tp
db.user=ADMIN
db.password=<the shared DB password>
```

## Database setup

Run the SQL scripts in SQL Developer connected to the shared Oracle DB:

1. `sql/03_drop.sql`  — clears all tables (safe to run on a fresh DB).
2. `sql/01_schema.sql` — creates tables, sequences, and seeds the rules table.
3. `sql/02_seed.sql`   — inserts sample manufacturers, products, and a test customer.

Hit **Commit** (F11) after each script. Without committing, changes don't persist.

## Build & run

```
mvn compile
mvn exec:java
```

## Project structure

```
.
├── pom.xml
├── db.properties              (gitignored)
├── README.md
├── sql/
│   ├── 01_schema.sql
│   ├── 02_seed.sql
│   └── 03_drop.sql
└── src/main/java/org/yourcompany/yourproject/
    ├── DB.java                (connection helper)
    ├── ProjectName.java       (entry point)
    ├── CartService.java       (eMart)
    ├── OrderService.java      (eMart)
    ├── ProductService.java    (eMart)
    └── (eDepot services go here once written)
```

## Data model

- `item` is the shared truth for stock_number / manufacturer / model_number / inventory.
- `emart_products` is the catalog's sales-side view of an item (category / warranty / price).
- All queries that need manufacturer or model JOIN `item` rather than denormalizing.

## Roles

- **eMart developer:** product search, cart, checkout, manager interface (Tej)
- **eDepot developer:** inventory, shipping notices, replenishment orders (Josh)
