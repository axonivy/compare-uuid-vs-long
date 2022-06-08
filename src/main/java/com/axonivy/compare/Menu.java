package com.axonivy.compare;

import java.io.IOException;
import java.util.Scanner;

public class Menu {

  private static void printFile(String filename) {
    var inputStream = Menu.class.getClassLoader().getResourceAsStream("menus/"+filename);
    try {
      assert inputStream != null;
      var lines = new String(inputStream.readAllBytes());
      System.out.println(lines);
    } catch (IOException e) {
      System.out.println("Error reading file: " + filename + ": " + e.getMessage());
      System.exit(0);
    }
  }

  public static void printMainMenu() {
    clearConsole();
    printFile("MainMenu.txt");
    System.out.print("Enter your choice: ");
    try (Scanner scanner = new Scanner(System.in)) {
      var choice = scanner.nextInt();
      processInput(choice);
    }
  }

  private static void processInput(int input) {
    switch (input) {
      case 1 -> createDb();
      case 2 -> compare();
      case 3 -> checkDb();
      case 4 -> System.exit(0);
      default -> {
        System.out.println("Invalid input!");
        printMainMenu();
      }
    }
  }

  private static void createDb() {
    DatabaseUtil.create();
  }

  private static void compare() {
    System.out.println("wait");
  }

  private static void checkDb() {
    var tables = DatabaseUtil.getTables();
    if (!tables.isEmpty()) {
      System.out.println("Tables: " + tables);
    } else {
      System.out.println("No tables found.");
    }
  }

  private static void clearConsole() {
    System.out.print("\033[H\033[2J");
    System.out.flush();
  }
}
