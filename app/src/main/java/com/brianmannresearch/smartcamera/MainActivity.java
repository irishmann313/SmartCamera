package com.brianmannresearch.smartcamera;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button loginButton, registerButton, guestButton, exitButton;
    EditText username, password;

    TextView statusText;

    boolean userName = false, passWord = false, success = false;
    int userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginButton = (Button) findViewById(R.id.LoginButton);
        registerButton = (Button) findViewById(R.id.RegisterButton);
        guestButton = (Button) findViewById(R.id.GuestButton);
        exitButton = (Button) findViewById(R.id.ExitButton);

        username = (EditText) findViewById(R.id.LoginName);
        password = (EditText) findViewById(R.id.Password);

        statusText = (TextView) findViewById(R.id.StatusText);

        username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (username.length()>=6){
                    userName = true;
                    if (passWord){
                        loginButton.setOnClickListener(MainActivity.this);
                    }
                } else {
                    userName = false;
                    loginButton.setClickable(false);
                }
            }
        });

        password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (password.length()>=8 && password.length()<16){
                    passWord = true;
                    if (userName){
                        loginButton.setOnClickListener(MainActivity.this);
                    }
                } else {
                    passWord = false;
                    loginButton.setClickable(false);
                }
            }
        });

        registerButton.setOnClickListener(this);
        guestButton.setOnClickListener(this);
        exitButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.LoginButton:
                final String user = username.getText().toString();
                String pass = password.getText().toString();

                String hexUser;
                try {
                    hexUser = passwordEncrypt(user);
                    pass += hexUser;
                    pass = passwordEncrypt(pass);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Response.Listener<String> responseListener = new Response.Listener<String>(){

                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            success = jsonResponse.getBoolean("success");
                            userID = jsonResponse.getInt("userID");

                            if (success){
                                Intent loginIntent = new Intent(MainActivity.this, TripActivity.class);
                                loginIntent.putExtra("mode", "login");
                                loginIntent.putExtra("userid", userID);
                                loginIntent.putExtra("username", user);
                                MainActivity.this.startActivity(loginIntent);
                            }else{
                                statusText.setText("Login failed! Incorrect username or password");
                                Toast.makeText(MainActivity.this, "Login failed! Incorrect username or password", Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                };

                LoginRequest loginRequest = new LoginRequest(user, pass, responseListener);
                RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
                queue.add(loginRequest);
                break;
            case R.id.RegisterButton:
                startActivity(new Intent(this, RegisterActivity.class));
                break;
            case R.id.GuestButton:
                Intent guestIntent = new Intent(this, TripActivity.class);
                guestIntent.putExtra("mode", "guest");
                guestIntent.putExtra("username", "guest");
                startActivity(guestIntent);
                break;
            case R.id.ExitButton:
                showFinishAlert();
                break;
        }
    }

    private String passwordEncrypt(String password) throws Exception{
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(password.getBytes());

        byte byteData[] = md.digest();

        StringBuffer sb = new StringBuffer();
        for (int i=0; i<byteData.length; i++){
            sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    @Override
    public void onBackPressed(){

    }

    private void showFinishAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        alertDialog.setMessage("Are you sure you want to exit the app?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // do nothing
                    }
                });
        final AlertDialog alert = alertDialog.create();
        alert.show();
    }
}
