package net.uilennest.here;

import android.os.Bundle;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;


// https://www.tutorialspoint.com/android/android_location_based_services.htm
import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.test.mock.MockPackageManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

// e-mail
import android.net.Uri;
import android.content.Intent;
import android.util.Log;

// json
import org.json.JSONException;
import org.json.JSONObject;

// http
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.DataOutputStream;
import java.util.LinkedList;
import java.util.List;

import android.app.ActivityManager;
import android.accounts.AccountManager;
import android.accounts.Account;

public class MainActivity extends Activity {

    Button buttonGetLocation;
    Button buttonSendHomePost;
    Button buttonSendHomePut;
    EditText editName;
    EditText editTitle;
    EditText editDescription;
    TextView textCoordinates;

    private static final int REQUEST_CODE_PERMISSION = 2;
    String mPermission = Manifest.permission.ACCESS_FINE_LOCATION;

    // GPSTracker class
    public String HOMEBASE_URL = "http://uilennest.net/homebase/datacenter/locations/1/";
    public String HTTP_METHOD = "POST";

    // using globals because of threading
    public String username = "username";
    public String title = "sent by Here";
    public String longitude = "unknown";
    public String latitude = "unknown";
    public String description = "I am here!";
    GPSTracker gps;


    // read gmail username from the google acount
    // this will not work on the emulator, which has no google accounts.
    // Hence the default username of 'emulator'
    public String getUsername(String defaultName) {
        List<String> possibleEmails = null;
        try {
            AccountManager manager = AccountManager.get(this);
            //Account[] accounts = manager.getAccountsByType("com.google");
            Account[] accounts = manager.getAccounts();

            for (Account account : accounts) {

                // return the first gmail account username
                if (account.name.contains("gmail.com")) {
                    String[] parts = account.name.split("@");
                    username = parts[0];
                    return username;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
         }

        return defaultName;
    }

    void doGetLocation() {
        gps = new GPSTracker(MainActivity.this);

        // check if GPS enabled
        if (gps.canGetLocation()) {
            // longitude = String.valueOf(gps.getLongitude());
            longitude = String.format("%.5f", gps.getLongitude());
            // latitude = String.valueOf(gps.getLatitude());
            latitude = String.format("%.5f", gps.getLatitude());
            textCoordinates = (TextView) findViewById(R.id.textCoordinates);
            textCoordinates.setText("latitude = "+latitude+", longitude = "+longitude);
        } else {
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gps.showSettingsAlert();
        }
    }

    void doShowLocation() {
        Toast.makeText(getApplicationContext(), "Location is - \nLon: "+ longitude+ "\nLat: " + latitude, Toast.LENGTH_LONG).show();
    }

    void doSendCoordinates(String method) {
        HTTP_METHOD = method;
        Toast.makeText(getApplicationContext(), HTTP_METHOD+ " to HomeBase: \n"+latitude+", "+longitude, Toast.LENGTH_LONG).show();
        sendCoordinates();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // attempt to update the username with the google account username.
        username = getUsername(username);

        try {
            if (ActivityCompat.checkSelfPermission(this, mPermission)
                    != MockPackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{mPermission},
                        REQUEST_CODE_PERMISSION);

                // If any permission above not allowed by user, this condition will
                //execute every time, else your else part will work
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        editName = (EditText) findViewById(R.id.editName);
        editName.setText(username);

        editDescription = (EditText) findViewById(R.id.editDescription);
        editDescription.setText(description);

        buttonGetLocation = (Button) findViewById(R.id.buttonGetLocation);
        buttonGetLocation.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // create class object
                doGetLocation();
                doShowLocation();
            }
        });

        buttonSendHomePost = (Button) findViewById(R.id.buttonSendHomePost);
        buttonSendHomePost.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                doGetLocation();
                HOMEBASE_URL = "http://uilennest.net/homebase/datacenter/locations/";
                doSendCoordinates("POST"); //update existing
            }
        });

        if (username.equalsIgnoreCase("nicovermaas")) {
            buttonSendHomePut = (Button) findViewById(R.id.buttonSendHomePut);
            buttonSendHomePut.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    doGetLocation();
                    HOMEBASE_URL = "http://uilennest.net/homebase/datacenter/locations/1/";
                    doSendCoordinates("PUT"); // create nieuw
                }
            });
        }
    }


    public void sendCoordinates() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(HOMEBASE_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod(HTTP_METHOD);
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    conn.setRequestProperty("Accept","application/json");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    //method 1
                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());



                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("timestamp", sdf.format(timestamp));
                    jsonParam.put("title", title);
                    jsonParam.put("username", editName.getText().toString());
                    jsonParam.put("description", editDescription.getText().toString());
                    jsonParam.put("latitude", latitude);
                    jsonParam.put("longitude", longitude);
                    // http://maps.google.com/maps?q=24.197611,120.780512
                    String google_map = "http://maps.google.com/maps?q="+latitude+","+longitude;
                    jsonParam.put("url", google_map);

                    Log.i("JSON", jsonParam.toString());
                    DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                    //os.writeBytes(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
                    os.writeBytes(jsonParam.toString());

                    os.flush();
                    os.close();

                    Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                    Log.i("MSG" , conn.getResponseMessage());

                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }
}