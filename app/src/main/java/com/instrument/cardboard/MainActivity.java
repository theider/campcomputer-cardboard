package com.instrument.cardboard;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.content.Context;
import android.net.NetworkInfo;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;


public class MainActivity extends ActionBarActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(MainActivity.this, LoadExperienceActivity.class);
//            intent.putExtra("PLAYER_POSITION", playerPositionText);
//            intent.putExtra("SERVER_IP_ADDRESS", serverIpAddressText);
        startActivity(intent);

    }

    public void onConnectButtonClicked(View view) {
        EditText inputCodeEditText = (EditText) findViewById(R.id.playerCodeEditText);
        String playerPositionText = inputCodeEditText.getText().toString();
        EditText inputServerIpAddressText = (EditText) findViewById(R.id.serverIpAddressEditText);
        String serverIpAddressText = inputServerIpAddressText.getText().toString();


        // check network
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo == null && networkInfo.isConnected()) {
            // display error
            Log.e(TAG, "network is not active");
        } else {
            Log.i(TAG, "start Stereo view");
            Intent intent = new Intent(MainActivity.this, LoadExperienceActivity.class);
//            intent.putExtra("PLAYER_POSITION", playerPositionText);
//            intent.putExtra("SERVER_IP_ADDRESS", serverIpAddressText);
            startActivity(intent);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
