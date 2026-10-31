\# KeyFlow — Project State



Last updated: 2026-10-29. Branch: `feature/product-api`.



\## Overview

KeyFlow is an original college project: an automated digital-product distribution platform

(sellers, products, inventory, digital keys, orders, fulfillment). It is not a keyboard

automation tool. Repo root: `C:\\Users\\Shubham\\keyFlow`. Backend: `backend/keyFlow-api`.



\## Stack

Java 24, Spring Boot 4.1.2, Maven Wrapper, Spring Data JPA (Hibernate 7.4.13), Flyway,

PostgreSQL 17 (Docker, container `keyflow-postgres`), Jakarta Validation, JUnit 5, Mockito.



\## Architecture

Modular monolith, package-by-feature under `com.keyflow`:

`seller`, `product` (controller, dto, entity, repository, service), `common` (exception, response),

`config`. Flyway owns the schema; Hibernate runs with `ddl-auto=validate`.



\## Database

| Migration | Description | Status |

|---|---|---|

| V1 | create initial schema (sellers, products, inventory\_databases, digital\_keys) | Applied; never edit |



Known gap for Phase 3: `digital\_keys` has independent foreign keys to `products` and

`inventory\_databases`, so nothing guarantees both belong to the same seller. Fix with a new

migration before building allocation.



\## Implemented API (development only, unauthenticated)

`sellerId` is supplied by the client and is NOT proof of identity. Authentication arrives in Phase 4.



| Method | Path | Notes |

|---|---|---|

| POST | `/api/v1/products` | Create (starts as DRAFT). 201 + Location |

| GET | `/api/v1/products?sellerId=\&page=\&size=` | Paged, size max 100, sorted createdAt desc, id asc |

| GET | `/api/v1/products/{id}?sellerId=` | 404 if missing or owned by another seller |

| PATCH | `/api/v1/products/{id}?sellerId=` | Partial update of name/description; archived products rejected (409) |

| POST | `/api/v1/products/{id}/archive?sellerId=` | DRAFT/ACTIVE to ARCHIVED; repeat returns 409 |



Error shape: `timestamp, status, code, message, path, fieldErrors\[]`.



## Test status (actually executed)
- `.\mvnw.cmd clean test`: 43 tests, 0 failures, 0 errors (2026-10-31).
  - 1 context load, 7 error handler, 11 service (mocks), 17 controller (mocks)
  - 7 PostgreSQL-backed repository tests (`ProductRepositoryPostgresTest`, Testcontainers, postgres:17)
- Repository tests verify: database-generated createdAt, status stored as string, ownership query,
  seller-scoped paging and newest-first ordering, unique email, foreign key, and status check constraints.
- Testcontainers tests need Docker Desktop running. They never touch the dev database.



## Manual verification (2026-10-31, PowerShell script against the running dev app)
All results matched expectations:
- Create returned DRAFT; list showed newest-first ordering and correct paging fields; get returned the product.
- Partial update changed the name and kept the description.
- Error codes: other-seller get 404, empty patch 400, page size 101 400, list without sellerId 400,
  malformed id 400, blank name 400, unknown seller 404.
- Archive returned ARCHIVED; archiving again 409; editing an archived product 409.
- Earlier (2026-10-29): the database was confirmed to hold the renamed, archived product.



\## Environment notes

\- Start Docker Desktop first, then `docker compose up -d` (never `down -v`; it deletes data).

\- Timezone workaround: `$env:JAVA\_TOOL\_OPTIONS = "-Duser.timezone=Asia/Kolkata"`.

\- Dev seller: run with `$env:SPRING\_PROFILES\_ACTIVE = "dev"`; `DevSellerSeeder` ensures

&#x20; `dev-seller@keyflow.local` exists and logs its ID.

\- Maven prints HTTP 401 warnings for Flyway community repositories; harmless so far.

\- Mockito self-attach warning during tests; harmless on JDK 24 for now.

- Timezone: the JVM reports `Asia/Calcutta`, which PostgreSQL 17 rejects ("invalid value for parameter TimeZone").
  Fix per machine: `-Duser.timezone=Asia/Kolkata` as a VM option in the IntelliJ run configuration, or
  `$env:JAVA_TOOL_OPTIONS` in PowerShell. Tests started from IntelliJ need the same option.



\## How to run

```powershell

cd C:\\Users\\Shubham\\keyFlow

docker compose up -d

cd backend\\keyFlow-api

$env:JAVA\_TOOL\_OPTIONS = "-Duser.timezone=Asia/Kolkata"

$env:SPRING\_PROFILES\_ACTIVE = "dev"

.\\mvnw.cmd spring-boot:run          # app on http://localhost:8080

.\\mvnw.cmd clean test               # tests (unset SPRING\_PROFILES\_ACTIVE first)

```



\## Known limitations and security boundaries

\- No authentication or authorization; client-supplied `sellerId` is development-only.

\- No concurrency tests.

\- The `DataIntegrityViolationException` to 409 mapping is not yet tested through HTTP
  (the database constraints themselves are tested).

\- Status ACTIVE exists in the schema but no endpoint activates a product yet.



## Remaining work
1. Open a pull request for `feature/product-api` into `main`.
2. Phase 3: V2 migration so a digital key's product and inventory database belong to the same seller,
   then inventory database APIs, CSV import, encrypted key storage.

## Exact next step
Open the PR, then design the V2 same-seller integrity migration.

