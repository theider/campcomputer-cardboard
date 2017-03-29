package com.instrument.http;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpClient {

    private static final String TAG = "HttpClient";

    public static HttpClientResponse submit(HttpClientRequest request) throws IOException,HttpClientRequestException {
        HttpURLConnection connection = (HttpURLConnection) new URL(request.getRequestURI()).openConnection();
        connection.setConnectTimeout(120000);
        String requestMethod = request.getRequestMethod().name();
        connection.setRequestMethod(requestMethod);
        Log.d(TAG, "HTTP " + requestMethod + " request " + request);
        if(request.getRequestMethod() == HttpClientRequestMethod.POST) {
            connection.setDoOutput(true); // Triggers POST REQUEST.
        }
        if(connection.getResponseCode() != 200) {
            HttpClientRequestException ex = new HttpClientRequestException("remote HTTP failure:" + connection.getResponseCode());
            ex.setResponseCode(connection.getResponseCode());
            throw ex;
        }
        InputStream in = connection.getInputStream();
        try {
            // response is JSON
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            byte[] data = new byte[16384];
            int r;
            int c = 0;
            do {
                r = in.read(data);
                if(r > 0) {
                    bout.write(data,0,r);
                    c += r;
                }
            } while(r > 0);
            if(connection.getResponseCode() == 200) {
                Log.d(TAG, " -- response 200 OK " + c + " + data bytes");
                HttpClientResponse response = new HttpClientResponse();
                response.setData(bout.toByteArray());
                response.setDataLength(c);
                return response;
            } else {
                Log.d(TAG, " -- response " + connection.getResponseCode() + " ERR");
                HttpClientRequestException ex = new HttpClientRequestException("HTTP failure");
                ex.setResponseCode(connection.getResponseCode());
                throw ex;
            }
        } finally {
            if(in != null) {
                in.close();
            }
        }
    }

}
