package com.axonivy.compare;

import java.io.IOException;
import java.util.Scanner;

public class Menu {
  static final Scanner scan = new Scanner(System.in);

  private static void printFile(String filename) {
    var inputStream = Menu.class.getClassLoader().getResourceAsStream("menus/" + filename);
    try {
      assert inputStream != null;
      var lines = new String(inputStream.readAllBytes());
      System.out.println(lines);
    } catch (IOException e) {
      System.out.println("Error reading file: " + filename + ": " + e.getMessage());
      System.exit(0);
    }
  }

  public static int getIntInput(String prompt) {
    var input = getInput(prompt);
    try {
      return Integer.parseInt(input);
    } catch (NumberFormatException e) {
      System.out.println("Invalid input: " + input);
      System.out.println("Input has to be a number.");
      return getIntInput(prompt);
    }
  }

  public static String getInput(String prompt) {
    System.out.print(prompt);
    return getInput();
  }

  public static String getInput() {
    return scan.nextLine();
  }

  public static void printMainMenu() {
    clearConsole();
    printFile("MainMenu.txt");
    System.out.print("Enter your choice: ");
    try {
      processInput(Integer.parseInt(scan.nextLine()));
    } catch (NumberFormatException e) {
      System.out.println("Input has to be a number.");
      printMainMenu();
    }
  }

  private static void processInput(int input) {
    switch (input) {
      case 1 -> Compare.compare(null);
      case 2 -> Compare.prepareDb();
      case 3 -> DatabaseCreateUtil.cleanupDatabase();
      case 4 -> System.exit(0);
      default -> System.out.println("Invalid input!");
    }
    waitForEnterPress();
    printMainMenu();
  }

  public static void waitForEnterPress() {
    Scanner s = new Scanner(System.in);
    System.out.println("\nPress enter to continue...");
    s.nextLine();
  }

  private static void clearConsole() {
    System.out.print("\033[H\033[2J");
    System.out.flush();
  }
}
