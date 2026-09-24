# Viva guide

## Five-minute demonstration

1. Explain MVC and the distinction between train and schedule.
2. Sign in as passenger in demo mode; search tomorrow's departures.
3. Select a seat and simulate card decline; show that no booking was saved.
4. Retry with approval and open the ticket in My bookings.
5. Cancel; show the retained ticket and released seat.
6. Sign in as admin; show the charge/refund ledger and train/departure forms.
7. Explain concurrency and cross-account privacy tests.

## Questions to prepare

**Encapsulation?** User has private final fields and getters. Password hashes stay inside repository operations. Services guard changes.

**Inheritance?** Admin and Passenger share User identity and override role/dashboard behavior.

**Polymorphism?** MainFrame calls dashboardTitle through User. Booking calls authorize through Payment; card/cash implementations respond differently.

**Why records?** Immutable data reduces accidental mutation and boilerplate. User remains an abstract class to show inheritance clearly.

**Why MVC?** Layout does not contain SQL or booking rules. Controller tasks bridge the UI and service; backend tests do not need Swing.

**Double booking?** Train/schedule locks serialize contenders and a unique allocation key enforces one seat owner at the database level.

**BigDecimal?** Decimal currency avoids binary floating-point rounding. The service reads the fare from the DB rather than trusting UI input.

**Payment failure?** An exception before commit rolls back booking, ticket and allocation. Real payment requires compensation because external charges are outside MySQL's transaction.

**Why no available_seats counter?** Counting allocations avoids counter drift and isolates every dated departure.

**Deletion?** Users/trains are deactivated/retired to preserve history. Occupancy allocations are physically removed because they represent current state.

**Is hiding a button security?** No. Services check roles again; public deployment still needs an API because desktop DB credentials can be extracted.

**Why H2?** Fast isolated local tests. It is not identical to MySQL, so a separate MySQL suite/CI configuration is included. Claim only verified environments.

**Optional scope?** Ticket Officer, real payments, password recovery, multiple seats per booking, intermediate stops and pagination remain future work.

## Reading order

User -> Payment -> Database -> JdbcRepository -> RailService.book/cancel -> AppController -> MainFrame -> RailServiceTest.

Explain operations in your own words before submitting. Try extending one feature yourself, such as report date filters, and be ready to defend tradeoffs.
