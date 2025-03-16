package edu.ismt.dipendra.mealmate.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

/**
 * Helper class to manage Google Play Services connections
 */
public class GooglePlayServicesHelper {
    
    private static final String TAG = "GooglePlayServicesHelper";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    
    /**
     * Check if Google Play Services is available and up to date
     * @param context Application context
     * @return true if Google Play Services is available and up to date
     */
    public static boolean isGooglePlayServicesAvailable(Context context) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
        return resultCode == ConnectionResult.SUCCESS;
    }
    
    /**
     * Check if Google Play Services is available and up to date, and show dialog if not
     * @param activity Activity to show dialog
     * @return true if Google Play Services is available and up to date
     */
    public static boolean checkPlayServicesWithDialog(Activity activity) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(activity);
        
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(activity, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.e(TAG, "This device is not supported for Google Play Services");
            }
            return false;
        }
        
        // Force a connection to ensure package name is registered correctly
        try {
            apiAvailability.getErrorDialog(activity, ConnectionResult.SUCCESS, PLAY_SERVICES_RESOLUTION_REQUEST);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Google Play Services: " + e.getMessage());
        }
        
        return true;
    }
    
    /**
     * Handle the result of a Google Play Services resolution
     * @param activity Activity that received the result
     * @param requestCode Request code from onActivityResult
     * @param resultCode Result code from onActivityResult
     * @return true if the result was handled
     */
    public static boolean handlePlayServicesResolutionResult(Activity activity, int requestCode, int resultCode) {
        if (requestCode == PLAY_SERVICES_RESOLUTION_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                Log.i(TAG, "Google Play Services resolution successful");
                return true;
            } else {
                Log.e(TAG, "Google Play Services resolution failed");
                return false;
            }
        }
        return false;
    }
} 