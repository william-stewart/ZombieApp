/**
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.location.sample.locationupdates;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Getting Location Updates.
 *
 * Demonstrates how to use the Fused Location Provider API to get updates about a device's
 * location. The Fused Location Provider is part of the Google Play services location APIs.
 *
 * For a simpler example that shows the use of Google Play services to fetch the last known location
 * of a device, see
 * https://github.com/googlesamples/android-play-location/tree/master/BasicLocation.
 *
 * This sample uses Google Play services, but it does not require authentication. For a sample that
 * uses Google Play services for authentication, see
 * https://github.com/googlesamples/android-google-accounts/tree/master/QuickStart.
 */
public class MainActivity extends AppCompatActivity implements
        ConnectionCallbacks, OnConnectionFailedListener, LocationListener, OnMapReadyCallback {

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    public static final float SMALLEST_DISPLACEMENT_IN_METERS = 0.0f;
    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    protected static final String TAG = "location-updates-sample";
    // Keys for storing activity state in the Bundle.
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    protected LocationRequest mLocationRequest;

    /**
     * Represents a geographical location.
     */
    protected Location mCurrentLocation;
    protected LocationHistoryManager mLocationHistoryManager;
    protected GoogleMap mMap;

    /**
     * Represents an unique device identification.
     */
    protected String mUid;

    // UI Widgets.
    protected Button mStartUpdatesButton;
    protected Button mStopUpdatesButton;
    protected MapFragment mMapFragment;
    protected TextView mUidTextView;
    protected TextView mLastUpdateTimeTextView;
    protected TextView mLatitudeTextView;
    protected TextView mLongitudeTextView;
    protected TextView mAltitudeTextView;
    protected TextView mAccuracyTextView;

    // Labels.
    protected String mUidLabel;
    protected String mLatitudeLabel;
    protected String mLongitudeLabel;
    protected String mAltitudeLabel;
    protected String mAccuracyLabel;
    protected String mLastUpdateTimeLabel;

    //Whether or not user is infected
    protected boolean isInfected = false;
    protected TextView infectionStatus;

    protected boolean isFirstBoot = true;

    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    protected Boolean mRequestingLocationUpdates;

    /**
     * Time when the location was updated represented as a String.
     */
    protected String mLastUpdateTime;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);



        // Locate the UI widgets.
        //mStartUpdatesButton = (Button) findViewById(R.id.start_updates_button);
        //mStopUpdatesButton = (Button) findViewById(R.id.stop_updates_button);
        mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment);
        mUidTextView = (TextView) findViewById(R.id.uid_text);
        mLatitudeTextView = (TextView) findViewById(R.id.latitude_text);
        mLongitudeTextView = (TextView) findViewById(R.id.longitude_text);
        mAltitudeTextView = (TextView) findViewById(R.id.altitude_text);
        mAccuracyTextView = (TextView) findViewById(R.id.accuracy_text);
        mLastUpdateTimeTextView = (TextView) findViewById(R.id.last_update_time_text);

        infectionStatus = (TextView) findViewById(R.id.textView2);
        infectionStatus.append("not infected");

        // Set labels.
        mUidLabel = "UID";
        mLatitudeLabel = "Latitude";
        mLongitudeLabel = "Longitude";
        mAltitudeLabel = "Altitude";
        mAccuracyLabel = "Accuracy";
        mLastUpdateTimeLabel = "Last location update time";

        // Prepares the map fragment for display and manipulation.
        mMapFragment.getMapAsync(this);

        // Request location updates by default.
        mRequestingLocationUpdates = true;
        setButtonsEnabledState();

        // Save a set number of location records as history for display.
        mLocationHistoryManager = new LocationHistoryManager(5);

        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);

        // Set the Unique ID
        mUid = UidProvider.getUniqueId(this);
        mUidTextView.setText(String.format(Locale.US, "%s: %s", mUidLabel, mUid));

        checkIfInfected();

        // Kick off the process of building a GoogleApiClient and requesting the LocationServices
        // API.
        buildGoogleApiClient();



        startService(new Intent(getBaseContext(), MyService.class));

    }

    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Log.i(TAG, "Updating values from bundle");
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
                setButtonsEnabledState();
            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }
            updateUI();
        }
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setSmallestDisplacement(SMALLEST_DISPLACEMENT_IN_METERS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Handles the Start Updates button and requests start of location updates. Does nothing if
     * updates have already been requested.
     */
    public void startUpdatesButtonHandler(View view) {
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;
            setButtonsEnabledState();
            startLocationUpdates();
        }
    }

    /**
     * Handles the Stop Updates button, and requests removal of location updates. Does nothing if
     * updates were not previously requested.
     */
    public void stopUpdatesButtonHandler(View view) {
        if (mRequestingLocationUpdates) {
            mRequestingLocationUpdates = false;
            setButtonsEnabledState();
            stopLocationUpdates();
        }
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * Ensures that only one button is enabled at any time. The Start Updates button is enabled
     * if the user is not requesting location updates. The Stop Updates button is enabled if the
     * user is requesting location updates.
     */
    private void setButtonsEnabledState() {
        if (mRequestingLocationUpdates) {
           // mStartUpdatesButton.setEnabled(false);
           // mStopUpdatesButton.setEnabled(true);
        } else {
           // mStartUpdatesButton.setEnabled(true);
           // mStopUpdatesButton.setEnabled(false);
        }
    }

    /**
     * Updates the latitude, the longitude, and the last location time in the UI.
     */
    private void updateUI() {
        mLatitudeTextView.setText(String.format(Locale.US, "%s: %f", mLatitudeLabel,
                mCurrentLocation.getLatitude()));
        mLongitudeTextView.setText(String.format(Locale.US, "%s: %f", mLongitudeLabel,
                mCurrentLocation.getLongitude()));
        mAltitudeTextView.setText(String.format(Locale.US, "%s: %f", mAltitudeLabel,
                mCurrentLocation.getAltitude()));
        mAccuracyTextView.setText(String.format(Locale.US, "%s: %f", mAccuracyLabel,
                mCurrentLocation.getAccuracy()));
        mLastUpdateTimeTextView.setText(String.format(Locale.US, "%s: %s", mLastUpdateTimeLabel,
                mLastUpdateTime));


        // If map is available, plot the location history.
        if(mMap != null) {
            // Remove all previous markers from map.
            //mMap.clear();

            int index = 0;
            float markerAlpha;
            for (Location l : mLocationHistoryManager.getAll()) {
                // Normalize alpha so that the first marker isn't entirely transparent
                markerAlpha = (index + 1.0f) / (mLocationHistoryManager.getSize() + 1.0f);

                LatLng mLatLng = new LatLng(l.getLatitude(), l.getLongitude());
                mMap.addMarker(new MarkerOptions().position(mLatLng).alpha(markerAlpha));

                index++;
                // Focus and zoom the camera on the most recent marker

                if (index == mLocationHistoryManager.getSize() && isFirstBoot) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mLatLng, 20));
                    displayBumpsOnMap();
                }

            }
        }
        infectionStatus.append(Float.toString(mCurrentLocation.getAccuracy()));
    }

    private void displayBumpsOnMap(){
        //Pulls data from a webservice and creates markers for each bump
        Log.e("display bumps on map","method called");
        try {
            if (mMap != null) {
                URL url = new URL("http://cs.furman.edu/~wstewart/displayinfectedtable.php");

                // Read all the text returned by the server
                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                String str;
                String dataLine = "";
                while ((str = in.readLine()) != null) {
                    // str is one line of text; readLine() strips the newline character(s)
                    if (str.contains("<table>")) {
                        dataLine = str;
                    }
                }
                dataLine = dataLine.replaceAll("<td>", "");
                dataLine = dataLine.replaceAll("</tr>", "");
                dataLine = dataLine.replaceAll("<table>", "");
                dataLine = dataLine.replaceAll("</table>", "");
                dataLine = dataLine.replaceAll("</td>", "/");
                //Log.e("raw",dataLine);
                String[] lineSplit = dataLine.split("<tr>");
                ArrayList<String> linesMinusTags = new ArrayList<String>();
                //TODO: parse entirety of xml CHECK IF UID IS FIRST ELEMENT
                //Split the individual lines and

                for (String s : lineSplit) {
                    String[] split = s.split("/");
                    if(split.length >= 2) {
                        double lat = Double.parseDouble(split[2]);
                        double lng = Double.parseDouble(split[1]);
                        //Log.e(Double.toString(lat), Double.toString(lng));
                        LatLng latLng = new LatLng(lat, lng);
                        mMap.addMarker(new MarkerOptions().position(latLng)); 
                    }
                }
                in.close();
            }else{Log.e("displaybumps","maps is null");}
        }
        catch(Exception e){
            Log.e("displayBumpsOnMap()","something went wrong");
            e.printStackTrace();
        }
    }

    private void notifyDatabase() {
        try {
            // Open connection to database wrapper.
            URL url = new URL("http://cs.furman.edu/~wstewart/webservice.php");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");

            String attributesToSend = "userid=" + URLEncoder.encode(mUid, "UTF-8")
                    + "&latitude=" + URLEncoder.encode(Double.toString(mCurrentLocation.getLatitude()), "UTF-8")
                    + "&longitude=" + URLEncoder.encode(Double.toString(mCurrentLocation.getLongitude()), "UTF-8")
                    + "&time=" + URLEncoder.encode(mLastUpdateTime, "UTF-8")
                    + "&accuracy=" + URLEncoder.encode(Double.toString(mCurrentLocation.getAccuracy()), "UTF-8")
                    + "&altitude=" + URLEncoder.encode(Double.toString(mCurrentLocation.getAltitude()), "UTF-8");

            // Send location data.
            conn.setFixedLengthStreamingMode(attributesToSend.getBytes().length);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            java.io.BufferedOutputStream out = new java.io.BufferedOutputStream(conn.getOutputStream());
            PrintStream pStream = new PrintStream(out);
            pStream.print(attributesToSend);
            pStream.close();


        }
        catch(java.net.MalformedURLException ex){
            // Error ignored, intentionally left blank.
        }
        catch(IOException e) {
            // Error ignored, intentionally left blank.
        }
    }

    /**
     * Adds location record to history.
     */
    private void logLocation() {
        mLocationHistoryManager.add(mCurrentLocation);
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Within {@code onPause()}, we pause location updates, but leave the
        // connection to GoogleApiClient intact.  Here, we resume receiving
        // location updates if the user has requested them.

        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");

        // If the initial location was never previously requested, we use
        // FusedLocationApi.getLastLocation() to get it. If it was previously requested, we store
        // its value in the Bundle and check for it in onCreate(). We
        // do not request it again unless the user specifically requests location updates by pressing
        // the Start Updates button.
        //
        // Because we cache the value of the initial location in the Bundle, it means that if the
        // user launches the activity,
        // moves to a new location, and then changes the device orientation, the original location
        // is displayed as the activity is re-created.
        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            if(mCurrentLocation != null) {
                mLastUpdateTime = Long.toString(mCurrentLocation.getTime() / 1000L);
                updateUI();
            }
        }

        // If the user presses the Start Updates button before GoogleApiClient connects, we set
        // mRequestingLocationUpdates to true (see startUpdatesButtonHandler()). Here, we check
        // the value of mRequestingLocationUpdates and if it is true, we start location updates.
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    public void checkIfInfected(){
        Log.e("Main activity","called checkIfInfected() method");

        try {
            // Create a URL for the desired page
            URL url = new URL("http://cs.furman.edu/~wstewart/displayinfectedtable.php");

            // Read all the text returned by the server
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String str;
            String dataLine= "";
            while ((str = in.readLine()) != null) {
                // str is one line of text; readLine() strips the newline character(s)
                if(str.contains("<table>")){
                    dataLine=str;
                }
            }
            dataLine = dataLine.replaceAll("<td>","");
            dataLine = dataLine.replaceAll("</tr>","");
            dataLine = dataLine.replaceAll("<table>","");
            dataLine = dataLine.replaceAll("</table>","");
            dataLine = dataLine.replaceAll("</td>","/");
            //Log.e("raw",dataLine);
            String[] lineSplit = dataLine.split("<tr>");
            ArrayList<String> linesMinusTags = new ArrayList<String>();
            //TODO: parse entirety of xml CHECK IF UID IS FIRST ELEMENT
            //Split the individual lines and check first element in array

            for(String s: lineSplit){
                String[] split = s.split("/");
                if(split[0].equals(mUid)){
                    infectionStatus.setText("Infection status: You are infected!");
                    Log.e("infected",split[0] + "%%%%" + mUid);
                }else{
                    Log.d("not infected",split[0] + "%%%%" + mUid);
                }
               // Log.e("from web",s);
            }

            in.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Callback that fires when the location changes.
     */
    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = Long.toString(mCurrentLocation.getTime() / 1000L);
        logLocation();
        updateUI();
        isFirstBoot = false;
        //notifyDatabase();
        Toast.makeText(this, getResources().getString(R.string.location_updated_message),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


    /**
     * Stores activity data in the Bundle.
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }
}
