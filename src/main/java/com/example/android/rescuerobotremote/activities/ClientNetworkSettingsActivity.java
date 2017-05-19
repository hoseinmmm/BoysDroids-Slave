package com.example.android.rescuerobotremote.activities;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.android.rescuerobotremote.R;
import com.example.android.rescuerobotremote.services.NetworkClientService;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by colon on 3/27/17.
 */

public class ClientNetworkSettingsActivity extends AppCompatActivity {

        private static final String TAG = "NetworkSettingsActivity";
        private IntentFilter filter;
        private WifiManager wifiManager;
        private String ssid;
        private int rssi;
        private String localIPAddress;
        private boolean isConnected;
        private TimerTask timerTask;
        private int activityRefreshRate = 2000;


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_settings);


            final Button startNetService = (Button) findViewById(R.id.start_network_client_button);
            startNetService.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent netClientIntent = new Intent(getApplication(),NetworkClientService.class);
                    EditText editText = (EditText) findViewById(R.id.editText_remote_IP);
                    netClientIntent.putExtra("serverIP",editText.getText().toString());
                    startService(netClientIntent);
                    startNetService.setEnabled(false);
                }
                });

            // First check if we have network connection
            final ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

            Timer timerObj = new Timer();
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                    if (isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
                        // Make sure connection is WIFI
                        if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                            Log.i(TAG, "Device is connected to Wi-Fi network");

                            // This run on UI thread method is needed to update the Views
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    refreshNetworkStatusViews();
                                }
                            });
                        } else {
                            Log.d(TAG, "Device connected to mobile network");
                        }
                    } else {
                        // This run on UI thread method is needed to update the Views
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                networkStatusDefaultViews();
                            }
                        });
                    }

                }
            };
            timerObj.schedule(timerTask, 0, activityRefreshRate);

        }

    private int getRSSI(WifiInfo wifiInfo) {
        return wifiInfo.getRssi();
    }

    private String getSSID(WifiInfo wifiInfo) {
        return wifiInfo.getSSID();
    }

    private String getLocalIPAddress(WifiInfo wifiInfo) {

        int ipAddress = wifiInfo.getIpAddress();
        return String.format(Locale.getDefault(), "%d.%d.%d.%d",
                (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    }

    public void networkStatusDefaultViews() {

        TextView textViewNetStatus = (TextView) findViewById(R.id.network_status_textview);
        textViewNetStatus.setText("No Connection");
        TextView textViewSSID = (TextView) findViewById(R.id.network_ssid_textview);
        textViewSSID.setText("N/A");
        TextView textViewRSSI = (TextView) findViewById(R.id.network_rssi_textview);
        textViewRSSI.setText("N/A");
        TextView textViewLocalIP = (TextView) findViewById(R.id.local_ip_textview);
        textViewLocalIP.setText("N/A");


    }

    private void refreshNetworkStatusViews() {
        // WIFIManager must use Application Context and not Activity Context
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo w = wifiManager.getConnectionInfo();

        ssid = getSSID(w);
        rssi = getRSSI(w);
        localIPAddress = getLocalIPAddress(w);

        Log.d(TAG, "Wifi SSID: " + ssid);
        Log.d(TAG, "Wifi RSSI: " + rssi);
        Log.d(TAG, "Wifi Local IP:" + localIPAddress);
        TextView textViewNetStatus = (TextView) findViewById(R.id.network_status_textview);
        textViewNetStatus.setText("Connected to Wi-Fi");
        TextView textViewSSID = (TextView) findViewById(R.id.network_ssid_textview);
        textViewSSID.setText(ssid);
        TextView textViewRSSI = (TextView) findViewById(R.id.network_rssi_textview);
        textViewRSSI.setText(String.valueOf(rssi) + " dBm");
        TextView textViewLocalIP = (TextView) findViewById(R.id.local_ip_textview);
        textViewLocalIP.setText(localIPAddress);

    }

    @Override
    protected void onPause() {
        super.onPause();
        timerTask.cancel(); // stop timer when View is paused
    }

    @Override
    protected void onResume(){
        super.onResume();
        timerTask.run();
    }



}
