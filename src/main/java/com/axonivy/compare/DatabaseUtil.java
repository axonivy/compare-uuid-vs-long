package com.axonivy.compare;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DatabaseUtil {

  public static void create() {
    if (getTables().isEmpty()) {
      createTaskTable();
      createSecurityMemberTables();
    }
  }

  private static void createTaskTable() {
    executeQuery(createTaskTableSqlStatement());
    System.out.println("Task table was created.");
  }

  private static void isConnectionError(Exception e) {
    var errorStrings = Arrays.asList("connection", "refused");
    if (errorStrings.stream().allMatch(e.getMessage()::contains)) {
      System.out.println("Could not connect to database.");
    }
    System.out.println(e.getMessage());
  }

  public static List<String> getTables() {
    var tableList = new ArrayList<String>();
    try (Connection connection = getConnection()) {
      Statement statement = connection.createStatement();
      // query only tables created by a user
      var query = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA not in ('information_schema', 'pg_catalog')";
      var result = statement.executeQuery(query);
      while (result.next()) {
        tableList.add(result.getString("TABLE_NAME"));
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return tableList;
  }

  private static String createSecurityMemberSqlStatement(String tableName, boolean bigint) {
    return "CREATE TABLE IF NOT EXISTS " + tableName + " ("
            + "Id " + (bigint ? "BIGINT" : "VARCHAR(255)") + " NOT NULL,"
            + "Name VARCHAR(255) NOT NULL,"
            + "PRIMARY KEY (Id)"
            + ")";
  }

  private static String createTaskTableSqlStatement() {
    return "CREATE TABLE IF NOT EXISTS Task ("
            + "Id BIGINT NOT NULL,"
            + "Name VARCHAR(255) NOT NULL,"
            + "UserId BIGINT NOT NULL,"
            + "UserRawUuid VARCHAR(255) NOT NULL,"
            + "UserUuid VARCHAR(255) NOT NULL,"
            + "PRIMARY KEY (Id)"
            + ")";
  }

  public static void createSecurityMemberTables() {
    try (Connection connection = getConnection()) {
      Statement statement = connection.createStatement();
      var tablesList = Arrays.asList("SecurityMemberLong", "SecurityMemberUuid", "SecurityMemberRawUuid");
      for (var table : tablesList) {
        statement.executeUpdate(createSecurityMemberSqlStatement(table, table.contains("Long")));
      }
      System.out.println("SecurityMember tables created.");
    } catch (SQLException e) {
      isConnectionError(e);
    }
  }

  public static String read() {
    StringBuilder sb = new StringBuilder();
    try (Connection connection = getConnection()) {
      Statement statement = connection.createStatement();
      var result = statement.executeQuery("SELECT * FROM Task");
      while (result.next()) {
        sb.append(result.getString("Id")).append(" ");
        sb.append(result.getString("Name")).append(" ");
        sb.append(result.getString("UserId")).append(" ");
        sb.append(result.getString("UserRawUuid")).append(" ");
        sb.append(result.getString("UserUuid")).append(" ");
        sb.append("\n");
      }
    } catch (Exception e) {
      isConnectionError(e);
    }
    return sb.toString();
  }

  private static Connection getConnection() throws SQLException {
    return DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "postgres", "nimda");
  }
}
