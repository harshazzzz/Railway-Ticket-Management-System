package com.railway.utils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class Validation {
  private Validation() {}

  public static String text(String value, String label, int max) {
    if (value == null
        || value.isBlank()
        || value.strip().length() > max
        || value.chars().anyMatch(Character::isISOControl))
      throw new AppException(label + " is required (maximum " + max + " characters).");
    return value.strip();
  }

  public static String email(String value) {
    String email = text(value, "Email", 190).toLowerCase(Locale.ROOT);
    if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
      throw new AppException("Enter a valid email address.");
    return email;
  }

  public static String password(char[] value) {
    String password = new String(value);
    if (password.length() < 10 || password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new AppException(
          "Password must have at least 10 characters and at most 72 UTF-8 bytes.");
    return password;
  }

  public static int capacity(int value) {
    if (value < 1 || value > 500) throw new AppException("Capacity must be between 1 and 500.");
    return value;
  }

  public static BigDecimal money(BigDecimal value) {
    if (value == null
        || value.signum() <= 0
        || value.scale() > 2
        || value.compareTo(new BigDecimal("99999999.99")) > 0)
      throw new AppException("Fare must be positive with no more than two decimal places.");
    return value.setScale(2);
  }
}
