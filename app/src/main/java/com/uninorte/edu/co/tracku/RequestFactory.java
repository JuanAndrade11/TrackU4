package com.uninorte.edu.co.tracku;

import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

/**
 * author JuanPablo
 */
public interface RequestFactory {

    public StringRequest getRequest(Response.Listener<String> responseListener,
                                    Response.ErrorListener errorListener);

}
