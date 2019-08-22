package com.ls.inner;

import java.util.List;

public class HttpReqData {
  private String mUrl = null;
  private HttpMethod mHttpMethod = HttpMethod.GET;
  private List<KVPair<String, String>> mHeaders = null;
  private List<KVPair<String, String>> mParams = null;

  private HttpReqData() {
  }

  public String getUrl() {
    return mUrl;
  }

  public List<KVPair<String, String>> getHeaders() {
    return mHeaders;
  }

  public List<KVPair<String, String>> getParams() {
    return mParams;
  }

  public HttpMethod getHttpMethod() {
    return mHttpMethod;
  }

  @Override public String toString() {
    return "HttpReqData{" +
        "mUrl='" + mUrl + '\'' +
        ", mHeaders=" + mHeaders +
        ", mParams=" + mParams +
        '}';
  }

  public static class Build {
    private String mUrl;
    private List<KVPair<String, String>> mHeaders;
    private List<KVPair<String, String>> mParams;
    private HttpMethod mHttpMethod;

    public Build setmUrl(String mUrl) {
      this.mUrl = mUrl;
      return this;
    }

    public Build setmHeaders(List<KVPair<String, String>> mHeaders) {
      this.mHeaders = mHeaders;
      return this;
    }

    public Build setmParams(List<KVPair<String, String>> mParams) {
      this.mParams = mParams;
      return this;
    }

    public Build setHttpMethod(HttpMethod httpMethod) {
      mHttpMethod = httpMethod;
      return this;
    }

    public Build get() {
      mHttpMethod = HttpMethod.GET;
      return this;
    }

    public Build post() {
      mHttpMethod = HttpMethod.POST;
      return this;
    }

    public HttpReqData build() {
      HttpReqData httpReqData = new HttpReqData();
      httpReqData.mUrl = this.mUrl;
      httpReqData.mHeaders = this.mHeaders;
      httpReqData.mParams = this.mParams;
      return httpReqData;
    }
  }
}
