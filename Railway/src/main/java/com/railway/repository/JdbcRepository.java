package com.railway.repository;

import com.railway.database.Database;
import com.railway.utils.AppException;
import java.sql.*;
import java.util.*;

/** JDBC plumbing: all variable data is bound; SQL identifiers are fixed in source. */
public final class JdbcRepository {
  @FunctionalInterface
  public interface Work<T> {
    T run(Connection c) throws SQLException;
  }

  @FunctionalInterface
  public interface Mapper<T> {
    T map(ResultSet rs) throws SQLException;
  }

  private final Database database;

  public JdbcRepository(Database database) {
    this.database = database;
  }

  public <T> T read(Work<T> work) {
    try (Connection c = database.open()) {
      return work.run(c);
    } catch (SQLException e) {
      throw translate(e);
    }
  }

  public <T> T transaction(Work<T> work) {
    try (Connection c = database.open()) {
      c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
      c.setAutoCommit(false);
      try {
        T result = work.run(c);
        c.commit();
        return result;
      } catch (SQLException | RuntimeException e) {
        c.rollback();
        throw e;
      }
    } catch (SQLException e) {
      throw translate(e);
    }
  }

  private AppException translate(SQLException e) {
    if (e.getSQLState() != null && e.getSQLState().startsWith("23"))
      return new AppException(
          "This record already exists or conflicts with a related record. Refresh and try again.",
          e);
    return new AppException("Database operation failed. Check your connection and retry.", e);
  }

  public static <T> List<T> query(Connection c, String sql, Mapper<T> mapper, Object... args)
      throws SQLException {
    try (PreparedStatement s = prepare(c, sql, args);
        ResultSet rs = s.executeQuery()) {
      List<T> results = new ArrayList<>();
      while (rs.next()) results.add(mapper.map(rs));
      return results;
    }
  }

  public static int update(Connection c, String sql, Object... args) throws SQLException {
    try (PreparedStatement s = prepare(c, sql, args)) {
      return s.executeUpdate();
    }
  }

  public static long insert(Connection c, String sql, Object... args) throws SQLException {
    try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      bind(s, args);
      s.executeUpdate();
      try (ResultSet keys = s.getGeneratedKeys()) {
        if (!keys.next()) throw new SQLException("No generated key");
        return keys.getLong(1);
      }
    }
  }

  private static PreparedStatement prepare(Connection c, String sql, Object... args)
      throws SQLException {
    PreparedStatement s = c.prepareStatement(sql);
    try {
      s.setQueryTimeout(15);
      bind(s, args);
      return s;
    } catch (SQLException e) {
      s.close();
      throw e;
    }
  }

  private static void bind(PreparedStatement s, Object... args) throws SQLException {
    for (int i = 0; i < args.length; i++) s.setObject(i + 1, args[i]);
  }
}
