package com.brianmannresearch.smartcamera;



import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

public class LoginRequest extends StringRequest {

    private static final String loginServerUrl = "http://10.13.147.183:80/my-site/login.php";
    private Map<String, String> params;

    public LoginRequest(String username, String password, Response.Listener<String> listener){
        super(Method.POST, loginServerUrl, listener, null);
        params = new HashMap<>();
        params.put("username", username);
        params.put("password", password);
    }

    public Map<String, String> getParams(){
        return params;
    }
}
