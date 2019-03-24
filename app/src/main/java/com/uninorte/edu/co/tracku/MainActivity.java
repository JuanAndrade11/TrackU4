package com.uninorte.edu.co.tracku;

import android.Manifest;
import android.app.Activity;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;

import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.uninorte.edu.co.tracku.com.uninorte.edu.co.tracku.gps.GPSManager;
import com.uninorte.edu.co.tracku.com.uninorte.edu.co.tracku.gps.GPSManagerInterface;
import com.uninorte.edu.co.tracku.database.core.TrackUDatabaseManager;
import com.uninorte.edu.co.tracku.database.entities.Historical;
import com.uninorte.edu.co.tracku.database.entities.User;
import com.uninorte.edu.co.tracku.networking.WebService;
import com.uninorte.edu.co.tracku.networking.WebServiceManager;
import com.uninorte.edu.co.tracku.networking.WebServiceManagerInterface;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        GPSManagerInterface, OnMapReadyCallback, OmsFragment.OnFragmentInteractionListener, WebServiceManagerInterface {

    Activity thisActivity=this;
    GPSManager gpsManager;
    GoogleMap googleMap;
    OSMActivity osmActivity;
    double latitude;
    double longitude;
    OmsFragment omsFragment;
    private String user;
    public WebService webService;

    static TrackUDatabaseManager INSTANCE;


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
    public String getUserId(){
        return getIntent().getStringExtra("userName");
    }
    public boolean locationRegistration(String latitude, String longitude, String userID, String date){
        try{
            Historical newHisrotical = new Historical();
            newHisrotical.latitude=latitude;
            newHisrotical.longitude=longitude;
            newHisrotical.date=date;
            newHisrotical.userID=userID;
            INSTANCE.historicalDao().insertHistorical(newHisrotical);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermissions();
        getDatabase(this);

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
                    respuesta = this.webService.execute("http://192.168.0.8:8080/restweb/webresources/control/getdata?id="+userName+"&pas="+password).get();
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
                    respuesta = webService.execute("http://192.168.0.8:8080/restweb/webresources/control/query?id="+userName+"&pas="+password).get();
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
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);



        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //SupportMapFragment supportMapFragment=(SupportMapFragment)
        //        this.getSupportFragmentManager().findFragmentById(R.id.google_maps_control);
        //supportMapFragment.getMapAsync(this);

        FloatingActionButton floatingActionButton1=
                (FloatingActionButton)
                        findViewById(R.id.zoom_in_button);
        floatingActionButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(googleMap!=null){
                    googleMap.moveCamera(CameraUpdateFactory.zoomIn());
                }
            }
        });


        FloatingActionButton floatingActionButton2=
                (FloatingActionButton)
                        findViewById(R.id.zoom_out_button);
        floatingActionButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(googleMap!=null){
                    googleMap.moveCamera(CameraUpdateFactory.zoomOut());
                }
            }
        });

        FloatingActionButton floatingActionButton3=
                (FloatingActionButton)
                        findViewById(R.id.focus_button);

        floatingActionButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(googleMap!=null){
                    googleMap.moveCamera(
                            CameraUpdateFactory.newLatLng(
                                    new LatLng(latitude,longitude)));
                }
            }
        });


    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            WebServiceManager.CallWebServiceOperation(this,"http://192.168.0.8:8080/resttest/webresources",
                    "maincontroller",
                    "operation",
                    "PUT",
                    "This is a test",
                    "Settings");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.google_maps_fragment_opt) {
            // Handle the camera action
        } else if (id == R.id.osm_fragment_opt) {
            this.omsFragment =  OmsFragment.newInstance("","");
            android.support.v4.app.FragmentTransaction fragmentTransaction =
                    getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.map_control, omsFragment);
            fragmentTransaction.commit();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
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
                        "The permissions weren't granted, then the app will be close");
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
        this.latitude=latitude;
        this.longitude=longitude;
        ((TextView)findViewById(R.id.latitude_value)).setText(latitude+"");
        ((TextView)findViewById(R.id.longitude_value)).setText(longitude+"");
        //if (!locationRegistration(String.valueOf(latitude), String.valueOf(longitude),user,date)) {
          //  Toast.makeText(this, "Error while registering location!", Toast.LENGTH_LONG).show();
        //} else {
          //  Toast.makeText(this, "Location registered!", Toast.LENGTH_LONG).show();
           // finish();
        //}
        if(osmActivity!=null) {
            osmActivity.setCenter(latitude, longitude);

        }
    }

    @Override
    public void GPSManagerException(Exception error) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap=googleMap;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void WebServiceMessageReceived(String userState, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplication(),message,Toast.LENGTH_SHORT).show();
            }
        });
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
}
