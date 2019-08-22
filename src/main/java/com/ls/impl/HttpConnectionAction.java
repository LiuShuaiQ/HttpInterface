package com.ls.impl;

import com.ls.HttpAction;
import com.ls.inner.HttpCallback;
import com.ls.inner.HttpReqData;
import com.ls.inner.KVPair;
import com.ls.util.LogUtil;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class HttpConnectionAction implements HttpAction {

  public String request(HttpReqData data, HttpCallback callback) {
    try {
      StringBuilder tempParams = new StringBuilder();
      if (data.getParams() != null) {
        for (int i = 0; i < data.getParams().size(); i++) {
          if (i > 0) {
            tempParams.append("&");
          }
          tempParams.append(
              String.format("%s=%s", data.getParams().get(i).getKey(),
                  URLEncoder.encode(data.getParams().get(i).getValue(), "utf-8")));
        }
      }
      LogUtil.d("request param ==> " + tempParams);
      String requestUrl;
      if (data.getUrl().charAt(data.getUrl().length() - 1) == '?') {
        requestUrl = data.getUrl() + tempParams.toString();
      } else {
        requestUrl = data.getUrl() + "?" + tempParams.toString();
      }
      LogUtil.d("request url ==> " + requestUrl);
      URL connURL = new URL(requestUrl);
      HttpURLConnection urlConn = (HttpURLConnection) connURL.openConnection();
      urlConn.setConnectTimeout(5 * 1000);
      urlConn.setReadTimeout(5 * 1000);
      urlConn.setUseCaches(true);
      urlConn.setRequestMethod(data.getHttpMethod().getValue());
      if (data.getHeaders() != null) {
        for (int i = 0; i < data.getHeaders().size(); i++) {
          urlConn.setRequestProperty(data.getHeaders().get(i).getKey(),
              data.getHeaders().get(i).getValue());
        }
      }
      if (callback != null) {
        callback.onStart();
      }
      urlConn.connect();
      int respCode = urlConn.getResponseCode();
      if (respCode == 200) {
        //long contentLength = Long.parseLong(urlConn.getHeaderField("Content-Length"));
        String result = streamToString(urlConn.getInputStream(), callback);
        if (callback != null) {
          callback.onResponse(result);
        }
        LogUtil.d("request success, result--->" + result);
        return result;
      } else {
        if (callback != null) {
          callback.onError(new IllegalStateException("Http resp code is " + respCode));
        }
        LogUtil.d("request fail, code--->" + respCode);
      }
      urlConn.disconnect();
    } catch (Exception e) {
      if (callback != null) {
        callback.onError(e);
      }
      LogUtil.d(e.toString());
    }
    return null;
  }

  public void downloadFile(HttpReqData data, String outFile, HttpCallback callback) {
    try {
      URL url = new URL(data.getUrl());
      HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
      urlConn.setConnectTimeout(2 * 1000);
      urlConn.setReadTimeout(2 * 1000);
      urlConn.setUseCaches(true);
      urlConn.setRequestMethod("GET");
      urlConn.setRequestProperty("Content-Type", "application/json");
      urlConn.addRequestProperty("Connection", "Keep-Alive");
      if (callback != null) {
        callback.onStart();
      }
      urlConn.connect();
      int respCode = urlConn.getResponseCode();
      if (respCode == 200) {
        LogUtil.d("download file ..." + data.getUrl());
        File descFile = new File(outFile);
        FileOutputStream fos = new FileOutputStream(descFile);
        byte[] buffer = new byte[1024];
        int len;
        long readLen = 0;
        InputStream inputStream = urlConn.getInputStream();
        while ((len = inputStream.read(buffer)) != -1) {
          readLen += len;
          if (callback != null) {
            callback.onProcess(readLen);
          }
          // 写到本地
          fos.write(buffer, 0, len);
        }
        fos.flush();
        fos.close();
        if (callback != null) {
          callback.onResponse(outFile);
        }
      } else {
        if (callback != null) {
          callback.onError(new IllegalStateException("Http resp code is " + respCode));
        }
        LogUtil.d("download file fail" + outFile);
      }
      urlConn.disconnect();
    } catch (Exception e) {
      if (callback != null) {
        callback.onError(e);
      }
      LogUtil.d(e.toString());
    }
  }

  public void uploadFile(HttpReqData data, String uploadFile, HttpCallback callback) {
    try {
      String baseUrl = data.getUrl();
      File file = new File(uploadFile);
      URL url = new URL(baseUrl);
      HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
      urlConn.setDoOutput(true);
      urlConn.setDoInput(true);
      urlConn.setUseCaches(false);
      urlConn.setConnectTimeout(5 * 1000);
      urlConn.setReadTimeout(5 * 1000);
      urlConn.setRequestMethod("POST");
      urlConn.setRequestProperty("connection", "Keep-Alive");
      urlConn.setRequestProperty("Accept-Charset", "UTF-8");
      urlConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + "*****");
      if (callback != null) {
        callback.onStart();
      }
      urlConn.connect();
      String name = file.getName();
      DataOutputStream requestStream = new DataOutputStream(urlConn.getOutputStream());
      requestStream.writeBytes("--" + "*****" + "\r\n");
      StringBuilder tempParams = new StringBuilder();
      tempParams.append(
          "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + name + "\"; ");
      int pos = 0;
      if (data.getParams() != null) {
        int size = data.getParams().size();
        for (KVPair<String, String> param : data.getParams()) {
          tempParams.append(String.format("%s=\"%s\"", param.getKey(), param.getValue(), "utf-8"));
          if (pos < size - 1) {
            tempParams.append("; ");
          }
        }
      }

      tempParams.append("\r\n");
      tempParams.append("Content-Type: application/octet-stream\r\n");
      tempParams.append("\r\n");
      String params = tempParams.toString();
      requestStream.writeBytes(params);
      FileInputStream fileInput = new FileInputStream(file);
      int bytesRead;
      byte[] buffer = new byte[1024];
      DataInputStream in = new DataInputStream(new FileInputStream(file));
      while ((bytesRead = in.read(buffer)) != -1) {
        requestStream.write(buffer, 0, bytesRead);
      }
      requestStream.writeBytes("\r\n");
      requestStream.flush();
      requestStream.writeBytes("--" + "*****" + "--" + "\r\n");
      requestStream.flush();
      fileInput.close();
      int statusCode = urlConn.getResponseCode();
      if (statusCode == 200) {
        String result = streamToString(urlConn.getInputStream(), callback);
        callback.onResponse(result);
        LogUtil.d("upload success, result--->" + result);
      } else {
        if (callback != null) {
          callback.onError(new IllegalStateException("Http resp code is " + statusCode));
        }
        LogUtil.d("upload fail");
      }
    } catch (IOException e) {
      LogUtil.d(e.toString());
      if (callback != null) {
        callback.onError(e);
      }
    }
  }

  private static String streamToString(InputStream is, HttpCallback listener) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024];
      int len = 0;
      long readLength = 0;
      while ((len = is.read(buffer)) != -1) {
        baos.write(buffer, 0, len);
        readLength += len;
        if (listener != null) {
          listener.onProcess(readLength);
        }
      }
      baos.close();
      is.close();
      byte[] byteArray = baos.toByteArray();
      return new String(byteArray);
    } catch (Exception e) {
      LogUtil.d(e.toString());
      return null;
    }
  }
}
