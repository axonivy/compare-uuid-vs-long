package com.axonivy.compare;

import java.sql.*;
import java.util.*;

public class DatabaseUtil {

  private static final List<Database> databases = createDatabases();

  private static List<Database> createDatabases() {
    var databases = new ArrayList<Database>();
    var defaultPassword = "nimda";
    databases.add(new Database("postgresql", "jdbc:postgresql://localhost:5432/", "postgres", "postgres", defaultPassword));
//    databases.add(new Database("oracle", "jdbc:oracle:thin:@localhost:1521:oracle", "SYSTEM", defaultPassword));
    databases.add(new Database("mariadb", "jdbc:mariadb://localhost:3010/", "comparePerformance", "root", defaultPassword));
    databases.add(new Database("mysql", "jdbc:mysql://localhost:3306/", "comparePerformance","root", defaultPassword));
    databases.add(new Database("sqlserver", "jdbc:sqlserver://localhost:1433;encrypt=false", "comparePerformance", "sa", "secure1234PASSWORD!"));
    return databases;
  }

  public static List<Database> getDatabases() {
    return databases;
  }

  public static void create() {
    for (var database : databases) {
      System.out.println("\nCreating tables for database: " + database.type());
      if (!database.type().equals("postgresql")) {
        createDatabase(database);
      }
      createSecurityMemberTables(database);
      createTaskTable(database);
    }
  }

  private static void createDatabase(Database db) {
    try (Connection connection = getConnectionRaw(db)) {
      ResultSet resultSet = connection.getMetaData().getCatalogs();
      var databaseExists = false;
      while (resultSet.next()) {
        if (resultSet.getString(1).equals(db.databaseName())) {
          databaseExists = true;
          break;
        }
      }
      if (!databaseExists) {
        Statement statement = connection.createStatement();
        var script = "CREATE DATABASE IF NOT EXISTS ";
        if (db.type().equals("sqlserver"))
          script = "CREATE DATABASE ";
        statement.executeUpdate(script + "comparePerformance");
      }
    } catch (SQLException e) {
      isConnectionError(e);
    }
  }

  public static void createSecurityMemberTables(Database db) {
    try (Connection connection = getConnection(db)) {
      Statement statement = connection.createStatement();
      for (var table : getSecMemberTableNames()) {
        statement.executeUpdate(createSecurityMemberSqlStatement(db, table, table.contains("Long")));
      }
    } catch (SQLException e) {
      isConnectionError(e);
    }
    System.out.println("SecurityMember tables created.");
  }

  private static String createSecurityMemberSqlStatement(Database db, String tableName, boolean bigint) {
    return switch (db.type()) {
      case "sqlserver" -> "CREATE TABLE " + tableName + " (" +
              "Id " + (bigint ? "BIGINT IDENTITY(1,1)" : "VARCHAR(255) NOT NULL") + " PRIMARY KEY," +
              "Name VARCHAR(255) NOT NULL" +
              ")";
      case "postgresql" -> "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
              "Id " + (bigint ? "SERIAL," : "VARCHAR(255) NOT NULL,") +
              "Name VARCHAR(255) NOT NULL," +
              "PRIMARY KEY (Id)" +
              ")";
      default -> "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
              "Id " + (bigint ? "BIGINT AUTO_INCREMENT," : "VARCHAR(255),") +
              "Name TEXT NOT NULL," +
              "PRIMARY KEY (Id)" +
              ");";
    };
  }

  private static void createTaskTable(Database db) {
    try (Connection connection = getConnection(db)) {
      Statement statement = connection.createStatement();
      statement.executeUpdate(createTaskTableSqlStatement(db));
    } catch (SQLException e) {
      isConnectionError(e);
    }
    System.out.println("Task table was created.");
  }

  private static String createTaskTableSqlStatement(Database db) {
    return switch (db.type()) {
      case "sqlserver" -> "CREATE TABLE Task ("
              + "Id BIGINT IDENTITY(1,1),"
              + "Name VARCHAR(255) NOT NULL,"
              + "UserId BIGINT NOT NULL,"
              + "UserRawUuid VARCHAR(255) NOT NULL,"
              + "UserUuid VARCHAR(255) NOT NULL,"
              + "PRIMARY KEY (Id),"
              + "FOREIGN KEY (UserId) REFERENCES SecurityMemberLong(Id),"
              + "FOREIGN KEY (UserRawUuid) REFERENCES SecurityMemberRawUuid(Id),"
              + "FOREIGN KEY (UserUuid) REFERENCES SecurityMemberUuid(Id)"
              + ")";
      case "postgresql" -> "CREATE TABLE IF NOT EXISTS Task ("
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
      default -> "CREATE TABLE IF NOT EXISTS Task ("
              + "Id BIGINT AUTO_INCREMENT,"
              + "Name VARCHAR(255) NOT NULL,"
              + "UserId BIGINT REFERENCES SecurityMemberLong(Id),"
              + "UserRawUuid VARCHAR(255) REFERENCES SecurityMemberRawUuid(Id),"
              + "UserUuid VARCHAR(255) REFERENCES SecurityMemberUuid(Id),"
              + "PRIMARY KEY (Id)"
              + ")";
    };
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
      if (db.type().equals("sqlserver"))
        connection.setAutoCommit(false);
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
      if (db.type().equals("sqlserver"))
        connection.commit();
    } catch (SQLException e) {
      isConnectionError(e);
    }
  }

  public static void massInsertTaskToDb(Database db, int count) {
    var createMemberStatement = "INSERT INTO Task (Name, UserId, UserUuid, UserRawUuid) VALUES (?, ?, ?, ?)";
    if (!entriesAlreadyExist(db, "Task", count)) {
      try (Connection connection = getConnection(db)) {
        if (db.type().equals("sqlserver"))
          connection.setAutoCommit(false);
        PreparedStatement preparedStatement = connection.prepareStatement(createMemberStatement);
        var currentUsersSet = getRandomUsers(db);
        for (var i = 0; i < count; i++) {
          if ((i % 1000) == 0) {
            currentUsersSet = getRandomUsers(db);
            if (i % (count/10) == 0) {
              System.out.println("Prepared " + i + " tasks.");
            }
          }
          preparedStatement.setString(1, "Task-" + i);
          preparedStatement.setInt(2, Integer.parseInt(currentUsersSet.get(0)));
          preparedStatement.setString(3, currentUsersSet.get(1));
          preparedStatement.setString(4, currentUsersSet.get(2));
          preparedStatement.addBatch();
        }
        System.out.println("Executing batch insert to Database...");
        preparedStatement.executeBatch();
        if (db.type().equals("sqlserver"))
          connection.commit();
      } catch (SQLException e) {
        isConnectionError(e);
      }
    } else {
      System.out.println("Entries already exist in " + db.type() + " Task table.");
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
    var averageQueryTime = queryTimes.stream().mapToLong(Long::longValue).average();
    if (averageQueryTime.isEmpty()) {
      return 0;
    }
    return averageQueryTime.getAsDouble() / 1000000.0; // convert to ms
  }

  public static List<String> getRandomUsers(Database db) {
    var randomUsers = new ArrayList<String>();
    for (var table : DatabaseUtil.getSecMemberTableNames()) {
      randomUsers.add(DatabaseUtil.getRandomUser(db, table));
    }
    return randomUsers;
  }


//  public static boolean entriesAlreadyExist(String tableName, int entries) {
//    return getDatabases().stream().allMatch(db -> entriesAlreadyExist(db, tableName, entries));
//  }
//
  public static boolean entriesAlreadyExist(Database db, String tableName, int amountOfEntries) {
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
      var randomFunction = "RAND()";
      if (db.type().equals("postgresql"))
        randomFunction = "RANDOM()";
      var sql = "SELECT Id FROM " + tableName + " ORDER BY " + randomFunction + " LIMIT 1";
      if (db.type().equals("sqlserver"))
        sql = "SELECT TOP 1 Id FROM " + tableName + " ORDER BY NEWID()";
      var resultSet = statement.executeQuery(sql);
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
    System.out.println("Database " + db.type() + " cleaned up.");
  }

  private static Connection getConnection(Database db) throws SQLException {
    if (db.type().equals("sqlserver"))
      return DriverManager.getConnection(db.url()+";databaseName="+db.databaseName(), db.user(), db.password());
    if (db.type().equals("mysql"))
      return DriverManager.getConnection(db.url()+db.databaseName()+"?rewriteBatchedStatements=true", db.user(), db.password());
    return DriverManager.getConnection(db.url()+db.databaseName(), db.user(), db.password());
  }

  private static Connection getConnectionRaw(Database db) throws SQLException {
    return DriverManager.getConnection(db.url(), db.user(), db.password());
  }
}
