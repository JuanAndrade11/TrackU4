package com.uninorte.edu.co.tracku;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;


public class EnviaDatos {
    private String numero;

    public EnviaDatos(String numero){
        this.numero = numero;
    }

    public StringRequest getRequest(Response.Listener<String> responseListener, Response.ErrorListener errorListener){
        final HashMap<String,String> credenciales = new HashMap<>();
        credenciales.put("Num",numero);

        String url = "http://localhost/php/apiRest/public/api/usuarios";
        StringRequest request = new StringRequest(Request.Method.POST, url, responseListener, errorListener){

            public String getBodyContentType(){
                return "application/json charset="+getParamsEncoding();
            }

            public byte[] getBody(){
                try {
                    return new JSONObject(credenciales).toString().getBytes(getParamsEncoding());
                }catch (UnsupportedEncodingException e){

                }
                return null;
            }
        };
        request.setRetryPolicy(new LongTimeoutAndTryRetryPolicy(LongTimeoutAndTryRetryPolicy.RETRIES_PHONE_ISP));
        return request;
    }
}
