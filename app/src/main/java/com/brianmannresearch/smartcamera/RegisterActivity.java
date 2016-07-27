package com.brianmannresearch.smartcamera;

import android.content.DialogInterface;
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

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {

    Button registerButton, cancelButton;
    EditText registerUser, registerPassword, confirmPassword;
    TextView messageText;

    boolean username = false, password = false, match = false, success = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        registerButton = (Button) findViewById(R.id.completeRegister);
        cancelButton = (Button) findViewById(R.id.cancelRegister);
        registerUser = (EditText) findViewById(R.id.registerUserEdit);
        registerPassword = (EditText) findViewById(R.id.registerPasswordEdit);
        confirmPassword = (EditText) findViewById(R.id.confirmPasswordEdit);
        messageText = (TextView) findViewById(R.id.messageText);

        registerUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (registerUser.length() >= 6) {
                    username = true;
                    messageText.setText("");
                    if (password && match){
                        registerButton.setOnClickListener(RegisterActivity.this);
                    }
                } else {
                    username = false;
                    messageText.setText("Username must be at least six characters");
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        registerPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (goodPassword()) {
                    password = true;
                    messageText.setText("");
                    if (username && match){
                        registerButton.setOnClickListener(RegisterActivity.this);
                    }
                } else {
                    password = false;
                    messageText.setText("Password must be between 8 and fifteen characters, contain a lowercase and uppercase letter, and include a number");
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        confirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String pword = registerPassword.getText().toString(), confirm = confirmPassword.getText().toString();
                if (pword.matches(confirm)){
                    match = true;
                    messageText.setText("");
                    if (username && password){
                        registerButton.setOnClickListener(RegisterActivity.this);
                    }
                } else {
                    match = false;
                    messageText.setText("Passwords must match");
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        cancelButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.completeRegister:
                String username = registerUser.getText().toString();
                String password = registerPassword.getText().toString();
                String hexUser;
                try {
                    hexUser = passwordEncrypt(username);
                    password += hexUser;
                    password = passwordEncrypt(password);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Response.Listener<String> responseListener = new Response.Listener<String>(){

                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            success = jsonResponse.getBoolean("success");

                            if (success){
                                Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_LONG).show();
                                finish();
                            }else{
                                messageText.setText("Registration failed. Try again with a different username");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                };

                RegisterRequest registerRequest = new RegisterRequest(username, password, responseListener);
                RequestQueue queue = Volley.newRequestQueue(RegisterActivity.this);
                queue.add(registerRequest);
                break;
            case R.id.cancelRegister:
                showCancelAlert();
                break;
        }
    }

    private boolean goodPassword() {
        String password = registerPassword.getText().toString();
        if(password.length() < 8 || password.length() > 15){
            return false;
        } else if (!password.matches(".*[A-Z].*")){
            return false;
        } else if (!password.matches(".*[a-z].*")){
            return false;
        } else if (!password.matches(".*\\d.*")){
            return false;
        }
        return true;
    }

    private void showCancelAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        alertDialog.setTitle("Cancel");
        alertDialog.setMessage("Are you sure you want to cancel this registration?")
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
}
