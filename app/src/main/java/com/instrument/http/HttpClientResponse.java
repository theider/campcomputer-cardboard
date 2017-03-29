package com.instrument.http;

import java.util.Arrays;

/**
 * Created by theider on 1/29/15.
 */
public class HttpClientResponse {

    private byte[] data;
    private int dataLength;

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getDataLength() {
        return dataLength;
    }

    public void setDataLength(int dataLength) {
        this.dataLength = dataLength;
    }

    @Override
    public String toString() {
        return "HttpClientResponse{" +
                "data=" + Arrays.toString(data) +
                ", dataLength=" + dataLength +
                '}';
    }

}
