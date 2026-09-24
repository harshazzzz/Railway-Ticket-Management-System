package com.railway.database;

import com.railway.utils.AppException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.Properties;

/** One short-lived connection per operation; transactions share their connection explicitly. */
public final class Database {
  private final String url, user, password;

  public Database(String url, String user, String password) {
    this.url = url;
    this.user = user;
    this.password = password;
  }

  public static Database configured(boolean demo) {
    if (demo)
      return new Database("jdbc:h2:./data/railway;MODE=MySQL;DATABASE_TO_LOWER=TRUE", "sa", "");
    Properties p = new Properties();
    Path file = Path.of("config/database.properties");
    try {
      if (Files.exists(file))
        try (var in = Files.newInputStream(file)) {
          p.load(in);
        }
    } catch (IOException e) {
      throw new AppException("Cannot read database configuration.", e);
    }
    String url = System.getenv().getOrDefault("RAIL_DB_URL", p.getProperty("db.url", ""));
    String user = System.getenv().getOrDefault("RAIL_DB_USER", p.getProperty("db.user", ""));
    String password =
        System.getenv().getOrDefault("RAIL_DB_PASSWORD", p.getProperty("db.password", ""));
    if (!url.startsWith("jdbc:mysql:") || user.isBlank() || password.isBlank())
      throw new AppException("Configure MySQL in config/database.properties or use --demo.");
    return new Database(url, user, password);
  }

  public Connection open() throws SQLException {
    DriverManager.setLoginTimeout(10);
    return DriverManager.getConnection(url, user, password);
  }

  public void initialize() {
    try (var in = Database.class.getResourceAsStream("/schema.sql")) {
      if (in == null) throw new IOException("Missing schema resource");
      String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      try (var c = open();
          var s = c.createStatement()) {
        for (String statement : sql.split(";")) if (!statement.isBlank()) s.execute(statement);
      }
    } catch (IOException | SQLException e) {
      throw new AppException("Could not initialize database schema.", e);
    }
  }
}
