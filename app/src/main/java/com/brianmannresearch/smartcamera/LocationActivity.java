package com.brianmannresearch.smartcamera;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationActivity extends AppCompatActivity implements View.OnClickListener {

    Button bConfirm, bCancel;
    EditText editText;
    Geocoder gc;
    List<Address> locationList;
    double latitude, longitude;
    double[] LatLng = new double[2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        bConfirm = (Button) findViewById(R.id.confirm_button);
        bCancel = (Button) findViewById(R.id.cancel_button);
        editText = (EditText) findViewById(R.id.city_name);

        bConfirm.setOnClickListener(this);
        bCancel.setOnClickListener(this);
    }

    public void onClick(View v){
        switch (v.getId()){
            case R.id.confirm_button:
                if(Geocoder.isPresent()){
                    String location = editText.getText().toString();

                    gc = new Geocoder(this, Locale.getDefault());
                    try {
                        locationList = gc.getFromLocationName(location, 1);
                    } catch (IOException e) {
                        Toast.makeText(getApplicationContext(), "Failed: " + e.toString(), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                    if(locationList.size() > 0){
                        latitude = locationList.get(0).getLatitude();
                        longitude = locationList.get(0).getLongitude();
                        LatLng[0] = latitude;
                        LatLng[1] = longitude;
                        Intent resultData = new Intent();
                        resultData.putExtra("latlng", LatLng);
                        setResult(Activity.RESULT_OK, resultData);
                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(), "No results found", Toast.LENGTH_LONG).show();
                    }
                }
                break;
            case R.id.cancel_button:
                showCancelAlert();
                break;
        }
    }

    @Override
    public void onBackPressed(){

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
}
