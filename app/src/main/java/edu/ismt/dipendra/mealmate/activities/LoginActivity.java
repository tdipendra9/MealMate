package edu.ismt.dipendra.mealmate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.firebase.FirebaseHelper;
import edu.ismt.dipendra.mealmate.helpers.InputValidation;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "LoginActivity";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private final AppCompatActivity activity = LoginActivity.this;

    private MaterialToolbar toolbar;
    private TextInputLayout textInputLayoutEmail;
    private TextInputLayout textInputLayoutPassword;

    private TextInputEditText textInputEditTextEmail;
    private TextInputEditText textInputEditTextPassword;

    private AppCompatButton appCompatButtonLogin;
    private CircularProgressIndicator progressIndicator;

    private TextView textViewLinkRegister;

    private InputValidation inputValidation;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // Check if Google Play Services is available
        if (!checkPlayServices()) {
            // Google Play Services is not available or outdated
            // The checkPlayServices method will handle showing the appropriate dialog
            // We can still continue with app initialization, but Firebase features might not work
        }
        
        initViews();
        setupToolbar();
        initListeners();
        initObjects();
        
        // Check if user is already logged in
        FirebaseUser currentUser = firebaseHelper.getCurrentUser();
        if (currentUser != null) {
            navigateToMainActivity();
        }
    }
    
    /**
     * Check if Google Play Services is available and up to date
     * @return true if Google Play Services is available and up to date
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Toast.makeText(this, "This device is not supported for Google Play Services", 
                        Toast.LENGTH_LONG).show();
                // Don't finish the activity here, as we want to allow the user to at least see the login screen
            }
            return false;
        }
        return true;
    }
    
    /**
     * Setup toolbar
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
    }

    /**
     * This method is to initialize views
     */
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        textInputLayoutEmail = findViewById(R.id.textInputLayoutEmail);
        textInputLayoutPassword = findViewById(R.id.textInputLayoutPassword);

        textInputEditTextEmail = findViewById(R.id.textInputEditTextEmail);
        textInputEditTextPassword = findViewById(R.id.textInputEditTextPassword);

        appCompatButtonLogin = findViewById(R.id.appCompatButtonLogin);
        progressIndicator = findViewById(R.id.progressIndicator);

        textViewLinkRegister = findViewById(R.id.textViewLinkRegister);
    }

    /**
     * This method is to initialize listeners
     */
    private void initListeners() {
        appCompatButtonLogin.setOnClickListener(this);
        textViewLinkRegister.setOnClickListener(this);
    }

    /**
     * This method is to initialize objects to be used
     */
    private void initObjects() {
        inputValidation = new InputValidation(activity);
        firebaseHelper = FirebaseHelper.getInstance();
    }

    /**
     * This implemented method is to listen the click on view
     *
     * @param v view that is clicked
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.appCompatButtonLogin) {
            loginUser();
        } else if (v.getId() == R.id.textViewLinkRegister) {
            // Navigate to RegisterActivity
            Intent intentRegister = new Intent(getApplicationContext(), RegisterActivity.class);
            startActivity(intentRegister);
        }
    }

    /**
     * This method is to validate the input text fields and verify login credentials from Firebase
     */
    private void loginUser() {
        if (!inputValidation.isInputEditTextFilled(textInputEditTextEmail, textInputLayoutEmail, getString(R.string.error_message_email))) {
            return;
        }
        if (!inputValidation.isInputEditTextEmail(textInputEditTextEmail, textInputLayoutEmail, getString(R.string.error_message_email))) {
            return;
        }
        if (!inputValidation.isInputEditTextFilled(textInputEditTextPassword, textInputLayoutPassword, getString(R.string.error_message_password))) {
            return;
        }

        // Show progress indicator
        progressIndicator.setVisibility(View.VISIBLE);
        appCompatButtonLogin.setEnabled(false);

        String email = textInputEditTextEmail.getText().toString().trim();
        String password = textInputEditTextPassword.getText().toString().trim();

        firebaseHelper.loginUser(email, password, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                // Hide progress indicator
                progressIndicator.setVisibility(View.GONE);
                appCompatButtonLogin.setEnabled(true);
                
                // Login successful
                emptyInputEditText();
                navigateToMainActivity();
            }

            @Override
            public void onFailure(Exception e) {
                // Hide progress indicator
                progressIndicator.setVisibility(View.GONE);
                appCompatButtonLogin.setEnabled(true);
                
                // Login failed
                Toast.makeText(LoginActivity.this, getString(R.string.error_valid_email_password), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Navigate to MainActivity
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(activity, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * This method is to empty all input edit text
     */
    private void emptyInputEditText() {
        textInputEditTextEmail.setText(null);
        textInputEditTextPassword.setText(null);
    }
} 