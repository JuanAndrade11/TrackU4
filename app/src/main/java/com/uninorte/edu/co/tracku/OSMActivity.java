package com.uninorte.edu.co.tracku;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.uninorte.edu.co.tracku.com.uninorte.edu.co.tracku.gps.GPSManager;
import com.uninorte.edu.co.tracku.com.uninorte.edu.co.tracku.gps.GPSManagerInterface;
import com.uninorte.edu.co.tracku.database.core.TrackUDatabaseManager;
import com.uninorte.edu.co.tracku.database.entities.Historical;
import com.uninorte.edu.co.tracku.database.entities.User;
import com.uninorte.edu.co.tracku.networking.WebService;
import com.uninorte.edu.co.tracku.networking.WebServiceManagerInterface;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OSMActivity extends AppCompatActivity implements GPSManagerInterface, WebServiceManagerInterface {

    Activity thisActivity=this;
    GPSManager gpsManager;
    double latitude;
    String lat;
    double longitude;
    String lon;
    static TrackUDatabaseManager INSTANCE;
    MapView map;
    String user;
    public WebService webService;

    private void checkForDatabase(){
        if (MainActivity.INSTANCE == null){
            MainActivity.getDatabase(this);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);
        setContentView(R.layout.osm_activity);
        checkPermissions();
        checkForDatabase();


        String callType=getIntent().getStringExtra("callType");
        webService = new WebService();
        String respuesta;
        if(callType.equals("userLogin")) {
            String userName = getIntent().getStringExtra("userName");
            user = userName;
            String password = getIntent().getStringExtra("password");
            System.out.println("User login");
            if(checkConnection()){
                respuesta = "";
                try{
                    respuesta = this.webService.execute("http://192.168.0.12:8080/restweb/webresources/control/getdata?id="+userName+"&pas="+password).get();
                    if(!respuesta.equals("")){
                        Toast.makeText(this, "Successful Login!", Toast.LENGTH_LONG).show();
                        this.user = userName;
                        checkPermissions();
                    }else {
                        Toast.makeText(this, "User not found WS!", Toast.LENGTH_LONG).show();
                        finish();
                    }
                }catch (Exception e){
                    Toast.makeText(this, "User not found!", Toast.LENGTH_LONG).show();
                    finish();
                }
            }else{
                if (!userAuth(userName, password)) {
                    Toast.makeText(this, "User not found!", Toast.LENGTH_LONG).show();
                    finish();
                }else{
                    this.user = userName;
                    checkPermissions();
                }
            }



        }else if(callType.equals("userRegistration")) {
            String userName = getIntent().getStringExtra("userName");
            String password = getIntent().getStringExtra("password");
            if(checkConnection()){
                respuesta = "";
                try{
                    respuesta = webService.execute("http://192.168.0.12:8080/restweb/webresources/control/query?id="+userName+"&pas="+password).get();
                    if(!respuesta.equals("")){
                        Toast.makeText(this, "Successful Register!", Toast.LENGTH_LONG).show();
                        finish();
                    }else {
                        Toast.makeText(this, "Unsuccessful Register WS!"+respuesta, Toast.LENGTH_LONG).show();
                        finish();
                    }
                }catch (Exception e){
                    Toast.makeText(this, "Unsuccessful Register catch!", Toast.LENGTH_LONG).show();
                    finish();
                }
            }else {
                if (!userRegistration(userName, password)) {
                    Toast.makeText(this, "Error while registering user!", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(this, "User registered!", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }else{
            finish();
        }


        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        map = (MapView) findViewById(R.id.oms_map);
        map.setTileSource(TileSourceFactory.MAPNIK);

    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        map.onResume();

        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        MyLocationNewOverlay myLocationNewOverlay=
                new MyLocationNewOverlay(
                        new GpsMyLocationProvider(this),map);
        myLocationNewOverlay.enableMyLocation();
        this.map.getOverlays().add(myLocationNewOverlay);
    }
    static TrackUDatabaseManager getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (TrackUDatabaseManager.class) {
                if (INSTANCE == null) {
                    INSTANCE= Room.databaseBuilder(context,
                            TrackUDatabaseManager.class, "database-tracku").
                            allowMainThreadQueries().fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
    boolean checkConnection(){
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()){
            return true;
        }else {
            return false;
        }
    }
    public boolean userAuth(String userName,String password){
        try{
            List<User> usersFound=getDatabase(this).userDao().getUserByEmail(userName);
            if(usersFound.size()>0){
                if(usersFound.get(0).passwordHash.equals(md5(password))){
                    return true;
                }
            }else{
                return false;
            }
        }catch (Exception error){
            Toast.makeText(this,error.getMessage(),Toast.LENGTH_LONG).show();
        }
        return false;
    }

    public boolean userRegistration(String userName, String password){
        try{
            User newUser=new User();
            newUser.email=userName;
            newUser.passwordHash=md5(password);
            INSTANCE.userDao().insertUser(newUser);
        }catch (Exception error){
            Toast.makeText(this,error.getMessage(),Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    public String md5(String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i=0; i<messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public boolean locationRegistration(String latitude, String longitude, String userID, String date){
        try{
            Historical newHistorical = new Historical();
            newHistorical.latitude=latitude;
            newHistorical.longitude=longitude;
            newHistorical.date=date;
            newHistorical.userID=userID;
            INSTANCE.historicalDao().insertHistorical(newHistorical);
        }catch (Exception error){
            Toast.makeText(this,error.getMessage(),Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    public void checkPermissions(){
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            AlertDialog.Builder builder=new AlertDialog.Builder(this);
            builder.setMessage(
                    "We need the GPS location to track U and other permissions, please grant all the permissions...");
            builder.setTitle("Permissions granting");
            builder.setPositiveButton(R.string.accept,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(thisActivity,
                                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                            Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.READ_EXTERNAL_STORAGE,
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE},1227);
                        }
                    });
            AlertDialog dialog=builder.create();
            dialog.show();
            return;
        }else{
            this.gpsManager=new GPSManager(this,this);
            gpsManager.InitLocationManager();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==1227){
            if(grantResults[0]!=PackageManager.PERMISSION_GRANTED){
                AlertDialog.Builder builder=new AlertDialog.Builder(this);
                builder.setMessage(
                        "The permissions weren't granted, the app will be closed");
                builder.setTitle("Permissions granting");
                builder.setPositiveButton(R.string.accept,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        });
                AlertDialog dialog=builder.create();
                dialog.show();
            }else{
                this.gpsManager=new GPSManager(this,this);
                gpsManager.InitLocationManager();
            }
        }
    }



    @Override
    public void LocationReceived(double latitude, double longitude) {

        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        DecimalFormat df = new DecimalFormat("0.000000");
        String respuesta;
        if(checkConnection()){
            respuesta = "";
            try{
                respuesta = webService.execute("http://192.168.0.12:8080/restweb/webresources/control/sethistorico?id="+user+"&lat="+String.valueOf(latitude)+"&lon="+String.valueOf(longitude)+"&date="+date).get();
                if(!respuesta.equals("")){
                    Toast.makeText(this, "Location Registered! WS", Toast.LENGTH_LONG).show();
                    finish();
                }else {
                    Toast.makeText(this, "Location NOT Register WS!"+respuesta, Toast.LENGTH_LONG).show();
                    finish();
                }
            }catch (Exception e){
                Toast.makeText(this, "Location Register catch!", Toast.LENGTH_LONG).show();
                finish();
            }
        }else {
            if (!locationRegistration(String.valueOf(latitude), String.valueOf(longitude),user,date)) {
                Toast.makeText(this, "Error while registering location!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Location registered!", Toast.LENGTH_LONG).show();
            }
        }

        this.latitude=latitude;
        this.longitude=longitude;


        this.setCenter(latitude,longitude);
    }

    @Override
    public void GPSManagerException(Exception error) {

    }

    @Override
    public void WebServiceMessageReceived(String userState, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplication(),message, Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * Centra la posición de una persona en el mapa
     * @param latitude
     * @param longitude
     */
    public void setCenter(double latitude, double longitude){

        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        DecimalFormat df = new DecimalFormat("0.000000");
        IMapController mapController = map.getController();
        mapController.setZoom(9.5);
        GeoPoint newCenter = new GeoPoint(latitude, longitude);
        mapController.setCenter(newCenter);
        lat = df.format(latitude);
        lon = df.format(longitude);

        Marker startMarker = new Marker(map);
        startMarker.setPosition(newCenter);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        startMarker.setTitle("<"+lat+","+lon+">"+date);
        map.getOverlays().add(startMarker);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}