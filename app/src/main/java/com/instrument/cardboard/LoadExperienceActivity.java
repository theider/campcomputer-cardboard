package com.instrument.cardboard;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.instrument.http.HttpClientRequest;
import com.instrument.http.HttpClientRequestMethod;

import java.util.List;

/**
 * Created by theider on 1/30/15.
 */
public class LoadExperienceActivity extends ActionBarActivity implements FetchAssetsProcessListener {

    private static final String TAG = "LoadExperienceActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(LoadExperienceActivity.this, MainActivity.class);
        startActivity(intent);

        setContentView(R.layout.load_experience_view);
        // run fetch in background.  when it's done we'll spawn the cardboard view.
//        FetchAssetsProcess assetsFetch = new FetchAssetsProcess();
//        assetsFetch.setFetchProcessListener(this);
//        assetsFetch.execute(//new AssetRequest("starmap_data",new HttpClientRequest(HttpClientRequestMethod.GET,"http://10.0.2.105:8080/jumpjam-server/webresources/starmap/csv")),
//                new AssetRequest("starmap_v0",new HttpClientRequest(HttpClientRequestMethod.GET,"http://10.0.2.105:8080/jumpjam-server/media?resource_name=starmap-vertex0.glsl&resource_type=VERTEX_SHADER")),
//                new AssetRequest("starmap_v1",new HttpClientRequest(HttpClientRequestMethod.GET,"http://10.0.2.105:8080/jumpjam-server/media?resource_name=starmap-vertex1.glsl&resource_type=VERTEX_SHADER")),
//                new AssetRequest("starmap_v2",new HttpClientRequest(HttpClientRequestMethod.GET,"http://10.0.2.105:8080/jumpjam-server/media?resource_name=starmap-vertex2.glsl&resource_type=VERTEX_SHADER")),
//                new AssetRequest("starmap_v3",new HttpClientRequest(HttpClientRequestMethod.GET,"http://10.0.2.105:8080/jumpjam-server/media?resource_name=starmap-vertex3.glsl&resource_type=VERTEX_SHADER")),
//                new AssetRequest("starmap_f0",new HttpClientRequest(HttpClientRequestMethod.GET,"http://10.0.2.105:8080/jumpjam-server/media?resource_name=starmap-fragment0.glsl&resource_type=FRAGMENT_SHADER")),
//                new AssetRequest("starmap_f1",new HttpClientRequest(HttpClientRequestMethod.GET,"http://10.0.2.105:8080/jumpjam-server/media?resource_name=starmap-fragment1.glsl&resource_type=FRAGMENT_SHADER")),
//                new AssetRequest("starmap_f2",new HttpClientRequest(HttpClientRequestMethod.GET,"http://10.0.2.105:8080/jumpjam-server/media?resource_name=starmap-fragment2.glsl&resource_type=FRAGMENT_SHADER")),
//                new AssetRequest("starmap_f3",new HttpClientRequest(HttpClientRequestMethod.GET,"http://10.0.2.105:8080/jumpjam-server/media?resource_name=starmap-fragment3.glsl&resource_type=FRAGMENT_SHADER")));
    }

    @Override
    public void onAssetFetchProgress(int completed, int total) {
        Log.d(TAG, "progress " + completed + " of " + total);
        ProgressBar progBar = (ProgressBar) findViewById(R.id.progressBar);
        progBar.setMax(total);
        progBar.setProgress(completed);
    }


    @Override
    public void onAssetsFetchComplete(List<AssetRequest> assetRequests) {
        if(assetRequests != null) {
            for(AssetRequest req : assetRequests) {
                byte[] data = req.getAssetData().getBytes();
                AssetManager.getInstance().putAsset(req.getAssetId(),data);
            }
            Log.i(TAG, "start Stereo view");
            Intent intent = new Intent(LoadExperienceActivity.this, PearCardboardActivity.class);
            startActivity(intent);
        }

    }

    protected class ShowFailure implements Runnable {

        private final String errorMessage;

        public ShowFailure(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        @Override
        public void run() {
            Context context = getApplicationContext();
            CharSequence text = errorMessage;
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            // back to main
            Intent intent = new Intent(LoadExperienceActivity.this, MainActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onAssetsFetchFailure(String errorMessage) {
        // coming from a worker thread.
        runOnUiThread(new ShowFailure(errorMessage));
    }
}
