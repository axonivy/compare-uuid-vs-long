package com.axonivy.compare;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;

public class Compare {

  public static void main(String[] args) {
    Menu.printMainMenu();
  }

  public static void prepareDb() {
    DatabaseUtil.create();
    var amountOfEntries = Menu.getIntInput("How many Database entries do you want to create?: ");
    if (amountOfEntries < 1) {
      System.out.println("Invalid input!");
      return;
    }
    generateSecMemberEntries(amountOfEntries);
    generateTasks(amountOfEntries);
  }

  private static void generateTasks(int entries) {
    DatabaseUtil.getDatabases().forEach(db -> generateTasks(db, entries));
  }

  private static void generateTasks(Database db, int amountOfEntries) {
    if (DatabaseUtil.entriesAlreadyExist("Task", amountOfEntries)) {
      System.out.println("Entries already exist in table: " + "Task");
      return;
    }
    System.out.println("\nGenerating " + amountOfEntries + " entries in table: Task, " + db.type());
    DatabaseUtil.massInsertTaskToDb(db, amountOfEntries);
    System.out.println("Created " + amountOfEntries + " tasks\n");
  }

  private static void generateSecMemberEntries(int entries) {
    DatabaseUtil.getDatabases().forEach(db -> generateSecMemberEntries(db, entries));
  }

  private static void generateSecMemberEntries(Database db, int amountOfEntries) {
    var tableIndex = 1;
    var secMemberTables = DatabaseUtil.getSecMemberTableNames();
    for (var table : secMemberTables) {
      if (DatabaseUtil.entriesAlreadyExist(table, amountOfEntries)) {
        System.out.println("Entries already exist in table: " + table);
        continue;
      }
      System.out.println("\nGenerating " + amountOfEntries + " members for table: " + table + "... ("+db.type()+" " + tableIndex + "/" + secMemberTables.size() + ")");
      long startTime = System.nanoTime();
      DatabaseUtil.massInsertSecurityMembersToDb(db, table, amountOfEntries);
      long elapsedTime = System.nanoTime() - startTime;
      var prettyTime = prettyTime(elapsedTime / 1000000);
      System.out.println("Created " + amountOfEntries + " entries after " + prettyTime + " in table: " + table);
      tableIndex++;
    }
  }

  public static void compareAllDbs() {
    var databases = DatabaseUtil.getDatabases();
    for (var database : databases) {
      compare(database);
    }
  }

  public static void compare(Database db) {
    if (db == null) {
      compareAllDbs();
    } else {
      DecimalFormat df = new DecimalFormat("#.####");
      df.setRoundingMode(RoundingMode.CEILING);
      System.out.println("\nComparing tables in " + db.type() + " ...");
      var randomUsers = DatabaseUtil.getRandomUsers(db);
      var columns = List.of("UserId", "UserUuid", "UserRawUuid");
      for (int i = 0; i < randomUsers.size(); i++) {
        var milliseconds = df.format(DatabaseUtil.measureFindingTasks(db, columns.get(i), randomUsers.get(i)));
        String column = String.format("%12s", columns.get(i));
        System.out.println("Column: " + column + " | Average query time: " + milliseconds + "ms" + " | User: " + randomUsers.get(i));
      }
    }
  }

  public static void checkDb() {
    DatabaseUtil.getDatabases().forEach(Compare::checkDb);
  }

  public static void checkDb(Database db) {
    var tables = DatabaseUtil.getTablesFromDb(db);
    if (!tables.isEmpty()) {
      System.out.println("Tables: " + tables);
    } else {
      System.out.println("No tables found.");
    }
  }

  private static String prettyTime(long timeInMs) {
    var seconds = timeInMs / 1000;
    if (seconds <= 60) {
      return "< 1 minute";
    } else if (seconds <= 3600) {
      return (seconds / 60) + " minutes";
    }
    var hours = seconds / 3600;
    var minutes = (seconds % 3600) / 60;
    if (hours >= 24) {
      return (hours / 24) + " days " + (hours % 24) + " hours " + minutes + " minutes";
    }
    return hours + " hours " + minutes + " minutes";
  }
}
