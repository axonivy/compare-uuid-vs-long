package com.axonivy.compare;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DatabaseUtil {

  public static void create() {
    if (getTables().isEmpty()) {
      createTaskTable();
      createSecurityMemberTable(PrimaryKeyType.LONG.getValue());
    }
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

  public static boolean createTaskTable() {
    try (Connection connection = getConnection()) {
      Statement statement = connection.createStatement();
      String taskTableSql = "CREATE TABLE IF NOT EXISTS Task ("
              + "Id BIGINT NOT NULL,"
              + "Name VARCHAR(255) NOT NULL,"
              + "UserId BIGINT NOT NULL,"
              + "UserRawUuid VARCHAR(255) NOT NULL,"
              + "UserUuid VARCHAR(255) NOT NULL,"
              + "PRIMARY KEY (Id)"
              + ")";
      statement.execute(taskTableSql);
    } catch (SQLException e) {
      var errorStrings = Arrays.asList("connection", "refused");
      if (errorStrings.stream().allMatch(e.getMessage()::contains)) {
        System.out.println("Could not connect to database.");
        return false;
      }
      System.out.println(e.getMessage());
      return false;
    }
    return true;
  }

  public static void createSecurityMemberTable(String primaryKeyType) {
    try (Connection connection = getConnection()) {
      Statement statement = connection.createStatement();
      String taskTableSql = "CREATE TABLE IF NOT EXISTS SecurityMember ("
              + "Id " + primaryKeyType + " NOT NULL,"
              + "Name VARCHAR(255) NOT NULL,"
              + "PRIMARY KEY (Id)"
              + ")";
      statement.execute(taskTableSql);
    } catch (SQLException e) {
      throw new RuntimeException(e);
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
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return sb.toString();
  }

  public static String executeQuery(String query) {
    StringBuilder sb = new StringBuilder();
    try (Connection connection = getConnection()) {
      Statement statement = connection.createStatement();
      sb.append(statement.executeQuery(query));
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return sb.toString();
  }

  private static Connection getConnection() throws SQLException {
    return DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "postgres", "nimda");
  }
}
