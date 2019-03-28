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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.StringRequest;
import com.github.clans.fab.FloatingActionButton;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.uninorte.edu.co.tracku.com.uninorte.edu.co.tracku.gps.GPSManager;
import com.uninorte.edu.co.tracku.com.uninorte.edu.co.tracku.gps.GPSManagerInterface;
import com.uninorte.edu.co.tracku.database.core.TrackUDatabaseManager;
import com.uninorte.edu.co.tracku.database.daos.HistoricalDao;
import com.uninorte.edu.co.tracku.database.entities.Historical;
import com.uninorte.edu.co.tracku.database.entities.User;
import com.uninorte.edu.co.tracku.networking.WebService;
import com.uninorte.edu.co.tracku.networking.WebServiceManager;
import com.uninorte.edu.co.tracku.networking.WebServiceManagerInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class OSMActivity extends AppCompatActivity implements GPSManagerInterface, WebServiceManagerInterface, View.OnClickListener, CuadroDialogo.FinalizoCuadroDialogo {

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
    Context context;
    private int mInterval = 30000; // 30 seconds by default
    private Handler mHandler;



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

        context = this;
        String callType=getIntent().getStringExtra("callType");
        webService = new WebService();
        String respuesta;
        if(callType.equals("userLogin")) {
            String userName = getIntent().getStringExtra("userName");
            user = userName;
            boolean sw = true;
            String password = getIntent().getStringExtra("password");
            System.out.println("User login");
            if(!checkConnection()){
                respuesta = "";
                try{
                    respuesta = this.webService.execute("http://192.168.0.12:8080/restweb/webresources/control/getdata?id="+userName+"&pas="+password).get();
                    if(!respuesta.equals("")){
                        Toast.makeText(this, "Successful Login WS!", Toast.LENGTH_LONG).show();
                        this.user = userName;
                        checkPermissions();
                    }else {
                        Toast.makeText(this, "User not found WS!", Toast.LENGTH_LONG).show();
                        finish();
                    }
                }catch (Exception e){
                    Toast.makeText(this, "Failed Connection WS", Toast.LENGTH_LONG).show();
                    sw = false;
                }
                if(sw==false){
                    if (!userAuth(userName, password)) {
                        Toast.makeText(this, "User not found!", Toast.LENGTH_LONG).show();
                        finish();
                    }else{
                        this.user = userName;
                        checkPermissions();
                    }
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
            boolean sw = true;
            String password = getIntent().getStringExtra("password");
            if(checkConnection()){
                respuesta = "";
                try{
                    respuesta = webService.execute("http://192.168.0.12:8080/restweb/webresources/control/query?id="+userName+"&pas="+password).get();
                    if(!respuesta.equals("")){
                        Toast.makeText(this, "Successful Register WS!", Toast.LENGTH_LONG).show();
                        finish();
                    }else {
                        Toast.makeText(this, "Unsuccessful Register WS!"+respuesta, Toast.LENGTH_LONG).show();
                        finish();
                    }
                }catch (Exception e){
                    Toast.makeText(this, "Connection Failed WS!", Toast.LENGTH_LONG).show();
                    sw = false;
                }
                if(sw == false){
                    if (!userRegistration(userName, password)) {
                        Toast.makeText(this, "Error while registering user!", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(this, "User registered!", Toast.LENGTH_LONG).show();
                        finish();
                    }
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

        FloatingActionButton floatingActionButton=
                (FloatingActionButton)
                        findViewById(R.id.draw_Users);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkConnection()){
                    try{
                        drawUsers();
                    }catch (Exception e){
                        System.out.println("Fallo");
                    }
                }
            }
        });

        FloatingActionButton floatingActionButton2=
                (FloatingActionButton)
                        findViewById(R.id.draw_dates);
        floatingActionButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new CuadroDialogo(context,OSMActivity.this);
            }
        });

        mHandler = new Handler();
        //startRepeatingTask();
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
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

    public void drawUsers() throws ExecutionException, InterruptedException, JSONException {

        String respuesta = "";

        WebService webService = new WebService();
        respuesta = webService.execute("http://192.168.0.12:8080/restweb/webresources/control/gethistoricotodos").get();
        JSONArray jsonArray = new JSONArray(respuesta);
        for (int i = 0; i < jsonArray.length(); i++){
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String userID = jsonObject.getString("id");
            String lat = jsonObject.getString("lat");
            String lon = jsonObject.getString("lon");
            String date = jsonObject.getString("fecha");
            String status = jsonObject.getString("ONLINE");
            GeoPoint point = new GeoPoint(Double.parseDouble(lat), Double.parseDouble(lon));
            Marker startMarker = new Marker(map);
            startMarker.setPosition(point);
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            startMarker.setTitle("<"+lat+","+lon+">"+date+" User: "+userID+" Online: "+ status);
            map.getOverlays().add(startMarker);
        }
        if(jsonArray.length()==0){
            Toast.makeText(this, "No users to show", Toast.LENGTH_LONG).show();
        }
    }

    public void drawDates(String usuario, String ini, String fin) throws JSONException, ExecutionException, InterruptedException {
        ini = ini.replaceAll(" ","%20");
        fin = fin.replaceAll(" ","%20");
        String respuesta = "";
        webService = new WebService();
        respuesta = webService.execute("http://192.168.0.12:8080/restweb/webresources/control/gethistorico?id="+usuario+"&ini="+ini+"&fin="+fin).get();
        JSONArray jsonArray = new JSONArray(respuesta);
        for (int i = 0; i < jsonArray.length(); i++){
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String userID = usuario;
            String lat = jsonObject.getString("LATITUD");
            String lon = jsonObject.getString("LONGITUD");
            String date = jsonObject.getString("DATE");
            GeoPoint point = new GeoPoint(Double.parseDouble(lat), Double.parseDouble(lon));
            Marker startMarker = new Marker(map);
            startMarker.setPosition(point);
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            startMarker.setTitle("<"+lat+","+lon+">"+date+" User: "+userID+" Status: Unknown");
            map.getOverlays().add(startMarker);
        }
        if(jsonArray.length()==0){
            Toast.makeText(this, "User does not have locations registered in those dates", Toast.LENGTH_LONG).show();
        }
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
    public void SyncUploadLoc() throws JSONException, ExecutionException, InterruptedException {
        List<Historical> historicals = getDatabase(this).historicalDao().getAllHistoricals();
        JSONArray jsArray2 = new JSONArray(historicals);
        for (int i = 0; i < jsArray2.length(); i++) {
            JSONObject jsonObject = jsArray2.getJSONObject(i);
            String userID = jsonObject.getString("id");
            String lati = jsonObject.getString("lat");
            String longi= jsonObject.getString("lon");
            String dat = jsonObject.getString("fecha");
            String respuesta = "";
            webService = new WebService();
            respuesta = webService.execute("http://192.168.0.12:8080/restweb/webresources/control/sethistorico?id="+userID+"&lat="+lati+"&lon="+longi+"&date="+dat).get();
        }
        getDatabase(this).historicalDao().nukeTable();
    }

    public void SyncUploadUser() throws JSONException, ExecutionException, InterruptedException {
        List<User> users = getDatabase(this).userDao().getAllUsers();
        JSONArray jsArray2 = new JSONArray(users);
        for (int i = 0; i < jsArray2.length(); i++) {
            JSONObject jsonObject = jsArray2.getJSONObject(i);
            String userID = jsonObject.getString("userId");
            String password = jsonObject.getString("password_hash");
            String respuesta = "";
            webService = new WebService();
            respuesta = webService.execute("http://192.168.0.12:8080/restweb/webresources/control/query?id="+userID+"&pas="+password).get();
        }
        getDatabase(this).userDao().nukeTable();
    }

    Runnable WSStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                String respuesta = "";
                webService = new WebService();
                respuesta = webService.execute("http://192.168.0.12:8080/restweb/webresources/control/serverline").get();
                if(!respuesta.equals("")){
                    SyncUploadLoc();
                    SyncUploadUser();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(WSStatusChecker, mInterval);
            }
        }
    };


    void startRepeatingTask() {
        WSStatusChecker.run();
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(WSStatusChecker);
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
        date = date.replaceAll(" ","%20");
        String respuesta;
        boolean sw = true;
            respuesta = "";
            try{
                webService = new WebService();
                respuesta = webService.execute("http://192.168.0.12:8080/restweb/webresources/control/sethistorico?id="+user+"&lat="+String.valueOf(latitude)+"&lon="+String.valueOf(longitude)+"&date="+date).get();
                if(respuesta.equals("")){
                    Toast.makeText(this, "Location Not Registered WS !", Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(this, "Location Register WS!", Toast.LENGTH_LONG).show();
                }
            }catch (Exception e){
                Toast.makeText(this, "Location Register catch!", Toast.LENGTH_LONG).show();
                sw = false;
            }
            if(!sw){
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
    public void WebServiceMessageReceived(final String userState, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplication(),message, Toast.LENGTH_SHORT).show();

                if(userState=="SaveLocation"){
                    Log.i("CallWebServiceOperation_SaveLocation","OK");
                }
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
        startMarker.setTitle("<"+lat+","+lon+">"+date+" User: "+user+ " Online: true");
        map.getOverlays().add(startMarker);
    }

    @Override
    protected void onStop() {
        super.onStop();
        webService = new WebService();
        try {
            webService.execute("http://192.168.0.12:8080/restweb/webresources/control/setstatus?id="+user).get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRepeatingTask();
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void ResultadoCuadroDialogo(String id, String Fini, String Ffin) {

        try{
            drawDates(id,Fini,Ffin);
        }catch(Exception e){
            Toast.makeText(this, "Draw Dates not possible!", Toast.LENGTH_LONG).show();
        }


    }
}