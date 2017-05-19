package com.example.android.rescuerobotremote.services;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.example.android.rescuerobotremote.activities.MainActivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.TimerTask;


/**
 * Created by colon on 3/24/17.
 */

public class NetworkClientService extends IntentService {

    private static final String TAG = "NetworkClientService";
    private String serverIPAddress;
    private int serverPort = 28800;
    Socket objClient;
    private DataOutputStream dout;
    private DataInputStream din;
    ObjectOutputStream oos;
    private TimerTask timerTask;
    private int heartBeatMsgRate = 2000;
    private Intent broadcastIntent;
    private IntentFilter filter;
    private ServiceBroadcastReceiver broadcastReceiver;


    // Default constructor that calls super with the name of the "Service"/class
    public NetworkClientService() {
        super("NetworkClientService");
    }

    @Override
    public void onCreate() {
        super.onCreate();


        Log.i(TAG, "Started Service onCreate() method");
        // Display a toast message when the IntentService is created
        Toast toast = Toast.makeText(getApplicationContext(), "Network Client Service Started",
                Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();

        // Create Intent to pass received messages to the Main Activity through local broadcast
        broadcastIntent = new Intent();

        // Register Service Broadcast Receiver
        registerBroadcastReceiver();


    }

    @Override
    protected void onHandleIntent(final Intent intent) {

        serverIPAddress = intent.getStringExtra("serverIP");

        try {
            objClient = new Socket(serverIPAddress, serverPort);
            dout = new DataOutputStream(objClient.getOutputStream());
            din = new DataInputStream(objClient.getInputStream());


        } catch (IOException e) {
            Log.e(TAG, "IOException error: ", e);
        }

        while (true) {
            int msgID;
            try {
                msgID = din.readInt();
                switch (msgID) {
                    case 101:
                        Log.d(TAG,"Received move forward message");
                        broadcastIntent.setAction(MainActivity.ResponseReceiver.SEND_FORWARD_RX);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
                        sendPacket(100);
                        break;
                    case 102:
                        Log.d(TAG,"Received move right message");
                        broadcastIntent.setAction(MainActivity.ResponseReceiver.SEND_RIGHT_RX);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
                        break;
                    case 103:
                        Log.d(TAG,"Received move backward message");
                        broadcastIntent.setAction(MainActivity.ResponseReceiver.SEND_BACKWARDS_RX);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
                        break;
                    case 104:
                        Log.d(TAG,"Received move left message");
                        broadcastIntent.setAction(MainActivity.ResponseReceiver.SEND_LEFT_RX);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
                        break;
                    default:
                        Log.d(TAG,"Message ID not recognized");
                        break;

                }
            } catch (IOException e) {
                Log.e(TAG, "IOException error: ", e);
            }
        }

    }

    private void sendPacket(int msgID) {

        switch (msgID) {
            case 100:
                try {
                    dout.writeInt(100);
                    dout.flush();
                    Log.d(TAG, "SENT MESSAGE ID "+msgID+": HEART_BEAT");
                } catch (IOException e) {
                    Log.e(TAG, "IOException error: ", e);
                }
                break;

            default:
                Log.d(TAG, "MESSAGE ID NOT FOUND");
                break;
        }
    }

    private void registerBroadcastReceiver() {
        // Register Local Service Broadcast Receiver
        filter = new IntentFilter();
        // Define type of Broadcast to filter
        filter.addAction(ServiceBroadcastReceiver.SEND_PICTURE_TX);
        filter.addAction(ServiceBroadcastReceiver.SEND_OBSTACLE);
        filter.addAction(ServiceBroadcastReceiver.SEND_LOCATION);
        filter.addAction(ServiceBroadcastReceiver.SEND_INCLINATION);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastReceiver = new ServiceBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter);
    }

    public class ServiceBroadcastReceiver extends BroadcastReceiver {

        // Define the Action to use with the Intent filter
        public static final String SEND_PICTURE_TX = "SEND_PICTURE_TX";
        public static final String SEND_INCLINATION = "SEND_INCLINATION";
        public static final String SEND_OBSTACLE = "SEND_OBSTACLE";
        public static final String SEND_LOCATION = "SEND_LOCATION";

        // Read data sent by the IntentService
        @Override
        public void onReceive(Context context, Intent intent) {

            // We need to check what type of the received broadcast message it is
            String action = intent.getAction();
            if (action.equals(SEND_PICTURE_TX))
            {
                String url = intent.getStringExtra("ImageURL");
                Log.d("ServiceBroadcastRx","Picture url: "+url);
                sendImage(url);
            }
            if (action.equals(SEND_OBSTACLE))
            {
                try {
                dout.writeInt(200); //200 means obstacle
                dout.flush();
                Log.d(TAG, "SENT OBSTACLE");
                } catch (IOException e) {
                    Log.e(TAG, "IOException error: ", e);
                }
            }
            if (action.equals(SEND_LOCATION))
            {
                String lat = intent.getStringExtra("lat");
                String lng = intent.getStringExtra("lng");
                String str = lat + " " + lng;
                sendlocationdata(str);

            }
            if (action.equals(SEND_INCLINATION))
            {
                int inc = intent.getIntExtra("inclination",5);
                byte b = (byte) inc;
               sendinc(b);

                ///////////////
                /*
                String lat = intent.getStringExtra("lat");
                String lng = intent.getStringExtra("lng");
                String str = lat + " " + lng + " " + String.valueOf(inc);
                sendlocationdata(str);
*/

            }

        }
    }

    private void sendinc(byte b)
    {
        try {
            byte [] buffer = new byte[1];
            buffer[0] = b;
            ObjectOutputStream oos = new ObjectOutputStream(objClient.getOutputStream());
            dout.writeInt(400);
            dout.flush();
            oos.writeObject(buffer);

        } catch (IOException e) {
            Log.e(TAG, "IOException error: ", e);
        }

    }

    private void sendlocationdata(String str){
        try {
            byte [] buffer = str.getBytes();
            ObjectOutputStream oos = new ObjectOutputStream(objClient.getOutputStream());
            dout.writeInt(300);
            dout.flush();
            oos.writeObject(buffer);


        } catch (IOException e) {
            Log.e(TAG, "IOException error: ", e);
        }
    }

    private void sendImage(String url){
        try {
            FileInputStream fis = new FileInputStream(url);
            byte [] buffer = new byte[fis.available()];
            fis.read(buffer);
            ObjectOutputStream oos = new ObjectOutputStream(objClient.getOutputStream());
            dout.writeInt(105);
            dout.flush();
            oos.writeObject(buffer);

        } catch (IOException e) {
            Log.e(TAG, "IOException error: ", e);
        }
    }

}
