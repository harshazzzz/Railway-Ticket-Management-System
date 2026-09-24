package com.railway.model;

public final class Passenger extends User {
  public Passenger(long id, String name, String email, boolean active) {
    super(id, name, email, active);
  }

  @Override
  public String role() {
    return "PASSENGER";
  }

  @Override
  public String dashboardTitle() {
    return "Where will you go next?";
  }
}
