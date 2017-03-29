package com.instrument.http;

/**
 * Created by theider on 1/29/15.
 */
public class HttpClientRequestException extends Exception {

    public HttpClientRequestException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public HttpClientRequestException(String detailMessage) {
        super(detailMessage);
    }

    private int responseCode;

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

}
