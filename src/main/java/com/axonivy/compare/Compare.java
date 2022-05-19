package com.axonivy.compare;

public class Compare {

  public static void main(String[] args) {
    DatabaseUtil.create();
    System.out.println(DatabaseUtil.read());
    System.out.println(DatabaseUtil.getTables());
    System.out.println("script finished...");
  }
}
