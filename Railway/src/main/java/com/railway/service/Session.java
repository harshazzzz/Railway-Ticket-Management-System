package com.railway.service;

import com.railway.model.User;
import java.util.UUID;

public final class Session {
  private final UUID token = UUID.randomUUID();
  private final User user;

  Session(User user) {
    this.user = user;
  }

  UUID token() {
    return token;
  }

  public User user() {
    return user;
  }
}
