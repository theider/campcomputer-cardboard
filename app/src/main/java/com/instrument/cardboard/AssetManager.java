package com.instrument.cardboard;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by theider on 2/3/15.
 */
public class AssetManager {

    private static final String TAG = "AssetManager";

    private final Map<String,byte[]> assetsMap = new HashMap<>();

    public void putAsset(String name,byte[] assetData) {
        synchronized(assetsMap) {
            assetsMap.put(name,assetData);
            Log.d(TAG, "added asset " + name + " data size=" + assetData.length);
        }
    }

    public byte[] getAsset(String name) {
        synchronized(assetsMap) {
            return assetsMap.get(name);
        }
    }

    private static AssetManager ourInstance = new AssetManager();

    public static AssetManager getInstance() {
        return ourInstance;
    }

    private AssetManager() {
    }
}
