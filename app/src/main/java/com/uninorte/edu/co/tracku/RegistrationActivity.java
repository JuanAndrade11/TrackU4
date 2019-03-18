package com.uninorte.edu.co.tracku;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class RegistrationActivity extends Activity implements View.OnClickListener, Response.ErrorListener, Response.Listener<String> {

    EditText userName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_registration);

        findViewById(R.id.reg_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentToBeCalled=new Intent();
                String userName=((EditText)findViewById(R.id.reg_username_value)).getText()+"";
                String name=((EditText)findViewById(R.id.reg_name_value)).getText()+"";
                String password=((EditText)findViewById(R.id.reg_password_value)).getText()+"";
                String passwordConfirmation=((EditText)findViewById(R.id.reg_password_confirmation_value)).getText()+"";
                if(password.equals(passwordConfirmation)) {
                    intentToBeCalled.putExtra("callType", "userRegistration");
                    intentToBeCalled.putExtra("userName", userName);
                    intentToBeCalled.putExtra("name", name);
                    intentToBeCalled.putExtra("password", password);
                    intentToBeCalled.setClass(getApplicationContext(), MainActivity.class);
                    startActivity(intentToBeCalled);
                }
            }
        });


    }

    private void enviaDatos(){
        EnviaDatos enviaDatos = new EnviaDatos(userName.getText().toString());
        Request<?> request = enviaDatos.getRequest(this,this);
        AppController.getInstance().addToRequestQueue(request);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        enviaDatos();
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Log.e("ErrorResponse", error.getMessage());
    }

    @Override
    public void onResponse(String response) {
        GsonBuilder builder = new GsonBuilder();
        builder.setExclusionStrategies(new DefaultExclusionStrategy());
        Gson gson = builder.create();

        Datos modelo = gson.fromJson(response, Datos.class);
    }
}
