package com.axonivy.compare;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Compare {

  public static void main(String[] args) {
    Menu.printMainMenu();
  }

  public static void prepareDb() {
    DatabaseUtil.create();
    var amountOfEntries = Menu.getIntInput("How many Database entries do you want to create?: ");
    generateSecMemberEntries(amountOfEntries);
    generateTasks(amountOfEntries);
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
      for (var i = 0; i < amountOfEntries; i++) {
        DatabaseUtil.insertSecurityMemberToDb(table, "USER-" + i);
      }
      System.out.println("Created " + amountOfEntries + " entries in: " + table + "\n");
    }
  }

  public static void compare() {
    System.out.println("wip");

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
