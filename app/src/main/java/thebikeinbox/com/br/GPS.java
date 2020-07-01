package thebikeinbox.com.br;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class GPS extends Service {
    String GPX;
    Double lat_ini, lon_ini, alt_ini;
    Long time_ini;
    private LocationRequest mLocationRequest;
    private long UPDATE_INTERVAL = 1 * 1000;  /* 1 secs */
    private long FASTEST_INTERVAL = 1000; /* 1 sec */
    Thread thread;

    public GPS() {
    }

/* *** BINDER DE DADOS ************************************ */
    class MyServiceBinder extends Binder{
        public GPS getService(){
            return GPS.this;
        }
    }

    private IBinder mBinder=new MyServiceBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent){
        System.out.println("IBinder");
        return mBinder;
    }

/* *** FIM BINDER DE DADOS ******************************** */

    public void onCreate() {
        System.out.println("onCreate Service");
        startLocationUpdates();

    }



    // Trigger new location updates at interval
    protected void startLocationUpdates() {

        // Create the location request to start receiving updates
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        // Check whether location settings are satisfied
        // https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        // do work here
                        onLocationChanged(locationResult.getLastLocation());
                    }
                },
                Looper.myLooper());
    }
    public void onLocationChanged(Location location)  {
        lat_ini = location.getLatitude();
        lon_ini = location.getLongitude();
        alt_ini = location.getAltitude();
        time_ini = location.getTime();

//        Date date = new Calendar().getTime(time_ini.intValue());
        Date d = new Date(time_ini);
        System.out.println(lat_ini.toString() +"|"+lon_ini.toString() +"|"+alt_ini.toString()+"|"+d.toString());

    }

    public void getLastLocation() {
        // Get last known recent location using new Google Play Services SDK (v11+)
        FusedLocationProviderClient locationClient = getFusedLocationProviderClient(this);

        locationClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // GPS location can be null if GPS is switched off
                        if (location != null) {
                            onLocationChanged(location);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("MapDemoActivity", "Error trying to get last GPS location");
                        e.printStackTrace();
                    }
                });
    }



/*
    public void AddPoint(){
        if(true) {
            if(GPX.equals("")) { //CABEÇALHO
                GPX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<gpx creator=\"theBikeinBox_App\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://www.garmin.com/xmlschemas/GpxExtensions/v3 http://www.garmin.com/xmlschemas/GpxExtensionsv3.xsd http://www.garmin.com/xmlschemas/TrackPointExtension/v1 http://www.garmin.com/xmlschemas/TrackPointExtensionv1.xsd\" version=\"1.1\" xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\" xmlns:gpxx=\"http://www.garmin.com/xmlschemas/GpxExtensions/v3\">\n" +
                        " <metadata>\n" +
                        "  <time>" + Ano + "-" + String.format("%02d",Mes) + "-" + String.format("%02d",Dia) + "T" + hora_atual + "Z</time>\n" +
                        " </metadata>\n" +
                        " <trk>\n" +
                        "  <name>" + String.format("%02d",Dia) + "/" + String.format("%02d",Mes) + "/" + Ano + " - Track </name>\n" +
                        "  <type>1</type>\n" +
                        "  <trkseg>\n";
            }

            GPX += "   <trkpt lat=\"" + lastLat + "\" lon=\"" + lastLon + "\">\n" +
                    "    <ele>" + lastAlt + "</ele>\n" +
                    "    <time>" + Ano + "-" + String.format("%02d",Mes) + "-" + String.format("%02d",Dia) + "T" + hora_atual + "Z</time>\n" +
                    "    <extensions>\n" +
                    "     <gpxtpx:TrackPointExtension>\n" +
                    "      <gpxtpx:atemp>20</gpxtpx:atemp>\n" +
                    "     </gpxtpx:TrackPointExtension>\n" +
                    "    </extensions>\n" +
                    "   </trkpt>\n";
        }
    }
*/

}
