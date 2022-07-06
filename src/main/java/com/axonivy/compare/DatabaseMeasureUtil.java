package com.axonivy.compare;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class DatabaseMeasureUtil {

  public static QueryPerformanceMeasureResult measureFindingTasks(Database db, String tableType) {
    var queryTimes = new ArrayList<Long>();
    var rowCounts = new ArrayList<Integer>();
    var randomUsers = DatabaseCreateUtil.getRandomUsersWithTask(db, tableType, 100);
    var testCount = 100;

    try (Connection connection = DatabaseCreateUtil.getConnection(db)) {
      for (int j = 0; j < testCount; j++) {
        var rowCount = 0;
        var user = randomUsers.get(new Random().nextInt(randomUsers.size()));
        try (PreparedStatement statement = connection.prepareStatement(getMeasureQuery(tableType))) {
          setStatementParameter(tableType, statement, user);
          long startTime = System.nanoTime();
          try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
              if (resultSet.getRow() < 1) {
                throw new RuntimeException("ResultSet is empty.");
              }
              resultSet.getLong(1);
              rowCount++;
            }
          }
          var elapsedTime = System.nanoTime() - startTime;
          rowCounts.add(rowCount);
          queryTimes.add(elapsedTime);
        }

      }
    } catch (Exception e) {
      DatabaseCreateUtil.isConnectionError(e);
    }
    var averageQueryTime = !queryTimes.isEmpty() ? queryTimes.stream().mapToLong(Long::longValue).average().getAsDouble() / 1000000 : 0;
    var averageRowCount = !rowCounts.isEmpty() ? rowCounts.stream().mapToInt(Integer::intValue).average().getAsDouble() : 0;
    return new QueryPerformanceMeasureResult(averageQueryTime, Collections.max(queryTimes), Collections.min(queryTimes), averageRowCount);
  }

  private static String getMeasureQuery(String tableType) {
    var tskTbl = "Task" + tableType;
    var secTbl = "SecurityMember" + tableType;
    return "SELECT " + tskTbl + ".Id, " + secTbl + ".Id FROM " + tskTbl +
            " INNER JOIN " + secTbl + " ON " + tskTbl + ".UserId = " + secTbl + ".Id" +
            " WHERE " + secTbl + ".Id = ?";
  }

  private static void setStatementParameter(String tableType, PreparedStatement statement, String user) throws SQLException {
    if (tableType.equals("Long")) {
      statement.setInt(1, Integer.parseInt(user));
    } else {
      statement.setString(1, user);
    }
  }

}