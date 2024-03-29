package com.example.jisu.testmapping;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.BroadcastReceiver;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements View.OnClickListener/*, OnMapReadyCallback */{
    boolean win = false;
    private LinearLayout ll;
    private static final String TAG = "MainActivity";
    // Log.d(TAG,"");
    LocationManager locManager;
    AlertReceiver receiver;
    TextView locationText;
    PendingIntent proximityIntent;
    boolean isPermitted = false;
    boolean isLocRequested = false;
    boolean isAlertRegistered = false;
    final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    Button alert, alert_release;
    double lat = 0.0;
    double lng = 0.0;
    GoogleMap gMap;
    GroundOverlayOptions cctvMark;
    ArrayList<MarkerOptions> cctvList = new ArrayList<MarkerOptions>();
    boolean chcked = false;
    private float min = 200.0f;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationText = findViewById(R.id.location);
        ll = findViewById(R.id.ll);

        alert = findViewById(R.id.alert);
        alert_release = findViewById(R.id.alert_release);
        requestRuntimePermission();
        try {
            if (isPermitted) {
                locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, gpsLocationListener);
                isLocRequested = true;
            } else {
                Toast.makeText(this, "Permission이 없습니다..", Toast.LENGTH_LONG).show();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
       // ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);
        alert.setOnClickListener(this);
        alert_release.setOnClickListener(this);
    }

    private void requestRuntimePermission() {
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        } else {
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, gpsLocationListener);
            locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, gpsLocationListener);
            isPermitted = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    isPermitted = true;
                } else {
                    isPermitted = false;
                }
                return;
            }
        }
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.alert:
                receiver = new AlertReceiver();
                IntentFilter filter = new IntentFilter("com.example.jisu.testmapping");
                registerReceiver(receiver, filter);
                Intent intent = new Intent("com.example.jisu.testmapping");
                proximityIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
                try {
                    win = true;
                    locManager.addProximityAlert(37.562019, 127.035559, min, -1, proximityIntent);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
                isAlertRegistered = true;
                locationText.setText("위도 : " + lat + "\n" + "경도 : " + lng);
                break;
            case R.id.alert_release:
                min=200.0f;
                ll.setBackgroundColor(Color.BLACK);
                try {
                    if (isAlertRegistered) {
                        locManager.removeProximityAlert(proximityIntent);
                        unregisterReceiver(receiver);
                    }
                    win = false;
                    Toast.makeText(getApplicationContext(), "근접 경보 해제 되었습니다.", Toast.LENGTH_SHORT).show();
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (isLocRequested) {
                locManager.removeUpdates(gpsLocationListener);
                isLocRequested = false;
            }
            if (isAlertRegistered) {
                locManager.removeProximityAlert(proximityIntent);
                unregisterReceiver(receiver);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }


    final LocationListener gpsLocationListener = new LocationListener() {
        //단점은 움직여서 값이 변동이 되야 한다 그래야 작동한다.
        public void onLocationChanged(Location location) {
            lat = location.getLongitude();
            lng = location.getLatitude();
            locationText.setText("위도 : " + lat + "\n" + "경도 : " + lng);
            if (win) {
                try {
                    locManager.addProximityAlert(37.562019, 127.035559, min, -1, proximityIntent);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
//            chcked = true;
//            ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(MainActivity.this);
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };


    public class AlertReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isEntering = intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false);
            Log.d(TAG, "AlertReceiver" + isEntering);
            if (isEntering) {
                int mm = (int) min;
                switch (mm) {
                    case 20:
                        Toast.makeText(context, "목표에 도착했습니다.", Toast.LENGTH_LONG).show();
                        ll.setBackgroundColor(Color.YELLOW);
                        Log.d(TAG,"4번");
                        break;
                    case 50:
                        Toast.makeText(context, "목표반경 50미터 안에 들어왔습니다.", Toast.LENGTH_LONG).show();
                        ll.setBackgroundColor(Color.BLUE);
                        min = 20.0f;
                        Log.d(TAG,"3번");
                        break;
                    case 100:
                        Toast.makeText(context, "목표반경 100미터 안에 들어왔습니다.", Toast.LENGTH_LONG).show();
                        min = 50.0f;
                        Log.d(TAG,"2번");
                        ll.setBackgroundColor(Color.RED);
                        break;
                    case 200:
                        Toast.makeText(context, "목표반경 200미터 안에 들어왔습니다.", Toast.LENGTH_LONG).show();
                        ll.setBackgroundColor(Color.GREEN);
                        Log.d(TAG,"1번");
                        min = 100.0f;
                        break;
                }
            } else {
                int mm = (int) min;
                switch (mm) {
                    case 20:
                        Toast.makeText(context, "목표반경 20미터 멀어졌습니다.", Toast.LENGTH_LONG).show();
                        min = 50.0f;
                        break;
                    case 50:
                        Toast.makeText(context, "목표반경 50미터 멀어졌습니다.", Toast.LENGTH_LONG).show();
                        min = 100.0f;
                        break;
                    case 100:
                        Toast.makeText(context, "목표반경 100미터 멀어졌습니다.", Toast.LENGTH_LONG).show();
                        min = 200.0f;
                        break;
                    case 200:
                        Toast.makeText(context, "목표에서 200미터 멀어졌습니다", Toast.LENGTH_LONG).show();
                        break;
                }
            }
        }
    }
    //맵
    /*@Override
    public void onMapReady(final GoogleMap googleMap) {
        gMap = googleMap;
        gMap.clear();
        if (chcked) {
            LatLng latLng = new LatLng(lng, lat);
            if (cctvList.size() <= 0) {
                MarkerOptions marker = new MarkerOptions();
                marker.position(new LatLng(37.561698, 127.034293));
                cctvList.add(marker);
            }
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.title("내 위치");
            markerOptions.position(latLng);
            cctvList.add(markerOptions);
            for (int i = 0; i < cctvList.size(); i++) {
                gMap.addMarker(cctvList.get(i));
            }
            if (cctvList.size() >= 2) {
                cctvList.remove(1);
            }
        }
    }*/
}