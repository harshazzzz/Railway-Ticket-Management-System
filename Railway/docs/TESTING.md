# Testing evidence

## Executed locally

- 21 JUnit service tests: **21 passed, 0 failures, 0 errors, 0 skipped**.
- Tests execute real JDBC transactions against fresh H2 2.3.232 databases in MySQL compatibility mode.
- Java 23.0.1 compiled source with `--release 17`; a Java 17 runtime was not installed locally.
- Maven produced the shaded runnable JAR with MySQL, H2, BCrypt and FlatLaf dependencies.
- Actual Swing screens were rendered using `ScreenshotCapture`, real services and an isolated seeded database, then inspected visually.
- The coursework PDF and diagrams were rendered and checked for layout/readability.

The Windows sandbox required a temporary short drive mapping for Java path resolution. This is an environment workaround, not part of the project setup. Surefire also ran successfully in-process with `-DforkCount=0`. The normal portable command is `mvn clean verify`.

See `test-evidence/` for the local Surefire summary and a sanitized list of test cases. It deliberately omits machine-specific properties and environment paths from the original XML.

## Coverage

| Area | Cases |
|---|---|
| Login | Correct credentials, wrong credentials, missing account, normalization |
| Registration | BCrypt persistence, cleared password array, duplicate email |
| Validation | Email, BCrypt byte limit, money precision, route/capacity, SQL injection payloads |
| Authorization | Role checks, logout, inactive account, cross-user ticket/cancellation denial |
| CRUD | Train/user create/read/update/soft-delete, retained history |
| Schedule | Overlapping departure rejection, time order, capacity snapshot |
| Booking | Complete persistence, seat bounds, duplicate seat, concurrent contenders |
| Payment | Decline rolls back every write, charge/refund ledger balance |
| Cancellation | Idempotent refund, seat reuse, admin cancellation |
| Time | Departed trains cannot be booked or cancelled |
| Bootstrap | Second admin rejected |

## MySQL verification supplied, not run locally

No MySQL server or Docker was available. The GitHub workflow provisions MySQL 8.4 and executes the same suite after H2. H2 compatibility mode is not a substitute for that check.

For a local MySQL run, provision a disposable `railway_test` schema and set `RAIL_TEST_DB_URL`, `RAIL_TEST_DB_USER`, `RAIL_TEST_DB_PASSWORD`. The test user needs DDL permissions. Run `mvn test`. Records in that schema are deleted before every test; URLs naming another schema are rejected. Do not use production/application data.

## Manual acceptance checklist

These remain user-acceptance scenarios rather than claimed automated coverage:

1. Register a new passenger; verify invalid and duplicate inputs show clear errors.
2. Log in; search an empty date and a populated date; navigate by keyboard.
3. Open seat map; check occupied seats are disabled; simulate decline, then approval.
4. Open the resulting ticket; cancel and verify the released seat through another account.
5. Admin: add/edit a train, publish a departure, add/edit/deactivate a passenger.
6. Try retiring a train with a future booking and editing a booked train; verify rejection.
7. Stop MySQL temporarily; verify actionable connection errors, then restore it.
8. Run two app instances against MySQL and race for the same seat.
9. Verify layouts at the minimum supported window size and operating-system display scaling.

## Reproduce screenshots

```sh
mvn test-compile
mvn exec:java -Dexec.mainClass=com.railway.ScreenshotCapture -Dexec.classpathScope=test
```

This utility requires a graphical desktop and writes only demo screenshots. It creates its database in memory. CI intentionally does not invoke it on a headless machine.
