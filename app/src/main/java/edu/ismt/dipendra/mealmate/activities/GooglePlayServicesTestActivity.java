package edu.ismt.dipendra.mealmate.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.utils.GooglePlayServicesHelper;

/**
 * Test activity to verify Google Play Services integration hello
 */
public class GooglePlayServicesTestActivity extends AppCompatActivity {

    private static final String TAG = "GooglePlayServicesTest";
    private TextView tvStatus;
    private Button btnCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_play_services_test);

        // Initialize views
        tvStatus = findViewById(R.id.tvStatus);
        btnCheck = findViewById(R.id.btnCheck);
        Toolbar toolbar = findViewById(R.id.toolbar);

        // Set up toolbar
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Google Play Services Test");
        }

        // Set up button click listener
        btnCheck.setOnClickListener(v -> checkGooglePlayServices());

        // Check Google Play Services on startup
        checkGooglePlayServices();
    }

    /**
     * Check Google Play Services availability and display status
     */
    private void checkGooglePlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        
        StringBuilder statusText = new StringBuilder();
        statusText.append("Google Play Services Status:\n\n");
        
        switch (resultCode) {
            case ConnectionResult.SUCCESS:
                statusText.append("✅ SUCCESS: Google Play Services is available and up to date.");
                Log.d(TAG, "Google Play Services is available and up to date");
                break;
            case ConnectionResult.SERVICE_MISSING:
                statusText.append("❌ ERROR: Google Play Services is missing on this device.");
                Log.e(TAG, "Google Play Services is missing on this device");
                break;
            case ConnectionResult.SERVICE_UPDATING:
                statusText.append("⏳ UPDATING: Google Play Services is currently updating.");
                Log.w(TAG, "Google Play Services is currently updating");
                break;
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                statusText.append("⚠️ UPDATE REQUIRED: Google Play Services needs to be updated.");
                Log.w(TAG, "Google Play Services needs to be updated");
                break;
            case ConnectionResult.SERVICE_DISABLED:
                statusText.append("❌ DISABLED: Google Play Services is disabled on this device.");
                Log.e(TAG, "Google Play Services is disabled on this device");
                break;
            case ConnectionResult.SERVICE_INVALID:
                statusText.append("❌ INVALID: The version of Google Play Services installed is not authentic.");
                Log.e(TAG, "The version of Google Play Services installed is not authentic");
                break;
            default:
                statusText.append("❓ UNKNOWN ERROR: Google Play Services status code: ").append(resultCode);
                Log.e(TAG, "Google Play Services error code: " + resultCode);
                break;
        }
        
        // Add package name information
        statusText.append("\n\nPackage Name: ").append(getPackageName());
        
        // Display the status
        tvStatus.setText(statusText.toString());
        
        // Show dialog if there's an error
        if (resultCode != ConnectionResult.SUCCESS) {
            GooglePlayServicesHelper.checkPlayServicesWithDialog(this);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 