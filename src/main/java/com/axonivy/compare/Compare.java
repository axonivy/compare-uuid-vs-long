package com.axonivy.compare;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class Compare {

  public static void main(String[] args) {
    Menu.printMainMenu();
  }

  public static void prepareDb() {
    DatabaseCreateUtil.create();
    var amountOfEntries = Menu.getIntInput("How many Database entries do you want to create?: ");
    if (amountOfEntries < 1) {
      System.out.println("Invalid input!");
      return;
    }
    DatabaseCreateUtil.getDatabases().forEach(db -> DatabaseCreateUtil.generateSecMemberEntries(db, amountOfEntries));
    DatabaseCreateUtil.getDatabases().forEach(db -> DatabaseCreateUtil.massInsertTaskToDb(db, amountOfEntries));
  }


  public static void compareAllDbs() {
    var databases = DatabaseCreateUtil.getDatabases();
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
      for (var tableType : DatabaseCreateUtil.getTableTypeNames()) {
        var results = DatabaseMeasureUtil.measureFindingTasks(db, tableType);
        String keyType = String.format("%8s", tableType);
        var avgQueryTime = df.format(results.averageQueryTimeInMs());
        var maxQueryTime = df.format(results.maxTimeInMs() / 1000000);
        var minQueryTime = df.format(results.minTimeInMs() / 1000000);
        var rowsFound = df.format(results.averageRowsFound());
        System.out.println("Key Type: " + keyType + " | Query times: avg: " + avgQueryTime + "ms," +
                " min: " + minQueryTime + "ms, max: " + maxQueryTime + "ms | Avg. Rows found: " + rowsFound);
      }
    }
  }

}
