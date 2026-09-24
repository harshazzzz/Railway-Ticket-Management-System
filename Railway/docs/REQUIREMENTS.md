# Coursework traceability

| Requirement | Evidence |
|---|---|
| Java 17+, NetBeans | Maven release 17, standard Maven project, README instructions |
| Swing GUI | MainFrame, Theme, screenshots |
| MySQL / JDBC | Database configuration, schema.sql, setup.sql, JdbcRepository |
| MVC | view, controller, service, repository, model packages; DESIGN.md |
| Encapsulation | User private final fields, immutable records, service mutation boundary |
| Inheritance / abstraction | Abstract sealed User with Admin and Passenger |
| Polymorphism | Payment.authorize, User.dashboardTitle |
| CRUD | Train create/read/update/retire; user create/read/update/deactivate |
| Authentication | BCrypt login, passenger registration, admin bootstrap, sessions |
| Role access | RailService.require and ownership checks |
| Search / seats | RailService.search/seats, dated journeys and seat map |
| Booking / history | RailService.book/bookings/ticket; transactional allocation |
| Payment / cancellation | CardPayment/CashPayment, atomic refund and ledger |
| Reports | Admin overview and payment ledger |
| Secure SQL / resources | Parameter binding, try-with-resources, safe exceptions |
| Tests | 21 passing service tests; MySQL CI supplied but not locally run |
| Diagrams | ClassDiagram.png, DatabaseDiagram.png |
| Report | ProjectReport.pdf and editable ProjectReport.md |
| GitHub | README, .gitignore, workflow, GITHUB.md |
| Viva | VIVA.md and readable formatted source |

Ticket Officer was explicitly optional and is not included. Database availability is derived per schedule rather than stored on a train; this deliberate refinement prevents different journey dates from sharing one seat count. Delete operations on historical entities are soft deletes. Payments are simulations.
