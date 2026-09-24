package com.railway.model;

public final class Admin extends User {
  public Admin(long id, String name, String email, boolean active) {
    super(id, name, email, active);
  }

  @Override
  public String role() {
    return "ADMIN";
  }

  @Override
  public String dashboardTitle() {
    return "Operations overview";
  }
}
