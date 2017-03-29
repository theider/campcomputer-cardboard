package com.instrument.cardboard;

import android.os.AsyncTask;
import android.util.Log;

import com.instrument.http.HttpClient;
import com.instrument.http.HttpClientRequest;
import com.instrument.http.HttpClientRequestException;
import com.instrument.http.HttpClientResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by theider on 1/30/15.
 */
public class FetchAssetsProcess extends AsyncTask<AssetRequest, Void, List<AssetRequest>> {

    private static final String TAG = "AssetFetcher";

    private FetchAssetsProcessListener fetchProcessListener;

    public void setFetchProcessListener(FetchAssetsProcessListener fetchProcessListener) {
        this.fetchProcessListener = fetchProcessListener;
    }

    private final List<AssetRequest> assetRequests = new ArrayList<AssetRequest>();

    protected class ProgressUpdateProcess implements Runnable {

        private final int assetCount;

        private final int totalAssets;

        public ProgressUpdateProcess(int assetCount, int totalAssets) {
            this.assetCount = assetCount;
            this.totalAssets = totalAssets;
        }

        @Override
        public void run() {
            if(fetchProcessListener != null) {
                fetchProcessListener.onAssetFetchProgress(assetCount,totalAssets);
            }
        }
    }

    @Override
    protected List<AssetRequest> doInBackground(AssetRequest... params) {
        Log.d(TAG, "starting batch asset fetch process");
        int totalAssets = params.length;
        int assetCount = 0;
        try {
            for (AssetRequest assetRequest : params) {
                assetRequests.add(assetRequest);
                Log.d(TAG, "request asset " + assetRequest + " ... ");
                HttpClientRequest request = assetRequest.getClientRequest();
                HttpClientResponse response = HttpClient.submit(request);
                // got star map?
                byte[] data = response.getData();
                String assetText = new String(data);
                Log.d(TAG, "received asset data size=" + data.length);
                assetRequest.setAssetData(assetText);
                assetRequest.setCompleted(true);
                // on error the asset will not be marked completed.
                Log.d(TAG, " - OK");
                assetCount++;
                // UI update in a thread.
                new Thread(new ProgressUpdateProcess(assetCount, totalAssets)).start();
            }
            Log.d(TAG, "completed batch asset fetch process");
            return assetRequests;
        } catch (IOException ex) {
            Log.e(TAG, "HTTP IO error", ex);
            // we're done here.
            if (fetchProcessListener != null) {
                fetchProcessListener.onAssetsFetchFailure("server IO error");
            }
            return null;
        } catch (HttpClientRequestException ex) {
            Log.e(TAG, "HTTP error", ex);
            if (fetchProcessListener != null) {
                fetchProcessListener.onAssetsFetchFailure("server request failure");
            }
            return null;
        }
    }

    @Override
    protected void onPostExecute(List<AssetRequest> assetRequests) {
        if(fetchProcessListener != null) {
            // returns null on error
            fetchProcessListener.onAssetsFetchComplete(assetRequests);
        }
    }
}


