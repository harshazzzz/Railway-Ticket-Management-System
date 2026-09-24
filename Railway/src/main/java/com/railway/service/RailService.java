package com.railway.service;

import static com.railway.repository.JdbcRepository.*;

import com.railway.model.*;
import com.railway.repository.JdbcRepository;
import com.railway.utils.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.mindrot.jbcrypt.BCrypt;

/** Application boundary: authentication, permissions and atomic business operations. */
public final class RailService {
  private final JdbcRepository repository;
  private final Map<UUID, Long> sessions = new ConcurrentHashMap<>();
  private final Clock clock;
  private static final String DUMMY_HASH =
      BCrypt.hashpw("unused-timing-password", BCrypt.gensalt(10));

  public RailService(JdbcRepository repository) {
    this(repository, Clock.systemDefaultZone());
  }

  public RailService(JdbcRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  private LocalDateTime now() {
    return LocalDateTime.now(clock);
  }

  private static <T> T first(List<T> rows, String message) {
    if (rows.isEmpty()) throw new AppException(message);
    return rows.get(0);
  }

  private static User mapUser(ResultSet r) throws SQLException {
    return r.getString("role").equals("ADMIN")
        ? new Admin(
            r.getLong("id"), r.getString("name"), r.getString("email"), r.getBoolean("active"))
        : new Passenger(
            r.getLong("id"), r.getString("name"), r.getString("email"), r.getBoolean("active"));
  }

  private User require(Connection c, Session session, String role) throws SQLException {
    Long id = session == null ? null : sessions.get(session.token());
    if (id == null) throw new AppException("Your session has ended. Please sign in again.");
    User user =
        first(
            query(c, "SELECT * FROM users WHERE id=? AND active=TRUE", RailService::mapUser, id),
            "Account is inactive. Please sign in again.");
    if (role != null && !role.equals(user.role()))
      throw new AppException("You do not have permission for this action.");
    return user;
  }

  public Session login(String email, char[] password) {
    String normalized = Validation.email(email);
    String secret;
    try {
      secret = Validation.password(password);
    } finally {
      Arrays.fill(password, '\0');
    }
    return repository.read(
        c -> {
          var rows =
              query(
                  c,
                  "SELECT * FROM users WHERE email=?",
                  r -> new Object[] {mapUser(r), r.getString("password_hash")},
                  normalized);
          String hash = rows.isEmpty() ? DUMMY_HASH : (String) rows.get(0)[1];
          boolean valid = BCrypt.checkpw(secret, hash);
          if (rows.isEmpty() || !valid || !((User) rows.get(0)[0]).isActive())
            throw new AppException("Email or password is incorrect.");
          Session session = new Session((User) rows.get(0)[0]);
          sessions.put(session.token(), session.user().getId());
          return session;
        });
  }

  public void logout(Session session) {
    if (session != null) sessions.remove(session.token());
  }

  public long register(String name, String email, char[] password) {
    return createAccount(name, email, password, "PASSENGER", false);
  }

  /** Administrative CLI bootstrap only; serialized against concurrent bootstrap attempts. */
  public long bootstrapAdmin(String name, String email, char[] password) {
    return createAccount(name, email, password, "ADMIN", true);
  }

  private long createAccount(
      String name, String email, char[] password, String role, boolean bootstrap) {
    String n = Validation.text(name, "Name", 100), e = Validation.email(email);
    String hash;
    try {
      hash = BCrypt.hashpw(Validation.password(password), BCrypt.gensalt(12));
    } finally {
      Arrays.fill(password, '\0');
    }
    return repository.transaction(
        c -> {
          if (bootstrap) {
            query(c, "SELECT id FROM application_locks WHERE id=1 FOR UPDATE", r -> r.getInt(1));
            if (!query(c, "SELECT id FROM users WHERE role='ADMIN'", r -> r.getLong(1)).isEmpty())
              throw new AppException("An administrator already exists. Use the existing account.");
          }
          return insert(
              c,
              "INSERT INTO users(name,email,password_hash,role) VALUES(?,?,?,?)",
              n,
              e,
              hash,
              role);
        });
  }

  public List<User> users(Session session) {
    return repository.read(
        c -> {
          require(c, session, "ADMIN");
          return query(c, "SELECT * FROM users ORDER BY id", RailService::mapUser);
        });
  }

  public void updateUser(Session session, long id, String name, String email, boolean active) {
    String n = Validation.text(name, "Name", 100), e = Validation.email(email);
    repository.transaction(
        c -> {
          User actor = require(c, session, "ADMIN");
          User target =
              first(
                  query(c, "SELECT * FROM users WHERE id=? FOR UPDATE", RailService::mapUser, id),
                  "User not found.");
          if (target instanceof Admin && !active)
            throw new AppException("Administrator accounts cannot be deactivated.");
          update(c, "UPDATE users SET name=?,email=?,active=? WHERE id=?", n, e, active, id);
          return actor.getId();
        });
  }

  public long addPassenger(Session session, String name, String email, char[] password) {
    repository.read(c -> require(c, session, "ADMIN"));
    return register(name, email, password);
  }

  private static Train mapTrain(ResultSet r) throws SQLException {
    return new Train(
        r.getLong("id"),
        r.getString("train_name"),
        r.getString("start_station"),
        r.getString("destination"),
        r.getInt("capacity"),
        r.getBoolean("active"));
  }

  public List<Train> trains(Session session) {
    return repository.read(
        c -> {
          require(c, session, "ADMIN");
          return query(c, "SELECT * FROM trains ORDER BY id", RailService::mapTrain);
        });
  }

  public long saveTrain(
      Session session, long id, String name, String origin, String destination, int capacity) {
    String n = Validation.text(name, "Train name", 100),
        o = Validation.text(origin, "Origin", 100),
        d = Validation.text(destination, "Destination", 100);
    if (o.equalsIgnoreCase(d)) throw new AppException("Origin and destination must differ.");
    Validation.capacity(capacity);
    return repository.transaction(
        c -> {
          require(c, session, "ADMIN");
          if (id == 0)
            return insert(
                c,
                "INSERT INTO trains(train_name,start_station,destination,capacity) VALUES(?,?,?,?)",
                n,
                o,
                d,
                capacity);
          first(
              query(
                  c,
                  "SELECT id FROM trains WHERE id=? AND active=TRUE FOR UPDATE",
                  r -> r.getLong(1),
                  id),
              "Active train not found.");
          // A booked route is historical evidence: do not silently rewrite existing tickets.
          if (!query(
                  c,
                  "SELECT b.id FROM bookings b JOIN schedules s ON b.schedule_id=s.id WHERE"
                      + " s.train_id=?",
                  r -> r.getLong(1),
                  id)
              .isEmpty())
            throw new AppException(
                "This train has booking history. Create a new train to change its route or"
                    + " details.");
          update(
              c,
              "UPDATE trains SET train_name=?,start_station=?,destination=?,capacity=? WHERE id=?",
              n,
              o,
              d,
              capacity,
              id);
          return id;
        });
  }

  public void deleteTrain(Session session, long id) {
    repository.transaction(
        c -> {
          require(c, session, "ADMIN");
          first(
              query(
                  c,
                  "SELECT id FROM trains WHERE id=? AND active=TRUE FOR UPDATE",
                  r -> r.getLong(1),
                  id),
              "Active train not found.");
          if (!query(
                  c,
                  "SELECT b.id FROM bookings b JOIN schedules s ON s.id=b.schedule_id WHERE"
                      + " s.train_id=? AND b.status='CONFIRMED' AND s.departure>?",
                  r -> r.getLong(1),
                  id,
                  now())
              .isEmpty())
            throw new AppException("Cancel future confirmed bookings before retiring this train.");
          update(c, "UPDATE trains SET active=FALSE WHERE id=?", id);
          return null;
        });
  }

  public long addSchedule(
      Session session,
      long trainId,
      LocalDateTime departure,
      LocalDateTime arrival,
      BigDecimal fare) {
    Validation.money(fare);
    if (departure == null
        || arrival == null
        || !departure.isAfter(now())
        || !arrival.isAfter(departure))
      throw new AppException("Departure must be in the future and arrival must follow departure.");
    return repository.transaction(
        c -> {
          require(c, session, "ADMIN");
          Train train =
              first(
                  query(
                      c,
                      "SELECT * FROM trains WHERE id=? AND active=TRUE FOR UPDATE",
                      RailService::mapTrain,
                      trainId),
                  "Active train not found.");
          if (!query(
                  c,
                  "SELECT id FROM schedules WHERE train_id=? AND departure<? AND arrival>?",
                  r -> r.getLong(1),
                  trainId,
                  arrival,
                  departure)
              .isEmpty()) throw new AppException("This train already has an overlapping journey.");
          return insert(
              c,
              "INSERT INTO schedules(train_id,departure,arrival,fare,capacity) VALUES(?,?,?,?,?)",
              trainId,
              departure,
              arrival,
              fare,
              train.capacity());
        });
  }

  public List<Schedule> search(Session session, String origin, String destination, LocalDate date) {
    if (date == null) throw new AppException("Choose a travel date.");
    String o = origin == null ? "" : origin.strip(),
        d = destination == null ? "" : destination.strip();
    return repository.read(
        c -> {
          require(c, session, null);
          return query(
              c,
              "SELECT s.id AS sid,s.departure,s.arrival,s.fare,s.capacity AS sc,t.*, (SELECT"
                  + " COUNT(*) FROM seat_allocations a WHERE a.schedule_id=s.id) AS occupied FROM"
                  + " schedules s JOIN trains t ON t.id=s.train_id WHERE t.active=TRUE AND"
                  + " s.departure>=? AND s.departure<? AND s.departure>? AND (?='' OR"
                  + " LOWER(t.start_station)=LOWER(?)) AND (?='' OR LOWER(t.destination)=LOWER(?))"
                  + " ORDER BY s.departure",
              r ->
                  new Schedule(
                      r.getLong("sid"),
                      mapTrain(r),
                      r.getTimestamp("departure").toLocalDateTime(),
                      r.getTimestamp("arrival").toLocalDateTime(),
                      r.getBigDecimal("fare"),
                      r.getInt("sc"),
                      r.getInt("sc") - r.getInt("occupied")),
              date.atStartOfDay(),
              date.plusDays(1).atStartOfDay(),
              now(),
              o,
              o,
              d,
              d);
        });
  }

  public List<Seat> seats(Session session, long scheduleId) {
    return repository.read(
        c -> {
          require(c, session, null);
          int capacity =
              first(
                  query(
                      c, "SELECT capacity FROM schedules WHERE id=?", r -> r.getInt(1), scheduleId),
                  "Schedule not found.");
          Set<Integer> taken =
              new HashSet<>(
                  query(
                      c,
                      "SELECT seat_number FROM seat_allocations WHERE schedule_id=?",
                      r -> r.getInt(1),
                      scheduleId));
          List<Seat> seats = new ArrayList<>();
          for (int n = 1; n <= capacity; n++) seats.add(new Seat(n, !taken.contains(n)));
          return seats;
        });
  }

  public long book(Session session, long scheduleId, int seat, Payment payment) {
    if (payment == null) throw new AppException("Choose a payment method.");
    return repository.transaction(
        c -> {
          User user = require(c, session, "PASSENGER");
          long trainId =
              first(
                  query(
                      c,
                      "SELECT train_id FROM schedules WHERE id=?",
                      r -> r.getLong(1),
                      scheduleId),
                  "Schedule not found.");
          // Consistent lock order: train -> schedule -> booking. Retiring a train uses the same
          // first lock.
          first(
              query(
                  c,
                  "SELECT id FROM trains WHERE id=? AND active=TRUE FOR UPDATE",
                  r -> r.getLong(1),
                  trainId),
              "Train is no longer available.");
          Object[] s =
              first(
                  query(
                      c,
                      "SELECT departure,capacity,fare FROM schedules WHERE id=? FOR UPDATE",
                      r ->
                          new Object[] {
                            r.getTimestamp(1).toLocalDateTime(), r.getInt(2), r.getBigDecimal(3)
                          },
                      scheduleId),
                  "Schedule not found.");
          if (!((LocalDateTime) s[0]).isAfter(now()))
            throw new AppException("This train has already departed.");
          if (seat < 1 || seat > (int) s[1]) throw new AppException("Choose a valid seat number.");
          if (!query(
                  c,
                  "SELECT booking_id FROM seat_allocations WHERE schedule_id=? AND seat_number=?",
                  r -> r.getLong(1),
                  scheduleId,
                  seat)
              .isEmpty())
            throw new AppException("That seat was just booked. Please choose another seat.");
          BigDecimal fare = (BigDecimal) s[2];
          long booking =
              insert(
                  c,
                  "INSERT INTO bookings(user_id,schedule_id,status) VALUES(?,?,'CONFIRMED')",
                  user.getId(),
                  scheduleId);
          insert(
              c,
              "INSERT INTO tickets(booking_id,seat_number,price,reference) VALUES(?,?,?,?)",
              booking,
              seat,
              fare,
              UUID.randomUUID().toString());
          update(
              c,
              "INSERT INTO seat_allocations(schedule_id,seat_number,booking_id) VALUES(?,?,?)",
              scheduleId,
              seat,
              booking);
          // A declined simulated payment rolls back every write above, including seat allocation.
          if (!payment.authorize(fare))
            throw new AppException("Simulated payment declined. No booking or charge was saved.");
          long paymentId =
              insert(
                  c,
                  "INSERT INTO payments(booking_id,amount,payment_method,payment_status)"
                      + " VALUES(?,?,?,'PAID')",
                  booking,
                  fare,
                  payment.method());
          insert(
              c,
              "INSERT INTO payment_transactions(payment_id,transaction_type,amount)"
                  + " VALUES(?,'CHARGE',?)",
              paymentId,
              fare);
          return booking;
        });
  }

  private static final String BOOKING_SQL =
      "SELECT"
          + " b.id,b.user_id,b.schedule_id,u.name,t.train_name,t.start_station,t.destination,s.departure,b.status,k.seat_number,k.price,k.reference,p.payment_status"
          + " FROM bookings b JOIN users u ON u.id=b.user_id JOIN schedules s ON s.id=b.schedule_id"
          + " JOIN trains t ON t.id=s.train_id JOIN tickets k ON k.booking_id=b.id JOIN payments p"
          + " ON p.booking_id=b.id";

  private static Booking mapBooking(ResultSet r) throws SQLException {
    return new Booking(
        r.getLong(1),
        r.getLong(2),
        r.getLong(3),
        r.getString(4),
        r.getString(5),
        r.getString(6),
        r.getString(7),
        r.getTimestamp(8).toLocalDateTime(),
        r.getString(9),
        r.getInt(10),
        r.getBigDecimal(11),
        r.getString(12),
        r.getString(13));
  }

  public List<Booking> bookings(Session session) {
    return repository.read(
        c -> {
          User u = require(c, session, null);
          return u instanceof Admin
              ? query(c, BOOKING_SQL + " ORDER BY b.id DESC", RailService::mapBooking)
              : query(
                  c,
                  BOOKING_SQL + " WHERE b.user_id=? ORDER BY b.id DESC",
                  RailService::mapBooking,
                  u.getId());
        });
  }

  public Ticket ticket(Session session, long bookingId) {
    return repository.read(
        c -> {
          User u = require(c, session, null);
          Booking b =
              first(
                  query(c, BOOKING_SQL + " WHERE b.id=?", RailService::mapBooking, bookingId),
                  "Booking not found.");
          if (!(u instanceof Admin) && u.getId() != b.userId())
            throw new AppException("You cannot view another passenger's ticket.");
          return first(
              query(
                  c,
                  "SELECT * FROM tickets WHERE booking_id=?",
                  r ->
                      new Ticket(
                          r.getLong("id"),
                          bookingId,
                          r.getInt("seat_number"),
                          r.getBigDecimal("price"),
                          r.getString("reference")),
                  bookingId),
              "Ticket not found.");
        });
  }

  public void cancel(Session session, long bookingId) {
    repository.transaction(
        c -> {
          User user = require(c, session, null);
          long sid =
              first(
                  query(
                      c,
                      "SELECT schedule_id FROM bookings WHERE id=?",
                      r -> r.getLong(1),
                      bookingId),
                  "Booking not found.");
          LocalDateTime departure =
              first(
                  query(
                      c,
                      "SELECT departure FROM schedules WHERE id=? FOR UPDATE",
                      r -> r.getTimestamp(1).toLocalDateTime(),
                      sid),
                  "Schedule not found.");
          Object[] b =
              first(
                  query(
                      c,
                      "SELECT user_id,status FROM bookings WHERE id=? FOR UPDATE",
                      r -> new Object[] {r.getLong(1), r.getString(2)},
                      bookingId),
                  "Booking not found.");
          if (!(user instanceof Admin) && user.getId() != (long) b[0])
            throw new AppException("You cannot cancel another passenger's booking.");
          if (b[1].equals("CANCELLED")) return null; // Idempotent: never refund twice.
          if (!departure.isAfter(now()))
            throw new AppException("Departed journeys cannot be cancelled.");
          update(c, "UPDATE bookings SET status='CANCELLED' WHERE id=?", bookingId);
          update(c, "DELETE FROM seat_allocations WHERE booking_id=?", bookingId);
          update(c, "UPDATE payments SET payment_status='REFUNDED' WHERE booking_id=?", bookingId);
          update(
              c,
              "INSERT INTO payment_transactions(payment_id,transaction_type,amount) SELECT"
                  + " id,'REFUND',amount FROM payments WHERE booking_id=?",
              bookingId);
          return null;
        });
  }

  public List<Transaction> transactions(Session session) {
    return repository.read(
        c -> {
          require(c, session, "ADMIN");
          return query(
              c,
              "SELECT * FROM payment_transactions ORDER BY id DESC",
              r ->
                  new Transaction(
                      r.getLong("id"),
                      r.getLong("payment_id"),
                      r.getString("transaction_type"),
                      r.getBigDecimal("amount"),
                      r.getTimestamp("created_at").toLocalDateTime()));
        });
  }
}
