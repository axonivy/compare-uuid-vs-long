package com.axonivy.compare;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Compare {

  private static final int averageMsForHundredEntries = 1500;
  private static final int amountOfTables = 4;

  public static void main(String[] args) {
    Menu.printMainMenu();
  }

  public static void prepareDb() {
    DatabaseUtil.create();
    var amountOfEntries = Menu.getIntInput("How many Database entries do you want to create?: ");
    var estimatedTime = (amountOfEntries/100) * averageMsForHundredEntries * amountOfTables / 1000;
    if (amountOfEntries < 1) {
      System.out.println("Invalid input!");
      prepareDb();
    } else if (estimatedTime > 300) {
        System.out.println("Estimated time to create all data is: " +estimatedTime + "s. Are you sure you want to continue? (y/n)");
        if (!"y".equals(Menu.getInput())) {
          prepareDb();
        }
        else {
          generateSecMemberEntries(amountOfEntries);
          generateTasks(amountOfEntries);
        }
    }
  }

  private static void generateTasks(int amountOfEntries) {
    if (DatabaseUtil.entriesAlreadyExist("Task", amountOfEntries)) {
      System.out.println("Entries already exist in table: " + "Task");
      return;
    }

    int lastUsers = 0;
    var currentUsersSet = getRandomUsers();
    for (var i = 0; i < amountOfEntries; i++) {
      System.out.println(i + ": " + currentUsersSet);
      if (i/100 > lastUsers) {
        currentUsersSet = getRandomUsers();
        lastUsers += 1;
      }
      DatabaseUtil.insertTaskToDb("Task-" + i, currentUsersSet);
    }
    System.out.println("Created " + amountOfEntries + " tasks\n");
  }

  private static List<String> getRandomUsers() {
    var randomUsers = new ArrayList<String>();
    for (var table : DatabaseUtil.getSecMemberTableNames()) {
      var randomUser = DatabaseUtil.getRandomUser(table);
      randomUsers.add(randomUser);
    }
    return randomUsers;
  }

  private static void generateSecMemberEntries(int amountOfEntries) {
    for (var table : DatabaseUtil.getSecMemberTableNames()) {
      if (DatabaseUtil.entriesAlreadyExist(table, amountOfEntries)) {
        System.out.println("Entries already exist in table: " + table);
        continue;
      }
      System.out.println("Generating " + amountOfEntries + " members for table: " + table + "...");
      long startTime = System.nanoTime();
      for (var i = 0; i < amountOfEntries; i++) {
        DatabaseUtil.insertSecurityMemberToDb(table, "USER-" + i);
      }
      long elapsedTime = System.nanoTime() - startTime;
      var seconds = (double) elapsedTime / 1000000000.0;
      System.out.println("Created " + amountOfEntries + " entries after " + seconds + "s, in: " + table + "\n");
    }
  }

  public static void compare() {
    var randomUsers = getRandomUsers();
    var columns = List.of("UserId", "UserUuid", "UserRawUuid");
    for (int i = 0; i < randomUsers.size(); i++) {
      long startTime = System.nanoTime();
      var result = DatabaseUtil.findTasks(columns.get(i), randomUsers.get(i));
      long elapsedTime = System.nanoTime() - startTime;
      var seconds = (double) elapsedTime / 1000000000.0;
      System.out.println("Column: " + columns.get(i) + " | User: " + randomUsers.get(i) + " | Time: " + seconds + "s");
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
}
