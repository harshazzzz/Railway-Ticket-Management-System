# Design decisions

## Structure and MVC

App assembles Database, JdbcRepository, RailService and MainFrame. The view collects input and renders components. AppController moves slow tasks off the event-dispatch thread. RailService implements use cases and transactions. Repository helpers own binding, generated keys and resource cleanup. The repository and clock are injected through constructors. Domain records are immutable; User remains a traditional abstract class to make encapsulation and inheritance explicit.

Booking is a denormalized read model for table rendering. The underlying database remains normalized. This small application uses one cohesive application service rather than a large framework or many one-method services.

## Database

The brief's suggested available_seats column on trains would mix travel dates. Instead, trains store fleet metadata, schedules store dates/fare/capacity, and seat_allocations represent current occupancy. Availability is schedule capacity minus active allocations. Capacity is copied when a schedule is created, so later fleet capacity edits cannot invalidate existing seat numbers.

Bookings reference schedules; their train is derived rather than duplicated. Tickets preserve seat, fare and UUID reference. Payments store settlement status; payment_transactions retain charges and refunds. application_locks holds a sentinel row for serialized first-admin bootstrap, not a business entity.

Foreign keys preserve relationships. Unique booking foreign keys enforce one ticket and payment per booking. A primary key on (schedule_id, seat_number) prevents duplicate occupancy. Cancellation deletes the active allocation, never the historic ticket. Unique (payment_id, transaction_type) prevents repeated charge/refund entries.

Train schedules cannot overlap. Published schedules are immutable. Train editing is allowed only before booking history exists, preserving historic names/routes. Retirement requires future confirmed bookings to be cancelled first and prevents new reservations. User deletion means deactivation. Administrators cannot be deactivated in this version.

## Atomic booking

1. Require a live passenger session and re-read account status.
2. Begin a READ_COMMITTED transaction.
3. Find train ID; lock active train, then schedule with SELECT FOR UPDATE.
4. Recheck departure, seat bounds and occupancy.
5. Insert booking, ticket and allocation using the fare from the database.
6. Invoke Payment.authorize. A simulated decline throws and rolls back all writes.
7. Insert payment and charge ledger entry, then commit.

The train lock coordinates booking with retirement and historical edit protection. It is deliberately coarse: journeys of the same train serialize. The schedule lock and unique allocation key provide further protection. This favors understandable correctness over maximum throughput.

Cancellation locks schedule then booking, verifies ownership/admin and departure, changes status, releases the seat and appends a full refund in one transaction. A repeated request is a no-op. Cancellation never locks train, avoiding a reverse lock dependency. There is one seat per booking and no pending hold.

A real payment provider must not be called inside this database transaction. External charges cannot be undone by JDBC rollback; production requires a reservation/payment state machine, idempotency and reconciliation.

## Security

Passwords use BCrypt cost 12, minimum 10 characters and maximum 72 UTF-8 bytes. Arrays are cleared, although BCrypt requires a Java String that cannot be reliably erased. Hashes never enter public User objects. Unknown logins use a dummy hash. Validation checks text lengths, email, distinct stations, time order, capacity and exact decimal fares.

All user data uses prepared parameters. SQL identifiers are fixed in source. Connections, statements and result sets use try-with-resources. Safe exceptions do not expose credentials, URLs or SQL. Sessions use service-issued tokens, logout revokes them, and each protected operation re-reads the account. Passengers cannot call administrative methods or access another passenger's bookings/tickets.

These safeguards assume a trusted local runtime. Anyone who has shared database credentials can bypass the Java layer with a modified client; public deployment needs a backend API. Rate limiting and password recovery are future work.

## UI and testing

Navy navigation and teal primary actions establish a consistent visual hierarchy. Fields have associated labels, focus indicators remain enabled and tables are read-only. Seat buttons show occupancy; payment/ticket labels state that they are simulations. SwingWorker runs JDBC/BCrypt off the UI thread. A page generation counter prevents stale asynchronous results rendering after logout/navigation.

Service integration tests run through JDBC with isolated H2 databases. Two workers compete for one seat. An injected future clock checks departure rules. MySQL can run the same suite on a disposable schema. The test evidence explicitly separates executed checks from supplied CI/manual checks.
