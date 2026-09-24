package com.railway.model;

/** Immutable account data. Password hashes never leave the repository layer. */
public abstract sealed class User permits Admin, Passenger {
  private final long id;
  private final String name;
  private final String email;
  private final boolean active;

  protected User(long id, String name, String email, boolean active) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.active = active;
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public boolean isActive() {
    return active;
  }

  public abstract String role();

  public abstract String dashboardTitle();
}
