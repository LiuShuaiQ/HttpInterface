package com.ls;

import com.ls.inner.HttpCallback;
import com.ls.inner.HttpReqData;

public interface HttpAction {
  String request(HttpReqData data, HttpCallback callback);

  void downloadFile(HttpReqData data, String outFile, HttpCallback callback);

  void uploadFile(HttpReqData data, String uploadFile, HttpCallback callback);
}
