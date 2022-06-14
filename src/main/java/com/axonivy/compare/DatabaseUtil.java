package com.axonivy.compare;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class DatabaseUtil {

  public static void create() {
    if (getTables().isEmpty()) {
      createSecurityMemberTables();
      createTaskTable();
    }
  }

  private static void createTaskTable() {
    try (Connection connection = getConnection()) {
      Statement statement = connection.createStatement();
      statement.executeUpdate(createTaskTableSqlStatement());
    } catch (SQLException e) {
      isConnectionError(e);
    }
    System.out.println("Task table was created.");
  }

  private static String createTaskTableSqlStatement() {
    return "CREATE TABLE IF NOT EXISTS Task ("
            + "Id BIGINT NOT NULL,"
            + "Name VARCHAR(255) NOT NULL,"
            + "UserId BIGINT NOT NULL,"
            + "UserRawUuid VARCHAR(255) NOT NULL,"
            + "UserUuid VARCHAR(255) NOT NULL,"
            + "PRIMARY KEY (Id),"
            + "FOREIGN KEY (UserId) REFERENCES SecurityMemberLong(Id),"
            + "FOREIGN KEY (UserRawUuid) REFERENCES SecurityMemberRawUuid(Id),"
            + "FOREIGN KEY (UserUuid) REFERENCES SecurityMemberUuid(Id)"
            + ")";
  }

  public static List<String> getSecMemberTableNames() {
    return Arrays.asList("SecurityMemberLong", "SecurityMemberUuid", "SecurityMemberRawUuid");
  }

  public static void createSecurityMemberTables() {
    try (Connection connection = getConnection()) {
      Statement statement = connection.createStatement();
      for (var table : getSecMemberTableNames()) {
        statement.executeUpdate(createSecurityMemberSqlStatement(table, table.contains("Long")));
      }
    } catch (SQLException e) {
      isConnectionError(e);
    }
    System.out.println("SecurityMember tables created.");
  }

  private static String createSecurityMemberSqlStatement(String tableName, boolean bigint) {
    return "CREATE TABLE IF NOT EXISTS " + tableName + " ("
            + "Id " + (bigint ? "BIGINT" : "VARCHAR(255)") + " NOT NULL,"
            + "Name VARCHAR(255) NOT NULL,"
            + "PRIMARY KEY (Id)"
            + ")";
  }

  public static void createSecurityMember(String tableName, String userName) {
    var createMemberStatement = "INSERT INTO " + tableName + " (Name) VALUES (?)";
    if (!tableName.contains("Long")) {
      createMemberStatement = "INSERT INTO " + tableName + " (Name, Id) VALUES (?, ?)";
    }
    try (Connection connection = getConnection()) {
      PreparedStatement preparedStatement = connection.prepareStatement(createMemberStatement);
      preparedStatement.setString(1, userName);
      if (!tableName.contains("Long")) {
        var uuid = UUID.randomUUID().toString().toUpperCase();
        if (tableName.contains("RawUuid")) {
          preparedStatement.setString(2, uuid);
        } else {
          preparedStatement.setString(2, "USER-"+uuid);
        }
      }
      preparedStatement.executeUpdate();
    } catch (SQLException e) {
      isConnectionError(e);
    }
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
