package com.instrument.http;

/**
 * Created by theider on 1/29/15.
 */
public class HttpClientRequest {

    private HttpClientRequestMethod requestMethod;

    private String requestURI;

    public HttpClientRequest(HttpClientRequestMethod requestMethod, String requestURI) {
        this.requestMethod = requestMethod;
        this.requestURI = requestURI;
    }

    public String getRequestURI() {
        return requestURI;
    }

    public void setRequestURI(String requestURI) {
        this.requestURI = requestURI;
    }

    public HttpClientRequestMethod getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(HttpClientRequestMethod requestMethod) {
        this.requestMethod = requestMethod;
    }

    @Override
    public String toString() {
        return "HttpClientRequest{" +
                "requestURI='" + requestURI + '\'' +
                ", requestMethod=" + requestMethod +
                '}';
    }
}
