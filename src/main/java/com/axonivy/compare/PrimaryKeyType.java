package com.axonivy.compare;

public enum PrimaryKeyType {
  LONG("BIGINT"),
  STRING("VARCHAR(255)");

  private final String value;

  PrimaryKeyType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

}
