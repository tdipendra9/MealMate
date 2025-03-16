package edu.ismt.dipendra.mealmate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.adapters.RecipeAdapter;
import edu.ismt.dipendra.mealmate.firebase.FirebaseHelper;
import edu.ismt.dipendra.mealmate.models.Recipe;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private BottomNavigationView bottomNavigationView;
    private RecyclerView rvRecentRecipes;
    private TextView tvWelcome;
    private ImageView ivProfile;
    private MaterialCardView cardMealPlanning, cardGroceryList;
    
    private FirebaseAuth firebaseAuth;
    private FirebaseHelper firebaseHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        
        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseHelper = FirebaseHelper.getInstance();
        
        // Check if user is logged in
        checkUserLoggedIn();
        
        // Find views
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        rvRecentRecipes = findViewById(R.id.rvRecentRecipes);
        tvWelcome = findViewById(R.id.tvWelcome);
        ivProfile = findViewById(R.id.ivProfile);
        cardMealPlanning = findViewById(R.id.cardMealPlanning);
        cardGroceryList = findViewById(R.id.cardGroceryList);
        
        // Set up bottom navigation
        setupBottomNavigation();
        
        // Load user data
        loadUserData();
        
        // Load recent recipes
        loadRecentRecipes();
        
        // Set up click listeners
        setupClickListeners();
    }
    
    private void checkUserLoggedIn() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            // User is not logged in, redirect to login screen
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
    
    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(this);
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
    }
    
    private void loadUserData() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            String displayName = currentUser.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                tvWelcome.setText(getString(R.string.welcome_message, displayName));
            } else {
                tvWelcome.setText(getString(R.string.welcome_message, "User"));
            }
        }
    }
    
    private void loadRecentRecipes() {
        // Sample recipes - in a real app, these would come from a database or API
        List<Recipe> recipes = new ArrayList<>();
        recipes.add(new Recipe("Pancakes", "Breakfast", "30 min", R.drawable.ic_recipe));
        recipes.add(new Recipe("Pasta Carbonara", "Lunch", "45 min", R.drawable.ic_recipe));
        recipes.add(new Recipe("Chicken Curry", "Dinner", "60 min", R.drawable.ic_recipe));
        
        // Set up the RecyclerView
        RecipeAdapter adapter = new RecipeAdapter(this, recipes);
        rvRecentRecipes.setAdapter(adapter);
        rvRecentRecipes.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }
    
    private void setupClickListeners() {
        // Profile icon click
        ivProfile.setOnClickListener(v -> {
            try {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            } catch (Exception e) {
                Log.e("MainActivity", "Error navigating to ProfileActivity: " + e.getMessage());
                Toast.makeText(this, "Unable to open Profile", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Meal Plan button
        cardMealPlanning.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(MainActivity.this, MealPlanHomeActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e("MainActivity", "Error navigating to MealPlanHomeActivity: " + e.getMessage());
                Toast.makeText(this, "Unable to open Meal Plan", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Grocery List button
        cardGroceryList.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(MainActivity.this, GroceryListActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e("MainActivity", "Error navigating to GroceryListActivity: " + e.getMessage());
                Toast.makeText(this, "Unable to open Grocery List", Toast.LENGTH_SHORT).show();
            }
        });
        
        // View All Recent Recipes
        findViewById(R.id.tvViewAllRecipes).setOnClickListener(v -> {
            try {
                startActivity(new Intent(MainActivity.this, MealListActivity.class));
            } catch (Exception e) {
                Log.e("MainActivity", "Error navigating to MealListActivity: " + e.getMessage());
                Toast.makeText(this, "Unable to open Recipes", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Explore Recipes button
        findViewById(R.id.btnExploreRecipes).setOnClickListener(v -> {
            try {
                startActivity(new Intent(MainActivity.this, MealListActivity.class));
            } catch (Exception e) {
                Log.e("MainActivity", "Error navigating to MealListActivity: " + e.getMessage());
                Toast.makeText(this, "Unable to open Recipes", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        
        try {
            if (itemId == R.id.navigation_home) {
                // Already on home screen
                return true;
            } else if (itemId == R.id.navigation_recipe) {
                startActivity(new Intent(this, MealListActivity.class));
                return true;
            } else if (itemId == R.id.navigation_meal_plan) {
                startActivity(new Intent(this, MealPlanHomeActivity.class));
                return true;
            } else if (itemId == R.id.navigation_grocery) {
                startActivity(new Intent(this, GroceryListActivity.class));
                return true;
            } else if (itemId == R.id.navigation_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error in navigation: " + e.getMessage());
            Toast.makeText(this, "Navigation error", Toast.LENGTH_SHORT).show();
        }
        
        return false;
    }
} 