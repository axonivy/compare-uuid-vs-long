package com.axonivy.compare;

public record QueryPerformanceMeasureResult(double averageQueryTimeInMs, double maxTimeInMs, double minTimeInMs, double averageRowsFound) {}
