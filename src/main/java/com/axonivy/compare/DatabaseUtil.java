package com.axonivy.compare;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseUtil {

  public static void create() {
    createTaskTable();
    createSecurityMemberTable(PrimaryKeyType.LONG.getValue());
  }

  public static String getTables() {
    String tables = "";
    try(Connection connection = getConnection()) {
      Statement statement = connection.createStatement();
      var result = statement.executeQuery("SELECT * FROM information_schema.tables;");
      while (result.next()) {
        tables += result.getString("TABLE_NAME") + "\n";
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return tables;
  }

  public static void createTaskTable() {
    try(Connection connection = getConnection()) {
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
      throw new RuntimeException(e);
    }
  }

  public static void createSecurityMemberTable(String primaryKeyType) {
    try(Connection connection = getConnection()) {
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
        sb.append(result.getString("Id") + " ");
        sb.append(result.getString("Name") + " ");
        sb.append(result.getString("UserId") + " ");
        sb.append(result.getString("UserRawUuid") + " ");
        sb.append(result.getString("UserUuid") + " ");
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
