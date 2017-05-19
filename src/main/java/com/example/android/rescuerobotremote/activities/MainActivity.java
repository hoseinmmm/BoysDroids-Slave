package com.example.android.rescuerobotremote.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.rescuerobotremote.R;
import com.example.android.rescuerobotremote.listener.OnPictureCapturedListener;
import com.example.android.rescuerobotremote.services.NetworkClientService;
import com.example.android.rescuerobotremote.services.PictureService;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;


import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static android.R.attr.filter;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;



public class MainActivity extends AppCompatActivity implements OnPictureCapturedListener, SensorEventListener, ActivityCompat.OnRequestPermissionsResultCallback {


    public int total_commands =0;
    DatabaseHelper mOpenHelper;

    public int wifiactive = 1;
    public boolean newcommand = false;
    public int savedinclination = 5;

    //The information about the SQLite Database.
    public static final String DATABASE_NAME = "dbForTest.db";
    public static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "SQLiteTest";
    public static final String COMMAND = "command1";
    public static final String DIRECTION = "direction1";



    private static final String TAG = "MainActivity";

    public static final int MY_PERMISSIONS_REQUEST_ACCESS_CODE = 1;
    private ImageView uploadBackPhoto;
    private ImageView uploadFrontPhoto;
    private TimerTask timerTaskIMageCapture;
    private TimerTask timerTaskwifi;
    private int captureImageRate = 5000;

    private ResponseReceiver broadcastReceiverWifi;
    private Intent broadcastIntent;
    private IntentFilter filterWifi;


    public double lat, lng;

    public Location loc1 = new Location("");

    private LocationManager manager;
    private LocationListener listener;


    Sensor A;
    SensorManager AM;
        Float x,y,z;
    double x1=0,y1=0,z1=0, g=0;

    //88888888888888888888888888888888888888888888888888888888888888888
    @Override
    public void onSensorChanged(SensorEvent event) {
        x = event.values[0];
        y = event.values[1];
        z = event.values[2];

        g = Math.sqrt(x * x+y * y+z *z);// normalizing the value

        x1=x/g;
        y1=y/g;
        z1=z/g;

        int Xi = (int) Math.round(Math.toDegrees(Math.acos(x1)));
        int Yi = (int) Math.round(Math.toDegrees(Math.acos(y1)));
        int Zi = (int) Math.round(Math.toDegrees(Math.acos(z1)));


        if(Xi>20 && Zi<70)
        {//FI.setText("Robot is inclined Downwards");
            savedinclination = 1;

        }
        else if(Yi<75 && Xi>15) {
            //FI.setText("Robot is inclined Right");
            savedinclination = 3;

        }
        else if(Yi>110 && Xi>20) {
           // FI.setText("Robot is inclined Left");
            savedinclination = 4;

        }
        else if(Zi>110 && Xi>20) {
           // FI.setText("Robot is inclined Upwards");
            savedinclination = 2;

        }
        else {
           // FI.setText("Uninclined");
            savedinclination = 5;

        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    void sendinclination(int incl)
    {

        if(wifiactive==1) {
            broadcastIntent.setAction(NetworkClientService.ServiceBroadcastReceiver.SEND_INCLINATION);
            broadcastIntent.putExtra("inclination", incl);

            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
        }



    }


    //8888888888888888888888888888888888888888888888888888888888888888


    public static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            //Here the Database is created with the information we defined before.
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String sql = "CREATE TABLE " + TABLE_NAME + " ( " + COMMAND + " text not NULL, " + DIRECTION + " text not NULL, " + ");";
            Log.d("c", sql);
            db.execSQL(sql);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
        //If we don't add any thing into this part,
    }



    //__________________________________________________

    public final String ACTION_USB_PERMISSION = "com.hariharan.arduinousb.USB_PERMISSION";

    //EditText editText;
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;
    private IntentFilter filter;

    void obstacledetected()
    {
        if(wifiactive==1)
        {
            broadcastIntent.setAction(NetworkClientService.ServiceBroadcastReceiver.SEND_OBSTACLE);

            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
        }

    }

    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                //TextView textView = (TextView) findViewById(R.id.textView);
                //textView.setText("recieved data from usb");
                data = new String(arg0, "UTF-8");
                //data.concat("/n");
                //tvAppend(textView, data);
                //recived data from usb
                //if (data == "5") {
                    obstacledetected();
                   // textView.setText("it is obstacle");
               // }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }


        }
    };

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.

                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            //tvAppend(textView,"Serial Connection Opened!\n");

                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                            // tvAppend(textView,"port not Opened!\n");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                        //tvAppend(textView,"port isnull!\n");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                    // tvAppend(textView,"perm not granted!\n");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                onClickStart();
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                onClickStop();

            }
        }

        ;
    };


    public void onClickStart() {
        // tvAppend(textView,"begin!\n");
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            Log.d(TAG, "Opening USB Connection");

            //tvAppend(textView,"usb is not empty!\n");
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 0x2A03)//Arduino Vendor ID old => 9025(0x2341)
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                    //tvAppend(textView,"device id is null!\n");
                }

                if (!keep)
                    break;
            }
        } else {
            // tvAppend(textView,"usb is empty!\n");
        }


    }

    public void savequery(String str)
    {
        /*
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String ab = String.valueOf(total_commands);
        String sql2 = "insert into " + TABLE_NAME + " (" + COMMAND + ", " + DIRECTION + ") values( '"+ ab + "', '" + str +"');";
        try {
           // Log.d("b2", "add to database");
            //Log.d("a1", sql1);
            db.execSQL(sql2);

            // setTitle("Insert two Records Successfully.");
        } catch (SQLException e) {
            //setTitle("Insert the Record Failed.");
        }
        */


        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        String command1 = String.valueOf(total_commands);
        String direct1 = str;

        editor.putString(command1, direct1);
        //commits your edits
        editor.commit();
    }

    public void onClickSend(int command)
    {
        newcommand = true;




        if (command == 1) {
            String string = "1";
            serialPort.write(string.getBytes());
           savequery("1");

        }
        if (command == 2) {
            String string = "2";
            serialPort.write(string.getBytes());

           savequery("2");

        }
        if (command == 3) {
            String string = "3";
            serialPort.write(string.getBytes());

         savequery("3");
        }
        if (command == 4) {
            String string = "4";
            serialPort.write(string.getBytes());

          savequery("4");

        }




        newcommand =false;
        getlocation();
        sendlocation();

//        String latstr = String.valueOf(loc1.getAltitude());
  //      String lngstr = String.valueOf(loc1.getLongitude());

    //    broadcastIntent.putExtra("lat", latstr);
      //  broadcastIntent.putExtra("lng", lngstr);

        sendinclination(savedinclination);

       // broadcastIntent.putExtra("inclination", savedinclination);

//        if(wifiactive==1) {
  //          broadcastIntent.setAction(NetworkClientService.ServiceBroadcastReceiver.SEND_INCLINATION);
    //        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
      //  }



    }

    public void onClickStop() {

        serialPort.close();


    }

    public void checkWifiOnAndConnected()
    {

        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiMgr.isWifiEnabled()) { // Wi-Fi adapter is ON

            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();

            if( wifiInfo.getNetworkId() == -1 )
            {
                timerTaskwifi.cancel();
                timerTaskIMageCapture.cancel();
                wifiactive = 0;
                returnbase();
                //return false; // Not connected to an access point
            }
            //return true; // Connected to an access point

        }
        else
            {

                timerTaskwifi.cancel();
                timerTaskIMageCapture.cancel();
                wifiactive = 0;
                returnbase();
            //return false; // Wi-Fi adapter is OFF
        }
    }

    public void  returnbase()
    {
        while (total_commands>0)
        {
            /*
            SQLiteDatabase db = mOpenHelper.getReadableDatabase();
            String rawq = "SELECT * FROM " + TABLE_NAME + " WHERE " + COMMAND + " LIKE '" + String.valueOf(total_commands) + "'";
            Log.d("a2", rawq);

            Cursor c = db.rawQuery(rawq, null);

            if (c.getCount() != 0) {
                c.moveToFirst();
                String str = c.getString(c.getColumnIndex(DIRECTION));
                String reverse = getreverse(str);
                serialPort.write(reverse.getBytes());

            } else {
                c.getString(c.getColumnIndex(""));

            }

            c.close();
            total_commands--;
            */
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            String com = String.valueOf(total_commands);
            String comn = sharedPref.getString(com, "1");
            String reverse = getreverse(comn);
            serialPort.write(reverse.getBytes());
            total_commands--;
        }
    }

    public String getreverse(String str)
    {
        if(str.equals("1"))
        {
            return "4";
        }
        else if(str.equals("2"))
        {
            return "3";
        }
        else if(str.equals("3"))
        {
            return "2";
        }
        else
        {
            return "1";
        }
    }



    //_______________________________________________


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mOpenHelper = new DatabaseHelper(this);

        uploadBackPhoto = (ImageView) findViewById(R.id.backIV);

        // Create broadcast Intent to talk to Network Server Service
        broadcastIntent = new Intent();
        // Register Service Broadcast Receiver to receive broadcast from Network Service
        registerBroadcastReceiver();


        final Button takePictureButton = (Button) findViewById(R.id.take_picture_button);
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });
        //++++++++++++++++
        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);

        filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);

        onClickStart();
        //++++++++++++


        /*
        Timer timerObjImageCapture = new Timer();
        timerTaskIMageCapture = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() ->
                        new PictureService().startCapturing(MainActivity.this,MainActivity.this)
                );
            }
        };
        timerObjImageCapture.schedule(timerTaskIMageCapture, 0, captureImageRate);
*/

        //]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]

        manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {


                lat = location.getLatitude();
                lng = location.getLongitude();

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {
                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(i);

            }
        };

        configure_Location();


        //]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]

        AM = (SensorManager)getSystemService(SENSOR_SERVICE);
        A = AM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        AM.registerListener(this, A, SensorManager.SENSOR_DELAY_NORMAL);



    }

    void configure_Location() {
        // first check for permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET}
                        , 10);
            }
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    1
            );
            //return;
        }

    }


    void getlocation()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        manager.requestLocationUpdates("gps", 0, 0, listener);
        loc1.setLatitude(lat);
        loc1.setLongitude(lng);



    }

    void sendlocation()
    {

            broadcastIntent.setAction(NetworkClientService.ServiceBroadcastReceiver.SEND_LOCATION);
            String latstr = String.valueOf(loc1.getAltitude());
            String lngstr = String.valueOf(loc1.getLongitude());

            broadcastIntent.putExtra("lat", latstr);
            broadcastIntent.putExtra("lng", lngstr);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);

    }

    // Define Broadcast receiver class to receive received network packets from the Network Server Service
    public class ResponseReceiver extends BroadcastReceiver {

        // Define the Action to use with the Intent filter
        public static final String SEND_FORWARD_RX = "SEND_FORWARD_RX";
        public static final String SEND_BACKWARDS_RX = "SEND_BACKWARDS_RX";
        public static final String SEND_RIGHT_RX = "SEND_RIGHT_RX";
        public static final String SEND_LEFT_RX = "SEND_LEFT_RX";

        // Read data sent by the IntentService
        @Override
        public void onReceive(Context context, Intent intent) {

            // We need to check what type of the received broadcast message it is
            String action = intent.getAction();
            if (action.equals(SEND_FORWARD_RX)) {
                receivedMoveForwardMsg();
                // String msg = intent.getStringExtra(MainActivity.SEND_PACKET);
                //   double msg = intent.getDoubleExtra(MainActivity.SEND_PACKET,0);
            }
            if (action.equals(SEND_BACKWARDS_RX)) {
                receivedMoveBackwardMsg();
            }
            if (action.equals(SEND_RIGHT_RX)) {
                receivedMoveRightMsg();
            }
            if (action.equals(SEND_LEFT_RX)) {
                receivedMoveLeftMsg();
            }
        }
    }

    private void registerBroadcastReceiver() {
        // Register Local Service Broadcast Receiver
        filterWifi = new IntentFilter();
        // Define type of Broadcast to filter
        filterWifi.addAction(ResponseReceiver.SEND_FORWARD_RX);
        filterWifi.addAction(ResponseReceiver.SEND_BACKWARDS_RX);
        filterWifi.addAction(ResponseReceiver.SEND_RIGHT_RX);
        filterWifi.addAction(ResponseReceiver.SEND_LEFT_RX);
        filterWifi.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastReceiverWifi = new ResponseReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiverWifi, filterWifi);
    }


    /*
    Network API starts here
     */

    private void receivedMoveForwardMsg(){
        Log.d(TAG,"Received send forward message");
        // Send robot move forward command here
        total_commands++;
        onClickSend(1);
    }
    private void receivedMoveBackwardMsg(){
        Log.d(TAG,"Received send backward message");
        // Send robot move backward command here
        total_commands++;
        onClickSend(4);
    }
    private void receivedMoveRightMsg(){
        Log.d(TAG,"Received send right message");
        // Send robot move right command here
        total_commands++;
        onClickSend(3);
    }
    private void receivedMoveLeftMsg(){
        Log.d(TAG,"Received send left message");
        // Send robot move left command here
        total_commands++;
        onClickSend(2);
    }


    private void takePicture()
    {

        Timer timerObjImageCapture = new Timer();
        timerTaskIMageCapture = new TimerTask() {
            @Override
            public void run() {
                if(wifiactive==1)
                {
                    runOnUiThread(() ->
                            new PictureService().startCapturing(MainActivity.this, MainActivity.this)
                    );
                }
            }
        };
        timerObjImageCapture.schedule(timerTaskIMageCapture, 0, captureImageRate);


//-------------------------------------------------
        Timer timerwifi = new Timer();
        timerTaskwifi = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        checkWifiOnAndConnected();
                    }
                });
            }
        };
        timerwifi.schedule(timerTaskwifi, 0, 5000);

//--------------------------------------------------



        /*
        runOnUiThread(() ->
                new PictureService().startCapturing(MainActivity.this,MainActivity.this)
        );
        */
    }



    // Inflate Option Menu methods
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.settings_menu_id:
                Intent intent = new Intent(this, ClientNetworkSettingsActivity.class);
                startActivity(intent);
                return true;
        }
        return false;
    }



/*
Picture listener and service methods
 */

    private void showToast(final String text) {
        runOnUiThread(() ->
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void onDoneCapturingAllPhotos(TreeMap<String, byte[]> picturesTaken) {
        if (picturesTaken != null && !picturesTaken.isEmpty()) {
            showToast("Done capturing photo");
            return;
        }
        showToast("No camera detected!");
    }

    @Override
    public void onCaptureDone(String pictureUrl, byte[] pictureData) {

        if (pictureData != null && pictureUrl != null) {
            runOnUiThread(() -> {
                final Bitmap bitmap = BitmapFactory.decodeByteArray(pictureData, 0, pictureData.length);
                Log.d("IMAGEREPORTER", "Image size: " + pictureData.length + " Height: " + bitmap.getHeight() + " Width: " + bitmap.getWidth() + " Byte count: " + bitmap.getAllocationByteCount());
                final int nh = (int) (bitmap.getHeight() * (512.0 / bitmap.getWidth()));
                final Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 512, nh, true);
                Log.d("IMAGEREPORTER", "Image size: " + pictureData.length + " Height: " + scaled.getHeight() + " Width: " + scaled.getWidth() + " Byte count: " + scaled.getAllocationByteCount());
                if (pictureUrl.contains("0_pic.jpg")) {
                    uploadBackPhoto.setImageBitmap(scaled);
                    broadcastIntent.setAction(NetworkClientService.ServiceBroadcastReceiver.SEND_PICTURE_TX);
                    broadcastIntent.putExtra("ImageURL",pictureUrl);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
                }
            });
            showToast("Picture saved to " + pictureUrl);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_CODE: {
                if (!(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    checkPermissions();
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() {
        final String[] requiredPermissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
        };
        final List<String> neededPermissions = new ArrayList<>();
        for (final String p : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    p) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(p);
            }
        }
        if (!neededPermissions.isEmpty()) {
            requestPermissions(neededPermissions.toArray(new String[]{}),
                    MY_PERMISSIONS_REQUEST_ACCESS_CODE);
        }
    }




    // Use onPause method to unregister the Broadcast receiver to avoid "leaks"
    @Override
    protected void onPause() {
        /* we should unregister BroadcastReceiver here*/
        unregisterReceiver(broadcastReceiverWifi);
        unregisterReceiver(broadcastReceiver);
        onClickStop();
        super.onPause();
    }

    // Use onResume method to re-register the Broadcast receiver

    @Override
    protected void onResume() {
		/* we should register BroadcastReceiver here*/
        registerReceiver(broadcastReceiverWifi, filterWifi);
        registerReceiver(broadcastReceiver, filter);
        onClickStart();
        super.onResume();
    }


}