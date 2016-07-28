package com.brianmannresearch.smartcamera;



import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

public class RegisterRequest extends StringRequest {

    private static final String registerServerUrl = "http://10.25.172.60:80/my-site/register.php";
    private Map<String, String> params;

    public RegisterRequest(String username, String password, Response.Listener<String> listener){
        super(Method.POST, registerServerUrl, listener, null);
        params = new HashMap<>();
        params.put("username", username);
        params.put("password", password);
    }

    public Map<String, String> getParams(){
        return params;
    }
}
