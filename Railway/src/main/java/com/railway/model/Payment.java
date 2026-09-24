package com.railway.model;

import java.math.BigDecimal;

/** Simulation strategy. Sealed to prevent arbitrary untrusted payment implementations. */
public sealed interface Payment permits CardPayment, CashPayment {
  String method();

  boolean authorize(BigDecimal amount);
}
