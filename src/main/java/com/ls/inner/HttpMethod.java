package com.ls.inner;

public enum HttpMethod {
  GET("GET"), POST("POST"), OPTION("OPTION"),
  HEAD("HEAD"), PUT("PUT"), DELETE("DELETE");

  String value;

  HttpMethod(String value) {
    this.value = value;
  }

  public String getValue(){
    return value;
  }
}
