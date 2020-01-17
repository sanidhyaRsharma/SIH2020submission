package com.example.potholegame;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.potholegame.ui.VolleyMultipartRequest;
import com.example.potholegame.ui.home.HomeFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.content.Context.*;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener, SensorEventListener2 {

    private AppBarConfiguration mAppBarConfiguration;
    public MapboxMap mapboxMap;
    private MapView mapView;
    private PermissionsManager permissionsManager;
    private String univ;

    SensorManager manager;
    Button buttonStart;
    Button buttonStop;
    boolean isRunning;
    final String TAG = "SensorLog";
    FileWriter writer;

    private final static int ALL_PERMISSIONS_RESULT = 101;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1;
    private List<Location> locationList;
    private long lastone =0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Mapbox.getInstance(this, "pk.eyJ1Ijoic2FuaWRoeWFzaGFybWEiLCJhIjoiY2s1aHVyZTB2MDRxajNrbzc1Y3djcWJzdyJ9.aSXAVVaH5NJXErQUMH0Hmw");
        locationList = new ArrayList<>();
        univ="";

        setContentView(R.layout.activity_home);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                Intent i = new Intent(HomeActivity.this, MainActivity.class);
                startActivity(i);
            }
        });
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow,
                R.id.nav_tools, R.id.nav_share, R.id.nav_send)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        isRunning = false;
        Switch simpleSwitch =  findViewById(R.id.switch1);
        manager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Boolean switchState = simpleSwitch.isChecked();

        simpleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked){ //ON
                Log.d(TAG, "Writing to " + getStorageDir());
                try {
                    lastone = System.currentTimeMillis();
                    writer = new FileWriter(new File(getStorageDir(), "sensors_" + lastone + ".csv"));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                manager.registerListener(HomeActivity.this, manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 500000);
                manager.registerListener(HomeActivity.this, manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), 500000);
                isRunning = true;
            }
            else{ //OFF
                isRunning = false;
                manager.flush(HomeActivity.this);
                manager.unregisterListener(HomeActivity.this);

                try {
                    writer.close();
                    try {
                        RequestQueue requestQueue = Volley.newRequestQueue(this);
                        String URL = "http://192.168.43.26:5000/api/sendCarData";
                        JSONObject jsonBody = new JSONObject();
                        jsonBody.put("Data", univ);
//                        jsonBody.put("Author", "BNK");
                        final String requestBody = jsonBody.toString();

                        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                Log.i("VOLLEY", response);
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.e("VOLLEY", error.toString());
                            }
                        }) {
                            @Override
                            public String getBodyContentType() {
                                return "application/json; charset=utf-8";
                            }

                            @Override
                            public byte[] getBody() throws AuthFailureError {
                                try {
                                    univ="";
                                    return requestBody == null ? null : requestBody.getBytes("utf-8");
                                } catch (UnsupportedEncodingException uee) {
                                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                                    return null;
                                }
                            }

                            @Override
                            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                                String responseString = "";
                                if (response != null) {
                                    responseString = String.valueOf(response.statusCode);
                                    // can get more details such as response.headers
                                }
                                return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
                            }
                        };

                        requestQueue.add(stringRequest);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        RequestQueue queue = Volley.newRequestQueue(HomeActivity.this);
        String url ="http://192.168.43.26:5000/api/getLocations";
        // Request a string response from the provided URL.
        Log.d("LOCATIONREQUEST","BEFORE");
        StringRequest request = new StringRequest(Request.Method.GET, url, response -> {
            try{
                Log.d("LOCATIONREQUEST","BEFORE LOOP");
                JSONArray array = new JSONArray(response);
                for(int i = 0 ; i< array.length(); i++){
                    JSONArray arr = array.getJSONArray(i);
                    Location newLoc = new Location("");
                    newLoc.setLatitude(Double.parseDouble(arr.getDouble(0)+""));
                    newLoc.setLongitude(Double.parseDouble(arr.getDouble(1)+""));
                    locationList.add(newLoc);
                    Log.d("LOCATIONREQUEST",newLoc.toString());

                }

            }
            catch (JSONException e){

                e.printStackTrace();
                Log.d("LOCATIONREQUEST", "Error"+e);
            }
        }, error -> {
            Log.d("LOCATIONREQUEST", "Error"+error);

        });
        queue.add(request);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            mapboxMap.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    enableLocationComponent(style);
                }
            });
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        HomeActivity.this.mapboxMap = mapboxMap;
        mapboxMap.setStyle( Style.MAPBOX_STREETS, //Builder().fromUri("mapbox://styles/mapbox/cjerxnqt3cgvp2rmyuxbeqme7")
                new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        enableLocationComponent(style);
                        if(locationList.size() > 0) {
                            for (Location l : locationList) {
                                mapboxMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(l.getLatitude(), l.getLongitude()))
                                        .title("Gift!"));
                            }
                        }
                    }
                });
    }

    private void enableLocationComponent(@NonNull Style loadedMapStyle){
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Get an instance of the component
            LocationComponent locationComponent = this.mapboxMap.getLocationComponent();

            // Activate with options
            locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(this, loadedMapStyle)
                            .build());

            // Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);
;
            // Set the component's render mode
            locationComponent.setRenderMode(RenderMode.COMPASS);

        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }
    public void setMapView(MapView mapView){
        this.mapView = mapView;
    }
    @Override
    @SuppressWarnings( {"MissingPermission"})
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    private String getStorageDir() {
        return this.getExternalFilesDir(null).getAbsolutePath();
        //  return "/storage/emulated/0/Android/data/com.iam360.sensorlog/";
    }

    @Override
    public void onFlushCompleted(Sensor sensor) {

    }

    @Override
    public void onSensorChanged(SensorEvent evt) {
        /*
        Location mloc = new Location("dummyprovider");
        mloc.setLatitude(0);
        mloc.setLongitude(0);
        mloc = getSingleLocation();
  //      getLocation();
  */
        if(isRunning) {
            try {
                //writer.write(String.format("%d; LOC; %f; %f\n", evt.timestamp,mloc.getLatitude(),mloc.getLongitude()));
                switch(evt.sensor.getType()) {
                    case Sensor.TYPE_ACCELEROMETER:
                        writer.write(String.format("%d,ACC,%f,%f,%f\n", evt.timestamp, evt.values[0], evt.values[1], evt.values[2]));
                        univ += String.format("%d,ACC,%f,%f,%f\n", evt.timestamp, evt.values[0], evt.values[1], evt.values[2]);
                        break;
                    case Sensor.TYPE_GYROSCOPE:
                        writer.write(String.format("%d,GYRO,%f,%f,%f\n", evt.timestamp, evt.values[0], evt.values[1], evt.values[2]));
                        univ += String.format("%d,GYRO,%f,%f,%f\n", evt.timestamp, evt.values[0], evt.values[1], evt.values[2]);
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
