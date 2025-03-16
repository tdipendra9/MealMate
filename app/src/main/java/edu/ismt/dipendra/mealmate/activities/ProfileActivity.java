package edu.ismt.dipendra.mealmate.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.firebase.FirebaseHelper;
import edu.ismt.dipendra.mealmate.helpers.PreferenceManager;
import edu.ismt.dipendra.mealmate.model.User;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvUserName, tvUserEmail, tvFullName, tvEmail, tvPhone, tvAppVersion;
    private MaterialButton btnEditProfile;
    private View layoutLogout;
    private SwitchCompat switchDarkMode, switchNotifications;
    private ImageView ivProfilePicture;
    private PreferenceManager preferenceManager;
    private FirebaseAuth firebaseAuth;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseHelper = FirebaseHelper.getInstance();
        
        // Initialize PreferenceManager
        preferenceManager = new PreferenceManager(this);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Profile");
        }
        
        // Initialize views
        initViews();
        
        // Load user data
        loadUserData();
        
        // Set up listeners
        setupListeners();
    }

    @SuppressLint("StringFormatInvalid")
    private void initViews() {
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvFullName = findViewById(R.id.tvFullName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvAppVersion = findViewById(R.id.tvAppVersion);
        
        btnEditProfile = findViewById(R.id.btnEditProfile);
        layoutLogout = findViewById(R.id.layoutLogout);
        
        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchNotifications = findViewById(R.id.switchNotifications);
        
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        
        // Set app version
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            tvAppVersion.setText(getString(R.string.app_version, versionName));
        } catch (Exception e) {
            tvAppVersion.setText(getString(R.string.app_version, "1.0"));
        }
        
        // Set switch states from preferences
        switchDarkMode.setChecked(preferenceManager.isDarkModeEnabled());
        switchNotifications.setChecked(preferenceManager.areNotificationsEnabled());
    }

    private void loadUserData() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            // First, try to get basic information from Firebase Auth
            String email = currentUser.getEmail();
            tvEmail.setText(email != null ? email : "");
            tvUserEmail.setText(email != null ? email : "");
            
            // Load phone number if available
            String phone = currentUser.getPhoneNumber();
            tvPhone.setText(phone != null ? phone : getString(R.string.not_provided));
            
            // Load profile picture if available
            if (currentUser.getPhotoUrl() != null) {
                // Use a library like Glide or Picasso to load the image
                // For example with Glide:
                // Glide.with(this).load(currentUser.getPhotoUrl()).into(ivProfilePicture);
            }
            
            // Get detailed user information from Firestore
            firebaseHelper.getCurrentUserData(new FirebaseHelper.FirebaseCallback() {
                @Override
                public void onSuccess(Object result) {
                    if (result instanceof User) {
                        User user = (User) result;
                        String userName = user.getName();
                        if (userName != null && !userName.isEmpty()) {
                            tvUserName.setText(userName);
                            tvFullName.setText(userName);
                        } else {
                            // Fallback to display name from Auth if Firestore name is not available
                            String displayName = currentUser.getDisplayName();
                            tvUserName.setText(displayName != null ? displayName : getString(R.string.user));
                            tvFullName.setText(displayName != null ? displayName : getString(R.string.user));
                        }
                    } else {
                        // Fallback to display name from Auth if Firestore data is not available
                        String displayName = currentUser.getDisplayName();
                        tvUserName.setText(displayName != null ? displayName : getString(R.string.user));
                        tvFullName.setText(displayName != null ? displayName : getString(R.string.user));
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    // In case of failure, use the display name from Auth
                    String displayName = currentUser.getDisplayName();
                    tvUserName.setText(displayName != null ? displayName : getString(R.string.user));
                    tvFullName.setText(displayName != null ? displayName : getString(R.string.user));
                }
            });
        }
    }

    private void setupListeners() {
        // Edit Profile button
        btnEditProfile.setOnClickListener(v -> {
            // Navigate to edit profile screen
            Toast.makeText(ProfileActivity.this, "Edit Profile feature coming soon", Toast.LENGTH_SHORT).show();
        });
        
        // Logout button
        layoutLogout.setOnClickListener(v -> logout());
        
        // Dark Mode switch
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferenceManager.setDarkModeEnabled(isChecked);
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
        
        // Notifications switch
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferenceManager.setNotificationsEnabled(isChecked);
            String message = isChecked ? "Notifications enabled" : "Notifications disabled";
            Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_SHORT).show();
        });
        
        // Change Password
        findViewById(R.id.layoutChangePassword).setOnClickListener(v -> {
            Toast.makeText(ProfileActivity.this, "Change Password feature coming soon", Toast.LENGTH_SHORT).show();
        });
        
        // Privacy Policy
        findViewById(R.id.layoutPrivacyPolicy).setOnClickListener(v -> {
            Toast.makeText(ProfileActivity.this, "Privacy Policy feature coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void logout() {
        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Sign out from Firebase
                    firebaseAuth.signOut();
                    
                    // Clear preferences if needed
                    // preferenceManager.clearUserData();
                    
                    // Navigate to login screen
                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 