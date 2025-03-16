package edu.ismt.dipendra.mealmate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import edu.ismt.dipendra.mealmate.R;

/**
 * Splash screen activity that displays the app logo and name
 * before redirecting to the appropriate screen.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // 2 seconds
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Set up animations
        setupAnimations();

        // Delay and then navigate to the next screen
        new Handler(Looper.getMainLooper()).postDelayed(this::checkUserAndNavigate, SPLASH_DURATION);
    }

    /**
     * Set up animations for the splash screen elements
     */
    private void setupAnimations() {
        ImageView ivLogo = findViewById(R.id.imageViewLogo);
        TextView tvAppName = findViewById(R.id.textViewAppName);
        TextView tvTagline = findViewById(R.id.textViewTagline);

        // Logo animation - rotate and fade in
        Animation rotate = AnimationUtils.loadAnimation(this, R.anim.rotate);
        rotate.setDuration(1000);
        ivLogo.startAnimation(rotate);

        // Text animations - fade in and slide up
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fadeIn.setDuration(1000);

        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        slideUp.setDuration(1000);

        tvAppName.startAnimation(slideUp);
        tvTagline.startAnimation(slideUp);
    }

    /**
     * Check if user is logged in and navigate to the appropriate screen
     */
    private void checkUserAndNavigate() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        Intent intent;
        if (currentUser != null) {
            // User is logged in, go to Home Activity
            intent = new Intent(SplashActivity.this, HomeActivity.class);
        } else {
            // User is not logged in, go to Login Activity
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        startActivity(intent);
        finish(); // Close the splash activity so it's not in the back stack
    }
} 