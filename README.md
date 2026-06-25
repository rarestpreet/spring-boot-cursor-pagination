# Product Feed — Cursor-Based Pagination

A Spring Boot backend that serves a paginated, filterable product feed of **200,000 products**, built around *
*cursor-based pagination** to guarantee fast, consistent results even as data changes in real time.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Running Locally](#running-locally)
- [Live Deployment](#live-deployment)
- [API Reference](#api-reference)
- [Pagination — All Scenarios Explained](#pagination--all-scenarios-explained)
- [Database Indexes](#database-indexes)
- [Seed Script](#seed-script)
- [Simulation](#simulation)
- [Creator Notes](#creator-notes)

---

## Tech Stack

| Layer    | Technology                                                               |
|----------|--------------------------------------------------------------------------|
| Backend  | Spring Boot 4, Java 21                                                   |
| Database | MySQL 8.0+                                                               |
| ORM      | Spring Data JPA / Hibernate                                              |
| Frontend | Vanilla HTML + CSS + JavaScript (served as Spring Boot static resources) |

---

## Running Locally

### Prerequisites

- Java 21+
- MySQL 8.0+
- Maven

### Steps

**1. Set the database password environment variable**

```bash
# Windows (PowerShell)
$env:DB_PASS = "your_mysql_password"

# Linux / macOS
export DB_PASS=your_mysql_password
```

> The `DB_HOST` (default: `localhost`) and `DB_PORT` (default: `3306`) environment variables are also configurable.
> See [`application.yaml`](src/main/resources/application.yaml).

**2. Choose a data population strategy**

- **Static Pre-population (Default):** Run the `seed.sql` script (as described in the [Seed Script](#seed-script) section) to populate the database before starting the application. This ensures all 200,000 products exist prior to user interaction.
- **Dynamic Simulation:** Add the `@Scheduled(fixedRate = 4000)` annotation to the `addProducts()` method in `Simulation.java` to dynamically insert batches of products during the session. This is useful for verifying that real-time additions do not cause duplicates or skipped items.

**3. Run the application**

```bash
./mvnw spring-boot:run
```

Hibernate will automatically validate or update the schema. Ensure the database exists before running. For the initial run, configuring `spring.jpa.hibernate.ddl-auto: update` in `application.yaml` will generate the tables and indexes automatically.

**4. Access the frontend**

```
http://localhost:8080/index.html
```

---

## Live Deployment

> **Live URL:** `https://spring-boot-cursor-pagination.onrender.com`

The frontend is accessible directly at:

```
https://spring-boot-cursor-pagination.onrender.com
```

The API is reachable at:

```
https://spring-boot-cursor-pagination.onrender.com/product/generate-feed
```

---

## API Reference

### `GET /product/generate-feed`

Returns a page of products and an opaque cursor for the next page.

#### Query Parameters

| Parameter | Type       | Default      | Description                                                                                                                                       |
|-----------|------------|--------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| `cursor`  | `string`   | `""` (empty) | Base64-encoded cursor from the previous response. Empty string means "start from the beginning".                                                  |
| `limit`   | `int`      | `20`         | Page size. Clamped to `[10, 50]`.                                                                                                                 |
| `filter`  | `Category` | `NONE`       | Category to filter by. One of: `NONE`, `ELECTRONICS`, `CLOTHING`, `BOOKS`, `BEAUTY`, `SPORTS`, `HEALTH`. `NONE` means no filter (all categories). |

#### Response

```json
{
  "productList": [
    {
      "id": 195043,
      "name": "ELECTRONICS-product-000001",
      "category": "ELECTRONICS",
      "price": 4729.83,
      "createdAt": "2025-09-12T14:30:00Z",
      "updatedAt": "2026-01-04T09:15:00Z"
    }
  ],
  "encodedCursor": "eyJsYXN0U2Vlbklk..."
}
```

- `encodedCursor` is `null` when there are no more products to fetch.
- Pass `encodedCursor` as the `cursor` param in the next request to get the next page.

---

## Pagination — All Scenarios Explained

The cursor is a **Base64-encoded JSON object** (to prevent manipulation by client) carrying four pieces of state:

```json
{
  "lastSeenId": 195043,
  "lastSeenCreatedAt": "2025-09-12T14:30:00Z",
  "snapShotTime": "2026-06-25T04:00:00Z",
  "category": "NONE"
}
```

The `snapShotTime` is the key to consistency — it is captured at the moment the user opens a feed and never changes
throughout that browse session or till filter stays same.

---

### Scenario 1 — First Page, No Filter (`cursor=""`, `filter=NONE`)

A fresh browse session with no category filter.

- `snapShotTime` is captured as `NOW()`.
- Queries:

    ```sql
    WHERE createdAt <= snapShotTime
    ORDER BY createdAt DESC, id DESC LIMIT
    ```

- Any products inserted after the snapshot was taken are **invisible for this session**.

---

### Scenario 2 — Subsequent Pages, No Filter (`cursor=<token>`, `filter=NONE`)

The user scrolls down — the frontend sends the cursor from the previous response.

- `snapShotTime` is **reused from the cursor** (not recaptured).
- Queries with a compound keyset condition:
  ```sql
  WHERE createdAt <= snapShotTime
  AND (
    createdAt < lastSeenCreatedAt
    OR (createdAt = lastSeenCreatedAt AND id < lastSeenId)
  )
  ORDER BY createdAt DESC, id DESC
  ```
- The `(createdAt, id)` compound condition handles **ties** on `createdAt` correctly — products with the same timestamp
  are still ordered deterministically by `id`.
- Because `snapShotTime` is fixed, 50 new products added mid-browse will **never appear** in the current session —
  guaranteeing no duplicate entries and no skipped entries.

---

### Scenario 3 — First Page, With Filter (`cursor=""`, `filter=ELECTRONICS`)

Same as Scenario 1 but scoped to a category.

- `snapShotTime` captured fresh.
- Queries:

    ```sql
    WHERE category = 'ELECTRONICS' AND createdAt <= snapShotTime
    ORDER BY createdAt DESC, id DESC
    ```

---

### Scenario 4 — Subsequent Pages, With Filter (`cursor=<token>`, `filter=ELECTRONICS`)

Same as Scenario 2 but with category condition added.

- `snapShotTime` reused from cursor.
- Full compound keyset + category filter applied.

---

### Scenario 5 — Filter Change Mid-Session

The user switches from `ELECTRONICS` to `CLOTHING` (or to "All Products") while browsing.

- The service detects that `cursor.category != filter`.
- Treats this as a **new session**: discards the old cursor, captures a fresh `snapShotTime`, and starts from page 1 for the new filter.
- The old session's snapshot is abandoned — no stale state leaks across filter changes.

---

### Scenario 6 — End of Feed

When a query returns an empty list, the response is:

```json
{ "productList": [], "encodedCursor": null }
```

The frontend stops requesting further pages when `encodedCursor` is `null`.

---

### Scenario 7 — Data Added While Browsing (The Core Guarantee)

Suppose 50 new products are inserted between page 1 and page 2 of a browse session:

1. Page 1 was fetched at time `T` → `snapShotTime = T`.
2. 50 new products are inserted with `createdAt > T`.
3. Page 2 is fetched → query applies `WHERE createdAt <= T`.
4. The 50 new products are **outside the snapshot window** — they are invisible.
5. The user sees a perfectly stable, consistent feed with no duplicates and no gaps.

---

## Database Indexes

Two composite indexes are defined on `product_data` to make pagination queries hit an **index range scan** rather than a
full table scan:

| Index Name                         | Columns                      | Used By                             |
|------------------------------------|------------------------------|-------------------------------------|
| `idx_products_created_id`          | `(created_at, id)`           | Scenarios 1 & 2 — no filter queries |
| `idx_products_category_created_id` | `(category, created_at, id)` | Scenarios 3 & 4 — filtered queries  |

Both indexes match the `ORDER BY createdAt DESC, id DESC` and `WHERE` clauses of their respective queries exactly.

---

## Seed Script

📄 **File:** [`seed.sql`](seed.sql) click to check the script

```bash
# Run from the command line
mysql -u root -p cursor_pagination_db < seed.sql
```

The seed script inserts **200,000 products in a single SQL statement** using a recursive CTE — no application-side loop,
no round-trips:


> **When to use this:** The seed script is intended for **pre-populating the database before any user interaction begins
** — for example, setting up a demo environment or a fresh production instance. It seeds 200,000 rows with realistic,
> spread-out `created_at` timestamps spanning the past year and varied prices.

> It is **not** needed during normal operation. If the database already has data, running the script again will append
> another 200,000 rows.

---

## Simulation

📄 **File:** [
`src/main/java/com/project/cursor_pagination/service/Simulation.java`](src/main/java/com/project/cursor_pagination/service/Simulation.java) click to check the scheduled method

`Simulation.java` is a Spring `@Component` that inserts random batches of 500–5,000 products into the database in a single call (capped at 200,000 total insertions per run). This can be configured to run automatically using Spring's `@Scheduled` annotation to simulate real-time data addition during user interaction.

> **When to use this:** The simulation is intended specifically for **testing the pagination consistency guarantee while data is actively changing**. By calling `addProducts()` while a user is simultaneously browsing the feed, one can verify that the snapshot mechanism correctly prevents duplicates and skipped entries in the live feed.

> This is a developer/QA tool — it is **not** a seed script and is not meant as the primary way to populate data. For bulk pre-seeding, use [`seed.sql`](seed.sql).

---

## Creator Notes

### Why Cursor Pagination?

Three strategies were evaluated — **offset**, **keyset**, and **cursor (snapshot-keyset)** — and cursor pagination was
chosen for the following reasons:

**Over offset pagination:**

Offset pagination (`LIMIT n OFFSET k`) forces the database to scan and discard the first `k` rows on every request. With
200,000 products, fetching page 5,000 means scanning 100,000 rows just to throw them away. Performance degrades linearly
with page depth. Additionally, the interface uses **infinite scroll**, not numbered pages — offset is designed for
numbered pages and has no natural mapping to a "load more" scroll pattern.

**Over plain keyset pagination:**

Plain keyset (`WHERE id < lastSeenId`) is fast and index-friendly, but it lacks a **snapshot window**. If products are
added while a user is browsing, new rows shift into positions that were already served, causing duplicates or gaps. For
a product feed where simultaneous additions are expected, this is unacceptable.

**Cursor pagination (snapshot-keyset) was chosen because:**

- The `snapShotTime` is captured once when the user opens the feed and embedded in every subsequent cursor.
- All queries filter `WHERE createdAt <= snapShotTime`, creating a stable **time-bounded window**.
- The compound keyset `(createdAt, id)` handles ties deterministically and keeps every query on the index.
- The result: no `OFFSET` scan waste, no data drift mid-browse, and O(log n) query time at any page depth.

---

### What Would Be Improved With More Time

**1. `hasNext` flag in the response**

Currently, the only signal that a feed has ended is `encodedCursor: null`, which is returned after the last page comes
back *empty*. This means the client always makes one extra "empty" request to discover the end. A proper `hasNext`
boolean — computed by fetching `limit + 1` rows and returning only `limit` — would allow the frontend to know there is
more data before the user scrolls to the very bottom.

**2. Response caching**

Multiple users browsing the same category at the same snapshot moment will trigger identical queries. A cache layer (
e.g., Redis with a short TTL, or Spring's `@Cacheable`) keyed on `(cursor, limit, filter)` would serve repeated requests
from memory, significantly reducing database load under concurrent usage.

**3. Cursor expiry / TTL**

Cursors are currently valid indefinitely. A cursor that is hours old represents a snapshot window far in the past, which
could return confusingly stale data. Adding an expiry timestamp to the cursor and returning a `410 Gone` or a fresh
first-page response when the cursor is too old would give better UX and avoid unnecessarily wide index scans.

**4. Configurable category list**

Categories are currently a hardcoded Java enum. Adding a `categories` table and making the enum dynamic (or replacing it
with a string-based system) would allow categories to be added or renamed without a redeployment.

**5. API rate limiting**

There are no safeguards against a single client hammering the feed endpoint. Adding token-bucket rate limiting per IP (
e.g., via Spring's `RateLimiter` or a gateway-level solution) would protect the database under abuse scenarios.

---

### How AI Was Used — And What It Got Wrong

**What AI helped with:**

- **Simplifying conditional logic:** The original branching in `ProductService` was more verbose. AI suggested collapsing the session-detection conditions into a single `isNewSession` boolean, which made the four query paths (first/next × filtered/unfiltered) cleaner and easier to reason about.

- **Research and learning:** AI served as a sounding board for understanding the trade-offs between offset, keyset, and snapshot-cursor pagination — explaining where each breaks down at scale and why snapshot isolation matters for live feeds.

- **Idea validation:** Before implementing the snapshot approach, the design was discussed with AI to pressure-test the logic — specifically, verifying that reusing `snapShotTime` across pages rather than recapturing it on each request was the correct choice. AI confirmed the reasoning and explained edge cases.

- **Generating the seed script:** AI suggested using `seed.sql` after the developer discussed requiring pre-loaded data before user interaction. Initially, the developer had proposed dynamic data generation—either via an API endpoint called to generate and add data when the user scrolls down (load-more data) or via a scheduled background method. However, to satisfy the requirement of having a populated database prior to interaction, the AI proposed a recursive CTE in SQL to insert 200,000 rows instantly, which was accepted.

**What AI got wrong (that was caught):**

- **The data pre-population discussion**: Explicitly documented the reasoning behind the final pre-population approach. The developer initially suggested generating data dynamically, either through an API endpoint triggered during scrolling or through a scheduled method. After reading the requirements, developer decided that the dataset needed to exist before any user interaction began and proposed a batch data insertion into DB after generating 200K record at once, the AI proposed using a standalone seed.sql script with recursive CTEs. The final design keeps seed.sql as the explicit, inspectable pre-population mechanism, while Simulation.java remains separate from the committed seed data flow.
