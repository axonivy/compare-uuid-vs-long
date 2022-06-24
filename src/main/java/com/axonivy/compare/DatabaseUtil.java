package com.axonivy.compare;

import java.sql.*;
import java.util.*;

public class DatabaseUtil {

  private static final List<Database> databases = createDatabases();

  private static List<Database> createDatabases() {
    var databases = new ArrayList<Database>();
    var defaultPassword = "nimda";
    databases.add(new Database("postgresql", "5432", "postgres", defaultPassword));
    databases.add(new Database("oracle", "1521", "SYSTEM", defaultPassword));
    databases.add(new Database("mariadb", "3010", "root", defaultPassword));
    databases.add(new Database("mysql", "3306", "root", defaultPassword));
    databases.add(new Database("mssql", "1433", "sa", "secure1234PASSWORD!"));
    return databases;
  }

  public static List<Database> getDatabases() {
    return databases;
  }

  public static void create() {
    for (var database : databases) {
      if (getTablesFromDb(database).isEmpty()) {
        createSecurityMemberTables(database);
        createTaskTable(database);
      } else {
        System.out.println("Tables already exist.");
      }
    }
  }

  public static void createSecurityMemberTables(Database db) {
    try (Connection connection = getConnection(db)) {
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

  private static void createTaskTable(Database db) {
    try (Connection connection = getConnection(db)) {
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

  public static void massInsertSecurityMembersToDb(Database db, String tableName, int count) {
    var createMemberStatement = "INSERT INTO " + tableName + " (Name) VALUES (?)";
    if (!tableName.contains("Long")) {
      createMemberStatement = "INSERT INTO " + tableName + " (Name, Id) VALUES (?, ?)";
    }
    try (Connection connection = getConnection(db)) {
      PreparedStatement preparedStatement = connection.prepareStatement(createMemberStatement);
      for (var i = 0; i < count; i++) {
        preparedStatement.setString(1, "user" + i);
        if (!tableName.contains("Long")) {
          var uuid = UUID.randomUUID().toString().toUpperCase();
          if (tableName.contains("RawUuid")) {
            preparedStatement.setString(2, uuid);
          } else {
            preparedStatement.setString(2, "USER-" + uuid);
          }
        }
        preparedStatement.addBatch();
      }
      preparedStatement.executeBatch();
    } catch (SQLException e) {
      isConnectionError(e);
    }
  }

  public static void massInsertTaskToDb(Database db, int count) {
    var createMemberStatement = "INSERT INTO Task (Name, UserId, UserUuid, UserRawUuid) VALUES (?, ?, ?, ?)";
    try (Connection connection = getConnection(db)) {
      PreparedStatement preparedStatement = connection.prepareStatement(createMemberStatement);
      int lastUsers = 0;
      var currentUsersSet = getRandomUsers(db);
      for (var i = 0; i < count; i++) {
        if (i / 100 > lastUsers) {
          currentUsersSet = getRandomUsers(db);
          lastUsers += 1;
        }
        preparedStatement.setString(1, "Task-" + i);
        preparedStatement.setInt(2, Integer.parseInt(currentUsersSet.get(0)));
        preparedStatement.setString(3, currentUsersSet.get(1));
        preparedStatement.setString(4, currentUsersSet.get(2));
        preparedStatement.addBatch();
      }
      preparedStatement.executeBatch();
    } catch (SQLException e) {
      isConnectionError(e);
    }
  }

  public static double measureFindingTasks(Database db, String columnName, String user) {
    var queryTimes = new ArrayList<Long>();
    PreparedStatement statement;
    try (Connection connection = getConnection(db)) {
      statement = connection.prepareStatement("SELECT * FROM Task WHERE " + columnName + " = ?");
      if (columnName.equals("UserId")) {
        statement.setInt(1, Integer.parseInt(user));
      } else {
        statement.setString(1, user);
      }
      for (int j = 0; j < 1000; j++) {
        long startTime = System.nanoTime();
        statement.executeQuery();
        var elapsedTime = System.nanoTime() - startTime;
        queryTimes.add(elapsedTime);
      }
    } catch (Exception e) {
      isConnectionError(e);
    }
    var averageQueryTime = queryTimes.stream().mapToLong(Long::longValue).average().getAsDouble();
    return averageQueryTime / 1000000.0; // convert to ms
  }

  public static List<String> getRandomUsers(Database db) {
    var randomUsers = new ArrayList<String>();
    for (var table : DatabaseUtil.getSecMemberTableNames()) {
      var randomUser = DatabaseUtil.getRandomUser(db, table);
      randomUsers.add(randomUser);
    }
    return randomUsers;
  }


  public static boolean entriesAlreadyExist(String tableName, int entries) {
    return getDatabases().stream().allMatch(db -> entriesAlreadyExist(db, tableName, entries));
  }

  public static boolean entriesAlreadyExist(Database db, String tableName, int amountOfEntries) {
    var tables = getTablesFromDb(db);
    if (tables.isEmpty()) {
      return false;
    }
    try (Connection connection = getConnection(db)) {
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

  public static List<String> getTablesFromDb(Database db) {
    var tableList = new ArrayList<String>();
    try (Connection connection = getConnection(db)) {
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

  public static String getRandomUser(Database db, String tableName) {
    try (Connection connection = getConnection(db)) {
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

  public static void cleanupDatabase() {
    getDatabases().forEach(DatabaseUtil::cleanupDatabase);
  }

  public static void cleanupDatabase(Database db) {
    try (Connection connection = getConnection(db)) {
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

  private static Connection getConnection(Database db) throws SQLException {
    return DriverManager.getConnection("jdbc:"+db.name()+"://localhost:"+db.port()+"/"+db.user(), db.user(), db.password());
  }
}
