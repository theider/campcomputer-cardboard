package com.instrument.cardboard;

import com.instrument.http.HttpClientRequest;

/**
 * Created by theider on 1/30/15.
 */
public class AssetRequest {

    private String assetId;

    private HttpClientRequest clientRequest;

    private boolean completed;

    private String assetData;

    public String getAssetData() {
        return assetData;
    }

    public void setAssetData(String assetData) {
        this.assetData = assetData;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public AssetRequest(String assetId, HttpClientRequest clientRequest) {
        this.assetId = assetId;
        this.clientRequest = clientRequest;
    }

    public String getAssetId() {
        return assetId;
    }

    public HttpClientRequest getClientRequest() {
        return clientRequest;
    }

    @Override
    public String toString() {
        return "AssetRequest{" +
                "assetId='" + assetId + '\'' +
                ", clientRequest=" + clientRequest +
                '}';
    }
}
