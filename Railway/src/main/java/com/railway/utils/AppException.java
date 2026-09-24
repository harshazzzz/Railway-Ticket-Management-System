package com.railway.utils;

/** Safe, actionable message suitable for display in the GUI. */
public class AppException extends RuntimeException {
  public AppException(String message) {
    super(message);
  }

  public AppException(String message, Throwable cause) {
    super(message, cause);
  }
}
