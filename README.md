# Ishtirak — client-server billing for neighbourhood generator operators

CSIS231 project (Adriana Jneid). JavaFX client + Spring Boot server + PostgreSQL, Maven multi-module, Git.

```
ishtirak/
├── common/   shared DTO records and enums (org.example.ishtirak.common)
├── server/   Spring Boot REST API (org.example.ishtirak.server)
│   ├── controller  service  repository  model  mapper  exception  security
│   └── resources/db/migration (schema + users)   resources/db/seed (demo data)
└── client/   JavaFX MVC client (org.example.ishtirak.client), FXML in resources
```

The layout of each module follows the course sample (`api` and `javafx` projects): same package names,
record DTOs with Bean Validation, `@Component` mappers, constructor injection, Lombok entities,
`SceneManager` + `Launcher` + `xxx-view.fxml` files on the client.

## 1. Requirements

* JDK 17 or newer (built and tested with JDK 21). Do **not** keep a JDK copy inside the project folder.
* Maven 3.8+ (or use `./mvnw` / `mvnw.cmd`)
* PostgreSQL 14+ running on `localhost:5432`

## 2. Create the databases (once)

```sql
-- in psql or pgAdmin, as the postgres user
CREATE DATABASE ishtirak_db;     -- used by the application
CREATE DATABASE ishtirak_test;   -- used by the automated tests (real PostgreSQL, not H2)
```

Default credentials are `postgres` / `postgres`. If yours differ, set environment variables before running:
`DB_USERNAME`, `DB_PASSWORD` (and optionally `DB_URL`, `TEST_DB_URL`).

You do **not** create tables by hand: Flyway creates the 8 tables, constraints and seed data at server start.

## 3. Build and run

```bash
mvn clean install -DskipTests              # builds common, server, client
java -jar server/target/server-1.0-SNAPSHOT.jar
# wait for "Started IshtirakApplication", then in a second terminal:
cd client && mvn javafx:run
```

In IntelliJ: open the root `pom.xml`, run `IshtirakApplication` (server), then run the client with the Maven
goal `javafx:run` in the `client` module (or the `Launcher` class).

The client calls `http://localhost:8080` by default; change it with `-Dishtirak.api.url=http://host:port`.

### Accounts (seeded by `V2__seed_users.sql`, BCrypt hashes)

| Username  | Password     | Role                                   |
|-----------|--------------|----------------------------------------|
| admin     | admin123     | ADMIN — everything                     |
| collector | collector123 | COLLECTOR — subscribers (read), readings, invoices, payments |
| former    | disabled123  | disabled account (login gives 403 ACCOUNT_DISABLED) |

Change these passwords before any real use.

### Demo data (`db/seed/V100__demo_data.sql`, development database only)

3 zones (Ehden and Ardeh rural), 6 subscribers, 6 meters from July 2026, July and August readings
(one August reading missing on purpose), and three real 2026 ministry tariffs as reported by L'Orient Today:
Feb 30,244 · Jun 41,973 · Aug 48,241 LBP/kWh; fees 385,000 (5 A), 685,000 (10 A), +300,000 per extra 5 A, +10% rural.
A good first demo: log in as admin → Billing run → August 2026 → Generate invoices.

To start over with a clean demo database: drop and recreate `ishtirak_db`, then start the server.

## 4. Tests

```bash
mvn test        # needs PostgreSQL running and the ishtirak_test database
```

* `BillingCalculatorTest` — the formula alone: 5 A, 10 A, >10 A, rural, non-rural, first month, later month, rounding.
* `*IntegrationTest` — the real server on a random port, real HTTP + JSON, real PostgreSQL (`ishtirak_test`):
  every documented error code, idempotent billing run, partial unique index, row lock on concurrent payments,
  billed − collected = outstanding.
* Client tests — JSON contract with the server and connection-loss handling.

## 5. Decisions and differences from the sample, with reasons

1. **Flyway** instead of the sample's `ddl-auto`: Hibernate cannot generate the CHECK constraints or the partial
   unique index `invoice(reading_id) WHERE voided = false`; the proposal names Flyway. Hibernate runs in
   `validate` mode, so a mismatch between entities and schema stops the server at start.
2. **Login token**: the sample returns user data without a token. The proposal requires a token and 401/403 rules,
   so login returns an HMAC-signed token (no extra library, no extra table) checked by a Spring MVC
   `HandlerInterceptor`; `@AdminOnly` marks admin services. Spring Security was not added because the sample
   only uses `spring-security-crypto`.
3. **Global error handler** (`@RestControllerAdvice`): the sample has none (its `ResourceNotFoundException` would
   return 500). Every error now uses `{status, code, message, fieldErrors[], timestamp}`.
4. **`GET /api/zones` is open to collectors**: the readings screen (V5) needs the zone list; creating, editing
   and deleting zones stay admin only.
5. **Small additions**: `GET /api/invoices/{id}/payments` (payment history in V4), `GET /api/subscriptions/{id}`,
   codes `READING_ABOVE_NEXT` (a correction cannot exceed a later month's reading) and
   `SUBSCRIPTION_HAS_READINGS` (initial reading and start date are fixed once readings exist).
6. **Amperage changes** apply from the next month that is not yet invoiced: issued invoices keep their snapshot
   amount; the schema has no amperage history table.
7. **Tests use a real PostgreSQL test database** instead of the sample's H2.

## 6. Endpoints

| Service | Method and path | Role |
|---|---|---|
| login | POST /api/auth/login | all |
| zones | GET /api/zones · POST /api/zones · PUT/DELETE /api/zones/{id} | GET all, others admin |
| subscribers | GET /api/subscribers?zoneId&status&search&page&size · GET /api/subscribers/{id} | all |
| | POST /api/subscribers · PUT /api/subscribers/{id} · PATCH /api/subscribers/{id}/close | admin |
| subscriptions | POST /api/subscriptions · PUT /api/subscriptions/{id} · PATCH /api/subscriptions/{id}/end | admin |
| tariffs | GET/POST /api/tariffs · PUT /api/tariffs/{id} | admin |
| readings | GET /api/readings/sheet?month=yyyy-MM&zoneId · POST /api/readings/batch | all |
| billing | POST /api/billing/runs | admin |
| invoices | GET /api/invoices?month&subscriberId&status · GET /api/invoices/{id}/payments | all |
| | POST /api/invoices/{id}/void | admin |
| payments | POST /api/invoices/{id}/payments | all |
| reports | GET /api/reports/dashboard?month · GET /api/reports/arrears?zoneId | admin |
