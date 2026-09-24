package com.railway.model;

import java.math.BigDecimal;

/** Passenger confirms a simulated cash payment; this is not a cash settlement integration. */
public record CashPayment() implements Payment {
  @Override
  public String method() {
    return "CASH";
  }

  @Override
  public boolean authorize(BigDecimal amount) {
    return amount.signum() > 0;
  }
}
