package com.instrument.cardboard;

import java.util.List;

/**
 * Created by theider on 1/30/15.
 */
public interface FetchAssetsProcessListener {

    public void onAssetFetchProgress(int completed,int total);

    public void onAssetsFetchComplete(List<AssetRequest> assetRequests);

    public void onAssetsFetchFailure(String errorMessage);

}
