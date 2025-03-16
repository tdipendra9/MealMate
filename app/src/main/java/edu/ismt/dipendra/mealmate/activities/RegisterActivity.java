package edu.ismt.dipendra.mealmate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.firebase.FirebaseHelper;
import edu.ismt.dipendra.mealmate.helpers.InputValidation;
import edu.ismt.dipendra.mealmate.model.User;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {

    private final AppCompatActivity activity = RegisterActivity.this;

    private MaterialToolbar toolbar;
    private TextInputLayout textInputLayoutName;
    private TextInputLayout textInputLayoutEmail;
    private TextInputLayout textInputLayoutPassword;
    private TextInputLayout textInputLayoutConfirmPassword;

    private TextInputEditText textInputEditTextName;
    private TextInputEditText textInputEditTextEmail;
    private TextInputEditText textInputEditTextPassword;
    private TextInputEditText textInputEditTextConfirmPassword;

    private AppCompatButton appCompatButtonRegister;
    private CircularProgressIndicator progressIndicator;
    private TextView textViewLinkLogin;

    private InputValidation inputValidation;
    private FirebaseHelper firebaseHelper;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        
        initViews();
        setupToolbar();
        initListeners();
        initObjects();
    }
    
    /**
     * Setup toolbar
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * This method is to initialize views
     */
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        textInputLayoutName = findViewById(R.id.textInputLayoutName);
        textInputLayoutEmail = findViewById(R.id.textInputLayoutEmail);
        textInputLayoutPassword = findViewById(R.id.textInputLayoutPassword);
        textInputLayoutConfirmPassword = findViewById(R.id.textInputLayoutConfirmPassword);

        textInputEditTextName = findViewById(R.id.textInputEditTextName);
        textInputEditTextEmail = findViewById(R.id.textInputEditTextEmail);
        textInputEditTextPassword = findViewById(R.id.textInputEditTextPassword);
        textInputEditTextConfirmPassword = findViewById(R.id.textInputEditTextConfirmPassword);

        appCompatButtonRegister = findViewById(R.id.appCompatButtonRegister);
        progressIndicator = findViewById(R.id.progressIndicator);
        textViewLinkLogin = findViewById(R.id.textViewLinkLogin);
    }

    /**
     * This method is to initialize listeners
     */
    private void initListeners() {
        appCompatButtonRegister.setOnClickListener(this);
        textViewLinkLogin.setOnClickListener(this);
    }

    /**
     * This method is to initialize objects to be used
     */
    private void initObjects() {
        inputValidation = new InputValidation(activity);
        firebaseHelper = FirebaseHelper.getInstance();
        user = new User();
    }

    /**
     * This implemented method is to listen the click on view
     *
     * @param v view that is clicked
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.appCompatButtonRegister) {
            registerUser();
        } else if (v.getId() == R.id.textViewLinkLogin) {
            finish();
        }
    }

    /**
     * This method is to validate the input text fields and register user in Firebase
     */
    private void registerUser() {
        if (!inputValidation.isInputEditTextFilled(textInputEditTextName, textInputLayoutName, getString(R.string.error_message_name))) {
            return;
        }
        if (!inputValidation.isInputEditTextFilled(textInputEditTextEmail, textInputLayoutEmail, getString(R.string.error_message_email))) {
            return;
        }
        if (!inputValidation.isInputEditTextEmail(textInputEditTextEmail, textInputLayoutEmail, getString(R.string.error_message_email))) {
            return;
        }
        if (!inputValidation.isInputEditTextFilled(textInputEditTextPassword, textInputLayoutPassword, getString(R.string.error_message_password))) {
            return;
        }
        if (!inputValidation.isInputEditTextMatches(textInputEditTextPassword, textInputEditTextConfirmPassword,
                textInputLayoutConfirmPassword, getString(R.string.error_password_match))) {
            return;
        }

        // Show progress indicator
        progressIndicator.setVisibility(View.VISIBLE);
        appCompatButtonRegister.setEnabled(false);

        user.setName(textInputEditTextName.getText().toString().trim());
        user.setEmail(textInputEditTextEmail.getText().toString().trim());
        user.setPassword(textInputEditTextPassword.getText().toString().trim());

        firebaseHelper.registerUser(user, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                // Hide progress indicator
                progressIndicator.setVisibility(View.GONE);
                appCompatButtonRegister.setEnabled(true);
                
                // Registration successful
                Toast.makeText(RegisterActivity.this, getString(R.string.success_message), Toast.LENGTH_LONG).show();
                emptyInputEditText();

                // Navigate to LoginActivity
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                // Hide progress indicator
                progressIndicator.setVisibility(View.GONE);
                appCompatButtonRegister.setEnabled(true);
                
                // Check if error is due to email already in use
                if (e.getMessage() != null && e.getMessage().contains("email address is already in use")) {
                    Toast.makeText(RegisterActivity.this, getString(R.string.error_email_exists), Toast.LENGTH_LONG).show();
                } else {
                    // Other registration error
                    Toast.makeText(RegisterActivity.this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * This method is to empty all input edit text
     */
    private void emptyInputEditText() {
        textInputEditTextName.setText(null);
        textInputEditTextEmail.setText(null);
        textInputEditTextPassword.setText(null);
        textInputEditTextConfirmPassword.setText(null);
    }
} 