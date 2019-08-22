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
      LogUtil.d("请求param ==> " + tempParams);
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
        LogUtil.d("Get方式请求成功，result--->" + result);
        return result;
      } else {
        if (callback != null) {
          callback.onError(new IllegalStateException("Http resp code is " + respCode));
        }
        LogUtil.d("Get方式请求失败，code--->" + respCode);
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
      // 新建一个URL对象
      URL url = new URL(data.getUrl());
      // 打开一个HttpURLConnection连接
      HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
      // 设置连接主机超时时间
      urlConn.setConnectTimeout(2 * 1000);
      //设置从主机读取数据超时
      urlConn.setReadTimeout(2 * 1000);
      // 设置是否使用缓存  默认是true
      urlConn.setUseCaches(true);
      // 设置为Post请求
      urlConn.setRequestMethod("GET");
      //urlConn设置请求头信息
      //设置请求中的媒体类型信息。
      urlConn.setRequestProperty("Content-Type", "application/json");
      //设置客户端与服务连接类型
      urlConn.addRequestProperty("Connection", "Keep-Alive");
      // 开始连接
      if (callback != null) {
        callback.onStart();
      }
      urlConn.connect();
      // 判断请求是否成功
      //            156635325090151
      //            156635325110866
      //            156635325111255
      //156643625257535
      int respCode = urlConn.getResponseCode();
      if (respCode == 200) {
        LogUtil.d("文件开始下载" + data.getUrl());
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
        LogUtil.d("文件下载失败" + outFile);
      }
      // 关闭连接
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
      //新建url对象
      URL url = new URL(baseUrl);
      //通过HttpURLConnection对象,向网络地址发送请求
      HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
      //设置该连接允许读取
      urlConn.setDoOutput(true);
      //设置该连接允许写入
      urlConn.setDoInput(true);
      //设置不能适用缓存
      urlConn.setUseCaches(false);
      //设置连接超时时间
      urlConn.setConnectTimeout(5 * 1000);   //设置连接超时时间
      //设置读取超时时间
      urlConn.setReadTimeout(5 * 1000);   //读取超时
      //设置连接方法post
      urlConn.setRequestMethod("POST");
      //设置维持长连接
      urlConn.setRequestProperty("connection", "Keep-Alive");
      //设置文件字符集
      urlConn.setRequestProperty("Accept-Charset", "UTF-8");
      //设置文件类型
      urlConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + "*****");
      if (callback != null) {
        callback.onStart();
      }
      urlConn.connect();
      String name = file.getName();
      DataOutputStream requestStream = new DataOutputStream(urlConn.getOutputStream());
      requestStream.writeBytes("--" + "*****" + "\r\n");
      //发送文件参数信息
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
      //发送文件数据
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
        // 获取返回的数据
        String result = streamToString(urlConn.getInputStream(), callback);
        callback.onResponse(result);
        LogUtil.d("上传成功，result--->" + result);
      } else {
        if (callback != null) {
          callback.onError(new IllegalStateException("Http resp code is " + statusCode));
        }
        LogUtil.d("上传失败");
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
