package com.axonivy.compare;

import java.sql.SQLException;
import java.util.ArrayList;
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

  private static void generateTasks(int amountOfEntries) {
    if (DatabaseUtil.entriesAlreadyExist("Task", amountOfEntries)) {
      System.out.println("Entries already exist in table: " + "Task");
      return;
    }
    System.out.println("\nGenerating " + amountOfEntries + " entries in table: Task");
    DatabaseUtil.massInsertTaskToDb(amountOfEntries);
    System.out.println("Created " + amountOfEntries + " tasks\n");
  }

  private static void generateSecMemberEntries(int amountOfEntries) {
    var tableIndex = 0;
    var secMemberTables = DatabaseUtil.getSecMemberTableNames();
    for (var table : secMemberTables) {
      if (DatabaseUtil.entriesAlreadyExist(table, amountOfEntries)) {
        System.out.println("Entries already exist in table: " + table);
        continue;
      }
      System.out.println("\nGenerating " + amountOfEntries + " members for table: " + table + "... (" + ++tableIndex + "/" + secMemberTables.size() + ")");
      long startTime = System.nanoTime();
      DatabaseUtil.massInsertSecurityMembersToDb(table, amountOfEntries);
      long elapsedTime = System.nanoTime() - startTime;
      var prettyTime = prettyTime(elapsedTime / 1000000);
      System.out.println("Created " + amountOfEntries + " entries after " + prettyTime + " in table: " + table);
    }
  }

  public static void compare() {
    System.out.println("\nComparing tables...");
    var randomUsers = DatabaseUtil.getRandomUsers();
    var columns = List.of("UserId", "UserUuid", "UserRawUuid");
    for (int i = 0; i < randomUsers.size(); i++) {
      var milliseconds = DatabaseUtil.measureFindingTasks(columns.get(i), randomUsers.get(i));
      System.out.println("Column: " + columns.get(i) + " | Average query time: " + milliseconds + "ms" + " | User: " + randomUsers.get(i));
    }
  }

  public static void checkDb() {
    var tables = DatabaseUtil.getTablesFromDb();
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
