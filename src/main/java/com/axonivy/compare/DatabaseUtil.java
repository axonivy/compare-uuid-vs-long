package com.axonivy.compare;

import java.sql.*;
import java.util.*;

public class DatabaseUtil {

  public static void create() {
    if (getTablesFromDb().isEmpty()) {
      createSecurityMemberTables();
      createTaskTable();
    }
    else {
      System.out.println("Tables already exist.");
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
            + "Id SERIAL,"
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
            + "Id " + (bigint ? "SERIAL" : "VARCHAR(255) NOT NULL") + ","
            + "Name VARCHAR(255) NOT NULL,"
            + "PRIMARY KEY (Id)"
            + ")";
  }

  public static void insertSecurityMemberToDb(String tableName, String userName) {
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

  public static void insertTaskToDb(String taskName, List<String> userIds) {
    var createMemberStatement = "INSERT INTO Task (Name, UserId, UserUuid, UserRawUuid) VALUES (?, ?, ?, ?)";
    try (Connection connection = getConnection()) {
      PreparedStatement preparedStatement = connection.prepareStatement(createMemberStatement);
      preparedStatement.setString(1, taskName);
      preparedStatement.setInt(2, Integer.parseInt(userIds.get(0)));
      preparedStatement.setString(3, userIds.get(1));
      preparedStatement.setString(4, userIds.get(2));
      preparedStatement.executeUpdate();
    } catch (SQLException e) {
      isConnectionError(e);
    }
  }

  public static boolean entriesAlreadyExist(String tableName) {
    return entriesAlreadyExist(tableName, 1);
  }

  public static boolean entriesAlreadyExist(String tableName, int amountOfEntries) {
    var tables = getTablesFromDb();
    if (tables.isEmpty()) {
      return false;
    }
    try (Connection connection = getConnection()) {
      Statement statement = connection.createStatement();
      var resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + tableName);
      if (resultSet.next()) {
        var count = resultSet.getInt(1);
        return count >= amountOfEntries;
      }
    } catch (SQLException e) {
      isConnectionError(e);
    }
    return false;
  }

  private static void isConnectionError(Exception e) {
    var errorStrings = Arrays.asList("connection", "refused");
    if (errorStrings.stream().allMatch(e.getMessage()::contains)) {
      System.out.println("Could not connect to database.");
    }
    System.out.println(e.getMessage());
  }

  public static List<String> getTablesFromDb() {
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

  public static String getRandomUser(String tableName) {
    try (Connection connection = getConnection()) {
      Statement statement = connection.createStatement();
      var resultSet = statement.executeQuery("SELECT Id FROM " + tableName + " ORDER BY RANDOM() LIMIT 1");
      if (resultSet.next()) {
        return resultSet.getString(1);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return null;
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

  public static void cleanupDatabase() {
    try (Connection connection = getConnection()) {
      Statement statement = connection.createStatement();
      statement.executeUpdate("DROP TABLE IF EXISTS Task");
      for (var table : getSecMemberTableNames()) {
        statement.executeUpdate("DROP TABLE IF EXISTS " + table);
      }
    } catch (SQLException e) {
      isConnectionError(e);
    }
    System.out.println("Database cleaned up.");
  }

  private static Connection getConnection() throws SQLException {
    return DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "postgres", "nimda");
  }
}
