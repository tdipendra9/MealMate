package edu.ismt.dipendra.mealmate;

import android.app.Application;
import android.util.Log;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.ConnectionResult;
import com.google.firebase.FirebaseApp;

public class MealMateApplication extends Application {
    
    private static final String TAG = "MealMateApplication";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        
        // Check Google Play Services availability
        checkGooglePlayServices();
    }
    
    /**
     * Check if Google Play Services is available and up to date
     */
    private void checkGooglePlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e(TAG, "Google Play Services is not available or outdated. Error code: " + resultCode);
            
            if (apiAvailability.isUserResolvableError(resultCode)) {
                Log.i(TAG, "User resolvable error detected. This will be shown in activities.");
            } else {
                Log.e(TAG, "This device is not supported for Google Play Services");
            }
        } else {
            Log.i(TAG, "Google Play Services is available and up to date");
        }
    }
} 