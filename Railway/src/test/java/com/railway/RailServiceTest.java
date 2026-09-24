package com.railway;

import static org.junit.jupiter.api.Assertions.*;

import com.railway.database.Database;
import com.railway.model.*;
import com.railway.repository.JdbcRepository;
import com.railway.service.*;
import com.railway.utils.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;

class RailServiceTest {
  Database db;
  JdbcRepository repo;
  RailService service;
  Session admin, passenger, other;
  long train, schedule;
  final LocalDateTime departure = LocalDate.now().plusDays(2).atTime(10, 0);

  @BeforeEach
  void setup() {
    // An optional isolated MySQL test schema exercises exactly the same suite.
    String url = System.getenv("RAIL_TEST_DB_URL");
    if (url != null && !url.matches("jdbc:mysql://[^/]+/railway_test(?:\\?.*)?"))
      throw new IllegalArgumentException("Tests require a disposable database named railway_test.");
    db =
        url == null
            ? new Database(
                "jdbc:h2:mem:"
                    + UUID.randomUUID()
                    + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa",
                "")
            : new Database(
                url, System.getenv("RAIL_TEST_DB_USER"), System.getenv("RAIL_TEST_DB_PASSWORD"));
    db.initialize();
    repo = new JdbcRepository(db);
    if (url != null)
      repo.transaction(
          c -> {
            for (String table :
                List.of(
                    "payment_transactions",
                    "payments",
                    "seat_allocations",
                    "tickets",
                    "bookings",
                    "schedules",
                    "trains",
                    "users")) JdbcRepository.update(c, "DELETE FROM " + table);
            return null;
          });
    service = new RailService(repo);
    service.bootstrapAdmin("Admin", "admin@test.local", "AdminPass123!".toCharArray());
    service.register("Passenger", "passenger@test.local", "Passenger123!".toCharArray());
    service.register("Other", "other@test.local", "OtherPass123!".toCharArray());
    admin = service.login("admin@test.local", "AdminPass123!".toCharArray());
    passenger = service.login("passenger@test.local", "Passenger123!".toCharArray());
    other = service.login("other@test.local", "OtherPass123!".toCharArray());
    train = service.saveTrain(admin, 0, "Express", "A", "B", 4);
    schedule =
        service.addSchedule(
            admin, train, departure, departure.plusHours(2), new BigDecimal("1250.00"));
  }

  long count(String table) {
    return repo.read(
        c -> JdbcRepository.query(c, "SELECT COUNT(*) FROM " + table, r -> r.getLong(1)).get(0));
  }

  @Test
  void loginAndPolymorphism() {
    assertInstanceOf(Admin.class, admin.user());
    assertInstanceOf(Passenger.class, passenger.user());
    assertNotEquals(admin.user().dashboardTitle(), passenger.user().dashboardTitle());
  }

  @Test
  void passwordsAreHashedAndInputCleared() {
    char[] p = "NewPassword123!".toCharArray();
    service.register("New", "new@test.local", p);
    assertEquals('\0', p[0]);
    String hash =
        repo.read(
            c ->
                JdbcRepository.query(
                        c,
                        "SELECT password_hash FROM users WHERE email=?",
                        r -> r.getString(1),
                        "new@test.local")
                    .get(0));
    assertTrue(hash.startsWith("$2a$"));
    assertNotEquals("NewPassword123!", hash);
  }

  @Test
  void loginRejectsIncorrectAndMissingAccount() {
    assertThrows(
        AppException.class,
        () -> service.login("admin@test.local", "WrongPassword1!".toCharArray()));
    assertThrows(
        AppException.class,
        () -> service.login("missing@test.local", "WrongPassword1!".toCharArray()));
  }

  @Test
  void duplicateEmailAndNormalization() {
    assertThrows(
        AppException.class,
        () ->
            service.register("Duplicate", " PASSENGER@test.local ", "Passenger123!".toCharArray()));
    assertNotNull(service.login("PASSENGER@test.local", "Passenger123!".toCharArray()));
  }

  @Test
  void validationRejectsInvalidInputs() {
    assertThrows(AppException.class, () -> Validation.email("bad-email"));
    assertThrows(AppException.class, () -> Validation.password("short".toCharArray()));
    assertThrows(AppException.class, () -> Validation.password("x".repeat(73).toCharArray()));
    assertThrows(AppException.class, () -> Validation.money(new BigDecimal("1.001")));
    assertThrows(AppException.class, () -> service.saveTrain(admin, 0, "Bad", "A", "A", 0));
  }

  @Test
  void injectionIsLiteralData() {
    assertThrows(
        AppException.class,
        () -> service.login("x' OR '1'='1@test.local", "WrongPassword1!".toCharArray()));
    assertEquals(3, count("users"));
    assertTrue(service.search(passenger, "A' OR 1=1 --", "", departure.toLocalDate()).isEmpty());
  }

  @Test
  void roleAndSessionPermissions() {
    assertThrows(AppException.class, () -> service.trains(passenger));
    assertThrows(AppException.class, () -> service.saveTrain(passenger, 0, "Bad", "A", "B", 4));
    assertThrows(AppException.class, () -> service.users(passenger));
    assertThrows(AppException.class, () -> service.book(admin, schedule, 1, new CashPayment()));
    service.logout(passenger);
    assertThrows(AppException.class, () -> service.bookings(passenger));
  }

  @Test
  void deactivatedSessionIsRevoked() {
    service.updateUser(admin, passenger.user().getId(), "Passenger", "passenger@test.local", false);
    assertThrows(AppException.class, () -> service.bookings(passenger));
    assertThrows(
        AppException.class,
        () -> service.login("passenger@test.local", "Passenger123!".toCharArray()));
    assertThrows(
        AppException.class,
        () -> service.updateUser(admin, admin.user().getId(), "Admin", "admin@test.local", false));
  }

  @Test
  void userCrud() {
    long id = service.addPassenger(admin, "New", "new@test.local", "NewPassword123!".toCharArray());
    service.updateUser(admin, id, "Updated", "updated@test.local", true);
    assertTrue(
        service.users(admin).stream()
            .anyMatch(u -> u.getId() == id && u.getName().equals("Updated")));
    service.updateUser(admin, id, "Updated", "updated@test.local", false);
    assertFalse(
        service.users(admin).stream()
            .filter(u -> u.getId() == id)
            .findFirst()
            .orElseThrow()
            .isActive());
  }

  @Test
  void trainCrud() {
    service.saveTrain(admin, train, "Updated", "A", "C", 6);
    assertEquals("Updated", service.trains(admin).get(0).name());
    service.deleteTrain(admin, train);
    assertFalse(service.trains(admin).get(0).active());
    assertTrue(service.search(passenger, "", "", departure.toLocalDate()).isEmpty());
  }

  @Test
  void scheduleValidationAndSnapshot() {
    assertThrows(
        AppException.class,
        () ->
            service.addSchedule(
                admin, train, departure.plusMinutes(30), departure.plusHours(3), BigDecimal.TEN));
    assertThrows(
        AppException.class,
        () ->
            service.addSchedule(admin, train, departure, departure.minusHours(1), BigDecimal.TEN));
    service.saveTrain(admin, train, "Updated", "A", "B", 8);
    assertEquals(4, service.seats(passenger, schedule).size());
  }

  @Test
  void bookingPersistsTicketPaymentAndAvailability() {
    long id = service.book(passenger, schedule, 2, new CardPayment(true));
    assertEquals(1, count("bookings"));
    assertEquals(1, count("tickets"));
    assertEquals(1, count("payments"));
    assertEquals(1, count("payment_transactions"));
    assertEquals(2, service.ticket(passenger, id).seatNumber());
    assertEquals(
        3, service.search(passenger, "A", "B", departure.toLocalDate()).get(0).availableSeats());
  }

  @Test
  void declinedPaymentRollsBackEverything() {
    assertThrows(
        AppException.class, () -> service.book(passenger, schedule, 2, new CardPayment(false)));
    for (String t :
        List.of("bookings", "tickets", "payments", "seat_allocations", "payment_transactions"))
      assertEquals(0, count(t), t);
    assertTrue(service.seats(passenger, schedule).stream().allMatch(Seat::available));
  }

  @Test
  void seatValidationAndDuplicatePrevention() {
    assertThrows(AppException.class, () -> service.book(passenger, schedule, 5, new CashPayment()));
    service.book(passenger, schedule, 1, new CashPayment());
    assertThrows(AppException.class, () -> service.book(other, schedule, 1, new CashPayment()));
    assertEquals(1, count("bookings"));
  }

  @Test
  void competingBookingsHaveExactlyOneWinner() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch start = new CountDownLatch(1);
    try {
      Callable<Boolean> action =
          () -> {
            start.await();
            try {
              service.book(passenger, schedule, 1, new CardPayment(true));
              return true;
            } catch (AppException e) {
              return false;
            }
          };
      Future<Boolean> a = pool.submit(action), b = pool.submit(action);
      start.countDown();
      assertNotEquals(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
      assertEquals(1, count("seat_allocations"));
      assertEquals(1, count("bookings"));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void cancellationRefundIsIdempotentAndSeatReusable() {
    long id = service.book(passenger, schedule, 1, new CashPayment());
    service.cancel(passenger, id);
    service.cancel(passenger, id);
    assertEquals(0, count("seat_allocations"));
    assertEquals(2, count("payment_transactions"));
    assertEquals("REFUNDED", service.bookings(passenger).get(0).paymentStatus());
    service.book(other, schedule, 1, new CardPayment(true));
    assertEquals(2, count("tickets"));
  }

  @Test
  void crossAccountPrivacy() {
    long id = service.book(passenger, schedule, 1, new CashPayment());
    assertTrue(service.bookings(other).isEmpty());
    assertThrows(AppException.class, () -> service.ticket(other, id));
    assertThrows(AppException.class, () -> service.cancel(other, id));
    assertEquals(1, service.bookings(admin).size());
  }

  @Test
  void historyProtectsTrainDetailsAndRetirement() {
    long id = service.book(passenger, schedule, 1, new CashPayment());
    assertThrows(AppException.class, () -> service.saveTrain(admin, train, "Changed", "A", "C", 4));
    assertThrows(AppException.class, () -> service.deleteTrain(admin, train));
    service.cancel(admin, id);
    service.deleteTrain(admin, train);
    assertEquals("Express", service.bookings(admin).get(0).train());
  }

  @Test
  void departedSchedulesCannotBeBookedOrCancelled() {
    long id = service.book(passenger, schedule, 1, new CashPayment());
    RailService later =
        new RailService(
            repo,
            Clock.fixed(
                departure.plusHours(1).atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()));
    Session p = later.login("passenger@test.local", "Passenger123!".toCharArray());
    assertThrows(AppException.class, () -> later.book(p, schedule, 2, new CashPayment()));
    assertThrows(AppException.class, () -> later.cancel(p, id));
  }

  @Test
  void reportsBalanceChargeAndRefund() {
    long id = service.book(passenger, schedule, 1, new CashPayment());
    service.cancel(admin, id);
    BigDecimal net =
        service.transactions(admin).stream()
            .map(t -> t.type().equals("REFUND") ? t.amount().negate() : t.amount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertEquals(0, net.signum());
    assertThrows(AppException.class, () -> service.transactions(passenger));
  }

  @Test
  void bootstrapCannotCreateSecondAdmin() {
    assertThrows(
        AppException.class,
        () -> service.bootstrapAdmin("Second", "second@test.local", "Password123!".toCharArray()));
  }
}
