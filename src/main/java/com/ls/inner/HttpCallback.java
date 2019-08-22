package com.ls.inner;

public interface HttpCallback {
  void onStart();

  void onProcess(long count);

  void onResponse(String result);

  void onError(Exception e);
}
