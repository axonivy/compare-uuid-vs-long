package com.axonivy.compare;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class Compare {

  public static void main(String[] args) {
    Menu.printMainMenu();
  }

  public static void prepareDb() {
    DatabaseCreateUtil.create();
    var amountOfEntries = Menu.getIntInput("How many Database Task entries do you want to create?: ");
    if (amountOfEntries < 1) {
      System.out.println("Invalid input!");
      return;
    }
    DatabaseCreateUtil.getDatabases().forEach(db -> DatabaseCreateUtil.generateSecMemberEntries(db, Math.min(amountOfEntries/10, 100000)));
    DatabaseCreateUtil.getDatabases().forEach(db -> DatabaseCreateUtil.massInsertTaskToDb(db, amountOfEntries));
    DatabaseCreateUtil.updateIndexStatisticsForQueryAnalyzer();
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
      DecimalFormat df = new DecimalFormat("#.###");
      df.setRoundingMode(RoundingMode.CEILING);
      System.out.println("\nComparing tables in " + db.type() + " ...");
      for (var tableType : DatabaseCreateUtil.getTableTypeNames()) {
        var results = PerformanceTest.measureFindingTasks(db, tableType);
        var avgQueryTime = String.format("%6s", df.format(results.averageQueryTimeInMs()));
        var maxQueryTime = String.format("%6s", df.format(results.maxTimeInMs() / 1000000));
        var minQueryTime = String.format("%6s", df.format(results.minTimeInMs() / 1000000));
        var rowsFound = df.format(results.averageRowsFound());
        String keyType = String.format("%7s", tableType);
        System.out.println("Key Type: " + keyType + " | Query times: avg: " + avgQueryTime + "ms," +
                " min: " + minQueryTime + "ms, max: " + maxQueryTime + "ms | Avg. Rows found: " + rowsFound);
      }
    }
  }

}
