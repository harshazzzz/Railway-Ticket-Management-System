package com.railway.model;

import java.math.BigDecimal;

/** No card details are accepted or persisted. Decline mode exercises transaction rollback. */
public record CardPayment(boolean approved) implements Payment {
  @Override
  public String method() {
    return "CARD";
  }

  @Override
  public boolean authorize(BigDecimal amount) {
    return approved && amount.signum() > 0;
  }
}
