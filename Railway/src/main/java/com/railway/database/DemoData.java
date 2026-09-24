package com.railway.database;

import com.railway.model.CardPayment;
import com.railway.repository.JdbcRepository;
import com.railway.service.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class DemoData {
  private DemoData() {}

  /** Seed only an empty demo database. Existing user data is never reset. */
  public static void seed(Database database, RailService service) {
    boolean empty =
        new JdbcRepository(database)
            .read(
                c -> JdbcRepository.query(c, "SELECT id FROM users", r -> r.getLong(1)).isEmpty());
    if (!empty) return;
    service.bootstrapAdmin("Operations Admin", "admin@railway.demo", "Railway123!".toCharArray());
    service.register("Alex Morgan", "passenger@railway.demo", "Journey123!".toCharArray());
    Session admin = service.login("admin@railway.demo", "Railway123!".toCharArray());
    long coastal = service.saveTrain(admin, 0, "Coastal Express", "Colombo Fort", "Galle", 48);
    long hill = service.saveTrain(admin, 0, "Hill Country Intercity", "Colombo Fort", "Kandy", 40);
    long northern = service.saveTrain(admin, 0, "Northern Star", "Colombo Fort", "Jaffna", 60);
    LocalDate day = LocalDate.now().plusDays(1);
    long first = 0;
    for (int n = 0; n < 14; n++) {
      long id =
          service.addSchedule(
              admin,
              coastal,
              day.plusDays(n).atTime(8, 30),
              day.plusDays(n).atTime(11, 15),
              new BigDecimal("1500.00"));
      if (n == 0) first = id;
      service.addSchedule(
          admin,
          hill,
          day.plusDays(n).atTime(9, 0),
          day.plusDays(n).atTime(12, 30),
          new BigDecimal("2200.00"));
      service.addSchedule(
          admin,
          northern,
          day.plusDays(n).atTime(6, 15),
          day.plusDays(n).atTime(14, 0),
          new BigDecimal("3500.00"));
    }
    Session passenger = service.login("passenger@railway.demo", "Journey123!".toCharArray());
    service.book(passenger, first, 7, new CardPayment(true));
    service.logout(passenger);
    service.logout(admin);
  }
}
