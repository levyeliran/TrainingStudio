package zina_eliran.app;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Context;
import com.firebase.client.Firebase;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

import zina_eliran.app.API.ServerAPI;
import zina_eliran.app.BusinessEntities.CMNLogHelper;


public class BaseActivity extends AppCompatActivity {

    //create shared preferences scope for all activities
    final String appPreferences = "trainingStudioPreferences";
    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    ServerAPI sApi;
    Context appContext;
    Intent intent;
    DateFormat dateFormatter = new SimpleDateFormat("dd/MM/yy");
    DateFormat timeFormatter = new SimpleDateFormat("kk:mm"); //kk = 1-24

    GoogleApiClient client;
    LocationRequest mLocationRequest;
    PendingResult<LocationSettingsResult> result;

    //permission types code
    static final Integer LOCATION = 0x1;
    static final Integer CALL = 0x2;
    static final Integer WRITE_EXST = 0x3;
    static final Integer READ_EXST = 0x4;
    static final Integer CAMERA = 0x5;
    static final Integer ACCOUNTS = 0x6;
    static final Integer GPS_SETTINGS = 0x7;

    static final int RESULT_LOAD_IMG = 0x8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sApi = ServerAPI.getInstance();
        appContext = this;
        Firebase.setAndroidContext(this);

        //create the Shared Preferences read/write objects
        preferences = getSharedPreferences(appPreferences, 0);
        editor = preferences.edit();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //"force" LTR alignment + english language
        String languageToLoad = "en";
        Locale locale = new Locale(languageToLoad);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());


        client = new GoogleApiClient.Builder(this)
                .addApi(AppIndex.API)
                .addApi(LocationServices.API)
                .build();


        //************************************************************
        //run this once when you want to "init" the registration process
        //clearSharedPreferences();
    }

    @Override
    public void onStart() {
        super.onStart();
        client.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        client.disconnect();
    }

    public String readFromSharedPreferences(String key) {
        String returnedValue = "";
        try {
            String value = preferences.getString(key, "");
            if (!value.equalsIgnoreCase("")) {
                returnedValue = value;
            }
        } catch (Exception e) {
            CMNLogHelper.logError("BaseActivity", e.getMessage());
        }
        return returnedValue;
    }

    public boolean writeToSharedPreferences(String key, String value) {
        try {
            editor.putString(key, value);
            editor.commit();
            return true;
        } catch (Exception e) {
            CMNLogHelper.logError("BaseActivity", e.getMessage());
        }
        return false;
    }

    public boolean deleteFromSharedPreferences(String key) {
        try {
            preferences.edit().remove(key).commit();
            return true;
        } catch (Exception e) {
            CMNLogHelper.logError("BaseActivity", e.getMessage());
        }
        return false;
    }

    public boolean clearSharedPreferences() {
        try {
            preferences.edit().clear().commit();
            return true;
        } catch (Exception e) {
            CMNLogHelper.logError("BaseActivity", e.getMessage());
        }
        return false;
    }

    public void navigateToActivity(Activity current, java.lang.Class<?> nextClass, boolean isFinishCurrentActivity, Map<String, String> intentParams) {
        try {
            Intent openActivity = new Intent(current, nextClass);
            if (isFinishCurrentActivity) {
                openActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            }

            //add intent params
            if (intentParams != null) {
                for (String k : intentParams.keySet()) {
                    openActivity.putExtra(k, intentParams.get(k));
                }
            }
            current.startActivity(openActivity);

            if (isFinishCurrentActivity) {
                current.finish();
            }

        } catch (Exception e) {
            CMNLogHelper.logError("BaseActivity", e.getMessage());
        }
    }

    public boolean isRegistered() {
        return !readFromSharedPreferences(_getString(R.string.user_registration_permission)).isEmpty();
    }

    public boolean isVerified() {
        return !readFromSharedPreferences(_getString(R.string.user_verification_permission)).isEmpty();
    }

    public Context _getAppContext() {
        return appContext;
    }

    public String getIntentParam(Intent intent, String key) {
        String result = null;
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                result = extras.getString(key);
            }
        }
        return result != null ? result : "";
    }

    public String _getString(int key) {
        return appContext.getString(key);
    }


    //permissions

    public void askForPermission(String permission, Integer requestCode) {
        try {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {

                    //This is called if user has denied the permission before
                    //In this case I am just asking the permission again
                    ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);

                } else {

                    ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
                }
            } else {
                CMNLogHelper.logError("BaseActivity", permission + " is already granted.");
            }
        } catch (Exception e) {
            CMNLogHelper.logError("BaseActivity", e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        try {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED) {
                switch (requestCode) {
                    //Location
                    case 1:
                        askForGPS();
                        break;
                    //Write external Storage
                    case 3:
                        break;
                    //Read External Storage
                    case 4:
                        Intent imageIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(imageIntent, 11);
                        break;
                    //Camera
                    case 5:
                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                            startActivityForResult(takePictureIntent, 12);
                        }
                        break;
                }

                //Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                //Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            CMNLogHelper.logError("BaseActivity", e.getMessage());
        }
    }

    private void askForGPS() {
        try {
            mLocationRequest = LocationRequest.create();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLocationRequest.setInterval(30 * 1000);
            mLocationRequest.setFastestInterval(5 * 1000);
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
            builder.setAlwaysShow(true);
            result = LocationServices.SettingsApi.checkLocationSettings(client, builder.build());
            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                    final Status status = result.getStatus();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:
                            break;
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            try {
                                status.startResolutionForResult(BaseActivity.this, GPS_SETTINGS);
                            } catch (IntentSender.SendIntentException e) {
                                CMNLogHelper.logError("BaseActivity", e.getMessage());
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            break;
                    }
                }
            });
        } catch (Exception e) {
            CMNLogHelper.logError("BaseActivity", e.getMessage());
        }
    }


}
