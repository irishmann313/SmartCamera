package com.brianmannresearch.smartcamera;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;

import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class GuestActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int TAKE_PICTURE = 0;
    private static final int RESULT_LOAD_IMAGE = 1;
    private static final int ESTIMATE_LOCATION = 2;

    ImageView selectedImage;
    Button bCamera, endButton, reviewButton;
    TextView exifData;
    String filename, mode, username = "guest";
    String [] filepath;
    File imagesFolder;
    int tripid;

    GPSTracker gps;
    StringBuilder builder;

    float[] latlong = new float[2];
    double[] latlng = new double[2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest);

        Bundle extras = getIntent().getExtras();
        if (extras != null){
            mode = extras.getString("mode");
            tripid = extras.getInt("tripid");
        }

        imagesFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), username + "_Trip_" + tripid);
        if ("new".matches(mode)) {
            imagesFolder.mkdirs();
        }

        builder = new StringBuilder();
        selectedImage = (ImageView) findViewById(R.id.selectedImage);
        bCamera = (Button) findViewById(R.id.bCamera);
        endButton = (Button) findViewById(R.id.endButton);
        exifData = (TextView) findViewById(R.id.ExifData);
        reviewButton = (Button) findViewById(R.id.reviewTrip);

        bCamera.setOnClickListener(this);
        endButton.setOnClickListener(this);
        selectedImage.setOnClickListener(this);
        reviewButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.selectedImage:
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, RESULT_LOAD_IMAGE);
                break;
            case R.id.bCamera:
                if (isLocationEnabled(this)) {
                    Intent photoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(photoIntent, TAKE_PICTURE);
                } else {
                    showSettingsAlert();
                }
                break;
            case R.id.endButton:
                showFinishAlert();
                break;
            case R.id.reviewTrip:
                Intent gIntent = new Intent(GuestActivity.this, GalleryActivity.class);
                gIntent.putExtra("tripid", tripid);
                gIntent.putExtra("username", username);
                startActivity(gIntent);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TAKE_PICTURE && resultCode == RESULT_OK){
            Bitmap chosenImage = (Bitmap) data.getExtras().get("data");
            selectedImage.setImageBitmap(chosenImage);

            filename = getOriginalImagePath();
            filepath = filename.split("/");

            gps = new GPSTracker(GuestActivity.this);
            ReadExif(filename, 0);

            File sourceFile = new File(filename);
            final File destFile = new File(imagesFolder, filepath[filepath.length-1]);
            try{
                copyFile(sourceFile, destFile);
                scanFile(destFile.getAbsolutePath());
            }catch (IOException ex){
                Toast.makeText(GuestActivity.this,  ex.toString(), Toast.LENGTH_LONG).show();
            }
        }else if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK){
            Uri chosenImage = data.getData();

            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(chosenImage, filePathColumn, null, null, null);
            assert cursor != null;
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            filename = cursor.getString(columnIndex);
            filepath = filename.split("/");
            cursor.close();

            Bitmap loadedBitmap = BitmapFactory.decodeFile(filename);

            ExifInterface exif = null;
            try {
                File pictureFile = new File(filename);
                exif = new ExifInterface(pictureFile.getAbsolutePath());
            }catch (IOException e){
                e.printStackTrace();
            }

            int orientation = ExifInterface.ORIENTATION_NORMAL;

            if (exif != null)
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation){
                case ExifInterface.ORIENTATION_ROTATE_90:
                    loadedBitmap = rotateBitmap(loadedBitmap, 90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    loadedBitmap = rotateBitmap(loadedBitmap, 180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    loadedBitmap = rotateBitmap(loadedBitmap, 270);
                    break;
            }

            selectedImage.setImageBitmap(loadedBitmap);
            assert exif != null;
            if (!exif.getLatLong(latlong)) {
                showDataAlert();
            }else{
                ReadExif(filename, 1);
            }
        }else if (requestCode == ESTIMATE_LOCATION && resultCode == RESULT_OK){
            latlng = data.getDoubleArrayExtra("latlng");
            ReadExif(filename, 1);
        }
        File sourceFile = new File(filename);
        final File destFile = new File(imagesFolder, filepath[filepath.length-1]);
        try{
            copyFile(sourceFile, destFile);
            scanFile(destFile.getAbsolutePath());
        }catch (IOException ex){
            Toast.makeText(GuestActivity.this,  ex.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private static Bitmap rotateBitmap(Bitmap bitmap, int degrees){
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private String getOriginalImagePath(){
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = GuestActivity.this.managedQuery(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, null);
        int column_index_data = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToLast();

        return cursor.getString(column_index_data);
    }

    private void ReadExif(String file, int mode){
        try{
            ExifInterface exifInterface = new ExifInterface(file);

            if (!exifInterface.getLatLong(latlong) && mode == 0) {
                double latitude = gps.getLatitude();
                double longitude = gps.getLongitude();
                exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, latitude < 0 ? "S" : "N");
                exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, longitude < 0 ? "W" : "E");
                exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE, getGeoCoordinates(Math.abs(latitude)));
                exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, getGeoCoordinates(Math.abs(longitude)));
                exifInterface.saveAttributes();
            }else if (!exifInterface.getLatLong(latlong) && mode == 1) {
                double latitude = latlng[0];
                double longitude = latlng[1];
                Toast.makeText(this, "Estimated photo", Toast.LENGTH_LONG).show();
                exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, latitude < 0 ? "S" : "N");
                exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, longitude < 0 ? "W" : "E");
                exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE, getGeoCoordinates(Math.abs(latitude)));
                exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, getGeoCoordinates(Math.abs(longitude)));
                exifInterface.saveAttributes();
            }

            builder = new StringBuilder();
            builder.append("Filename: ").append(filepath[filepath.length-1]).append("\n");
            builder.append("Date & Time: ").append(exifInterface.getAttribute(ExifInterface.TAG_DATETIME)).append("\n");
            builder.append("Trip ID: ").append(tripid).append("\n");
            if (mode == 2){
                builder.append("GPS Latitude: undefined\n");
                builder.append("GPS Longitude: undefined");
            }else {
                builder.append("GPS Latitude: ").append(exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE)).append(" ").append(exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)).append("\n");
                builder.append("GPS Longitude: ").append(exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)).append(" ").append(exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF));
            }
            exifData.setText(builder.toString());

            Toast.makeText(GuestActivity.this, "finished", Toast.LENGTH_LONG).show();
        }catch (IOException e){
            e.printStackTrace();
            Toast.makeText(GuestActivity.this, e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private String getGeoCoordinates(double lat){
        String[] degMinSec = Location.convert(lat, Location.FORMAT_SECONDS).split(":");
        return degMinSec[0] + "/1," + degMinSec[1] + "/1," + degMinSec[2] + "/1000";
    }

    private static boolean isLocationEnabled(Context context){
        int locationMode;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            try{
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

            }catch (Settings.SettingNotFoundException e){
                e.printStackTrace();
                return false;
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        }else{
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    private void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        alertDialog.setTitle("Location Services");
        alertDialog.setMessage("Your GPS seems to be disabled. Do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener(){
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id){
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = alertDialog.create();
        alert.show();
    }

    private void showDataAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        alertDialog.setTitle("Image Information");
        alertDialog.setMessage("Before you continue! Do you remember where this photo was taken?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener(){
                    public void onClick(final DialogInterface dialog, final int id) {
                        estimateLocation();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id){
                        dialog.cancel();
                        ReadExif(filename, 2);
                    }
                });
        final AlertDialog alert = alertDialog.create();
        alert.show();
    }

    private void showFinishAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        alertDialog.setMessage("Are you sure you want to end this trip?")
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

    private void estimateLocation(){
        Intent intent = new Intent(this, LocationActivity.class);
        startActivityForResult(intent, ESTIMATE_LOCATION);
    }

    @Override
    public void onBackPressed(){

    }

    private void copyFile(File sourceFile, File destFile) throws IOException{
        FileChannel source;
        FileChannel destination;
        source = new FileInputStream(sourceFile).getChannel();
        destination = new FileOutputStream(destFile).getChannel();
        if (source != null){
            destination.transferFrom(source, 0, source.size());
        }
        if (source != null){
            source.close();
        }
        destination.close();
    }

    private void scanFile(String path){
        MediaScannerConnection.scanFile(GuestActivity.this, new String[] { path }, null,
                new MediaScannerConnection.OnScanCompletedListener(){
                    public void onScanCompleted(String path, Uri uri){
                        Toast.makeText(GuestActivity.this, "Finished", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
