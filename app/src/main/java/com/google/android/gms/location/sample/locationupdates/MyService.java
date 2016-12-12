package com.google.android.gms.location.sample.locationupdates;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.Log;

import java.io.IOException;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by wstewart on 10/30/2016.
 */
public class MyService extends Service {

    private static final String TAG = "BOOMBOOMTESTGPS";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 10f;
    protected String mUid;

    private class LocationListener implements android.location.LocationListener{
        Location mLastLocation;

        public LocationListener(String provider)
        {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }
        @Override
        public void onLocationChanged(Location location)
        {
            Log.e(TAG, "onLocationChanged: " + location);
            mLastLocation.set(location);
            notifyDatabase(mLastLocation);
        }
        @Override
        public void onProviderDisabled(String provider)
        {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }
        @Override
        public void onProviderEnabled(String provider)
        {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }
    LocationListener[] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };
    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }
    @Override
    public void onCreate()
    {
        Log.e(TAG, "onCreate");
        initializeLocationManager();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }
        Log.d("zombie app","called the service class");
        mUid = UidProvider.getUniqueId(this);
    }
    @Override
    public void onDestroy()
    {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    private void notifyDatabase(Location mLastLocation) {
        try {
            // Open connection to database wrapper.
            URL url = new URL("http://cs.furman.edu/~wstewart/webservice.php");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");

            String attributesToSend = "userid=" + URLEncoder.encode(mUid, "UTF-8")
                    + "&latitude=" + URLEncoder.encode(Double.toString(mLastLocation.getLatitude()), "UTF-8")
                    + "&longitude=" + URLEncoder.encode(Double.toString(mLastLocation.getLongitude()), "UTF-8")
                    + "&time=" + URLEncoder.encode(Long.toString(mLastLocation.getTime() / 1000L), "UTF-8")
                    + "&accuracy=" + URLEncoder.encode(Double.toString(mLastLocation.getAccuracy()), "UTF-8")
                    + "&altitude=" + URLEncoder.encode(Double.toString(mLastLocation.getAltitude()), "UTF-8");
            Log.e("data to send",attributesToSend);
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
        catch(NullPointerException np){
            Log.e("notifyDatabase","null pointer exception");
        }
    }
}
