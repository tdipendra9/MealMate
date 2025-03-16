package edu.ismt.dipendra.mealmate.activities;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.adapters.RecipeAdapter;
import edu.ismt.dipendra.mealmate.firebase.FirebaseHelper;
import edu.ismt.dipendra.mealmate.model.User;
import edu.ismt.dipendra.mealmate.model.Meal;
import edu.ismt.dipendra.mealmate.model.MealPlanItem;
import edu.ismt.dipendra.mealmate.adapters.MealAdapter;

public class HomeActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener, SensorEventListener {

    private BottomNavigationView bottomNavigationView;
    private RecyclerView rvRecentRecipes;
    private TextView tvWelcome;
    private ImageView ivProfile;
    private MaterialCardView cardMealPlanning, cardGroceryList;
    
    // Today's meal plan views
    private TextView tvBreakfastMeal, tvLunchMeal, tvDinnerMeal;
    private ImageView ivBreakfastEdit, ivLunchEdit, ivDinnerEdit;
    
    private FirebaseAuth firebaseAuth;
    private FirebaseHelper firebaseHelper;
    
    // Shake detection
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private static final float SHAKE_THRESHOLD = 12.0f;
    private static final int MIN_TIME_BETWEEN_SHAKES = 1000; // in milliseconds
    private long lastShakeTime = 0;
    private float lastX = 0;
    private float lastY = 0;
    private float lastZ = 0;
    
    // Shared preferences for dark mode
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "MealMatePrefs";
    private static final String DARK_MODE_KEY = "dark_mode";
    
    // Tag for logging
    private static final String TAG = "HomeActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize shared preferences
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        
        // Apply dark mode setting
        boolean isDarkMode = sharedPreferences.getBoolean(DARK_MODE_KEY, false);
        AppCompatDelegate.setDefaultNightMode(isDarkMode ? 
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        
        setContentView(R.layout.activity_home);
        
        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseHelper = FirebaseHelper.getInstance();
        
        // Initialize shake detection
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        
        // Check if user is logged in
        checkUserLoggedIn();
        
        // Initialize views
        initViews();
        
        // Set up bottom navigation
        setupBottomNavigation();
        
        // Load user data
        loadUserData();
        
        // Load recent recipes
        loadRecentRecipes();
        
        // Load today's meal plan
        loadTodaysMealPlan();
        
        // Set up click listeners
        setupClickListeners();
        
        // Apply animations
        applyAnimations();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Register shake detector
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        
        // Refresh today's meal plan data
        loadTodaysMealPlan();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Unregister shake detector
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            
            float deltaX = Math.abs(lastX - x);
            float deltaY = Math.abs(lastY - y);
            float deltaZ = Math.abs(lastZ - z);
            
            // Check if the device was shaken hard enough
            if ((deltaX > SHAKE_THRESHOLD && deltaY > SHAKE_THRESHOLD) || 
                (deltaX > SHAKE_THRESHOLD && deltaZ > SHAKE_THRESHOLD) || 
                (deltaY > SHAKE_THRESHOLD && deltaZ > SHAKE_THRESHOLD)) {
                
                long currentTime = System.currentTimeMillis();
                
                // Check if enough time has passed since the last shake
                if (currentTime - lastShakeTime > MIN_TIME_BETWEEN_SHAKES) {
                    lastShakeTime = currentTime;
                    
                    // Toggle dark mode
                    toggleDarkMode();
                }
            }
            
            lastX = x;
            lastY = y;
            lastZ = z;
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for this implementation
    }
    
    /**
     * Toggle dark mode and save the setting to shared preferences
     */
    private void toggleDarkMode() {
        // Get current dark mode setting
        boolean isDarkMode = sharedPreferences.getBoolean(DARK_MODE_KEY, false);
        
        // Toggle dark mode
        isDarkMode = !isDarkMode;
        
        // Save the new setting
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(DARK_MODE_KEY, isDarkMode);
        editor.apply();
        
        // Apply the new setting
        AppCompatDelegate.setDefaultNightMode(isDarkMode ? 
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        
        // Show a toast message
        Toast.makeText(this, isDarkMode ? "Dark mode enabled" : "Dark mode disabled", Toast.LENGTH_SHORT).show();
        
        // Recreate the activity for a smoother transition
        recreate();
    }
    
    private void checkUserLoggedIn() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            // User is not logged in, redirect to login screen
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
    
    private void initViews() {
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        rvRecentRecipes = findViewById(R.id.rvRecentRecipes);
        tvWelcome = findViewById(R.id.tvWelcome);
        ivProfile = findViewById(R.id.ivProfile);
        cardMealPlanning = findViewById(R.id.cardMealPlanning);
        cardGroceryList = findViewById(R.id.cardGroceryList);
        
        // Today's meal plan views
        tvBreakfastMeal = findViewById(R.id.tvBreakfastMeal);
        tvLunchMeal = findViewById(R.id.tvLunchMeal);
        tvDinnerMeal = findViewById(R.id.tvDinnerMeal);
        ivBreakfastEdit = findViewById(R.id.ivBreakfastEdit);
        ivLunchEdit = findViewById(R.id.ivLunchEdit);
        ivDinnerEdit = findViewById(R.id.ivDinnerEdit);
    }
    
    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(this);
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
    }
    
    private void loadUserData() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            // First, try to get the display name from Firebase Auth
            String displayName = currentUser.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                tvWelcome.setText(getString(R.string.welcome_message, displayName));
            } else {
                // If display name is not available in Auth, try to get it from Firestore
                firebaseHelper.getCurrentUserData(new FirebaseHelper.FirebaseCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        if (result instanceof User) {
                            User user = (User) result;
                            String userName = user.getName();
                            if (userName != null && !userName.isEmpty()) {
                                tvWelcome.setText(getString(R.string.welcome_message, userName));
                            } else {
                                tvWelcome.setText(getString(R.string.welcome_message, "User"));
                            }
                        } else {
                            tvWelcome.setText(getString(R.string.welcome_message, "User"));
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // In case of failure, use a default name
                        tvWelcome.setText(getString(R.string.welcome_message, "User"));
                    }
                });
            }
        }
    }
    
    private void loadRecentRecipes() {
        // Use FirebaseHelper to get recent recipes from Firestore
        firebaseHelper.getRecentRecipes(5, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof List) {
                    List<?> items = (List<?>) result;
                    List<Meal> meals = new ArrayList<>();
                    
                    for (Object item : items) {
                        if (item instanceof Meal) {
                            meals.add((Meal) item);
                        }
                    }
                    
                    if (meals.isEmpty()) {
                        // If no recipes found, show a message
                        findViewById(R.id.tvNoRecipes).setVisibility(View.VISIBLE);
                        rvRecentRecipes.setVisibility(View.GONE);
                    } else {
                        // If recipes found, show them in the RecyclerView
                        findViewById(R.id.tvNoRecipes).setVisibility(View.GONE);
                        rvRecentRecipes.setVisibility(View.VISIBLE);
                        
                        // Create adapter with the meals
                        RecentMealAdapter adapter = new RecentMealAdapter(HomeActivity.this, meals);
                        
                        // Set up the RecyclerView
                        rvRecentRecipes.setAdapter(adapter);
                        rvRecentRecipes.setLayoutManager(new LinearLayoutManager(HomeActivity.this, LinearLayoutManager.HORIZONTAL, false));
                    }
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                // In case of failure, show a message
                findViewById(R.id.tvNoRecipes).setVisibility(View.VISIBLE);
                rvRecentRecipes.setVisibility(View.GONE);
                Log.e("HomeActivity", "Failed to load recent recipes", e);
            }
        });
    }
    
    /**
     * Simple adapter for recent recipes in the home screen
     */
    private class RecentMealAdapter extends RecyclerView.Adapter<RecentMealAdapter.ViewHolder> {
        private Context context;
        private List<Meal> meals;
        
        public RecentMealAdapter(Context context, List<Meal> meals) {
            this.context = context;
            this.meals = meals;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_recipe, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Meal meal = meals.get(position);
            
            holder.tvRecipeName.setText(meal.getName());
            holder.tvRecipeCategory.setText(meal.getCategory());
            
            // Format cooking time with null check
            try {
                String cookingTime = meal.getCookingTime() + " min";
                holder.tvRecipeCookTime.setText(cookingTime);
            } catch (Exception e) {
                // Default value if cooking time is not available
                holder.tvRecipeCookTime.setText("-- min");
            }
            
            // Load image if available
            if (meal.getImageUrl() != null && !meal.getImageUrl().isEmpty()) {
                Glide.with(context)
                    .load(meal.getImageUrl())
                    .apply(new RequestOptions().centerCrop())
                    .into(holder.ivRecipeImage);
            } else {
                // Set default image
                holder.ivRecipeImage.setImageResource(R.drawable.ic_recipe);
            }
            
            // Set click listener
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, MealDetailActivity.class);
                intent.putExtra("meal_id", meal.getId());
                intent.putExtra("meal_object", meal);
                startActivity(intent);
                // Add entry animation
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }
        
        @Override
        public int getItemCount() {
            return meals.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivRecipeImage;
            TextView tvRecipeName, tvRecipeCategory, tvRecipeCookTime;
            
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivRecipeImage = itemView.findViewById(R.id.ivRecipeImage);
                tvRecipeName = itemView.findViewById(R.id.tvRecipeName);
                tvRecipeCategory = itemView.findViewById(R.id.tvRecipeCategory);
                tvRecipeCookTime = itemView.findViewById(R.id.tvRecipeCookTime);
            }
        }
    }
    
    /**
     * Load today's meal plan from Firebase
     */
    private void loadTodaysMealPlan() {
        // Get today's date
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date today = calendar.getTime();
        
        // Use FirebaseHelper to get meal plans for today
        firebaseHelper.getMealPlanItemsByDate(today, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof List) {
                    List<?> items = (List<?>) result;
                    
                    // Default text for when no meal is planned
                    String noMealText = getString(R.string.no_meal_planned);
                    
                    // Set default text for all meal types
                    tvBreakfastMeal.setText(noMealText);
                    tvLunchMeal.setText(noMealText);
                    tvDinnerMeal.setText(noMealText);
                    
                    // Process the meal plan items
                    for (Object item : items) {
                        if (item instanceof MealPlanItem) {
                            MealPlanItem mealPlan = (MealPlanItem) item;
                            String mealType = mealPlan.getMealType();
                            String mealName = mealPlan.getMealName();
                            
                            if (mealType != null && mealName != null) {
                                switch (mealType.toLowerCase()) {
                                    case "breakfast":
                                        tvBreakfastMeal.setText(mealName);
                                        break;
                                    case "lunch":
                                        tvLunchMeal.setText(mealName);
                                        break;
                                    case "dinner":
                                        tvDinnerMeal.setText(mealName);
                                        break;
                                }
                            }
                        }
                    }
                    
                    Log.d(TAG, "Loaded today's meal plan successfully");
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load today's meal plan", e);
                
                // Set default text for all meal types
                String noMealText = getString(R.string.no_meal_planned);
                tvBreakfastMeal.setText(noMealText);
                tvLunchMeal.setText(noMealText);
                tvDinnerMeal.setText(noMealText);
            }
        });
    }
    
    private void setupClickListeners() {
        // Profile icon click
        ivProfile.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this, ivProfile, "profileTransition");
            startActivity(intent, options.toBundle());
        });
        
        // Meal Plan button
        cardMealPlanning.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, MealPlanHomeActivity.class);
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this, cardMealPlanning, "cardTransition");
            startActivity(intent, options.toBundle());
        });
        
        // Grocery List button
        cardGroceryList.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, GroceryListActivity.class);
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this, cardGroceryList, "cardTransition");
            startActivity(intent, options.toBundle());
        });
        
        // View All Recent Recipes
        findViewById(R.id.tvViewAllRecipes).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, MealListActivity.class);
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this, rvRecentRecipes, "recipeListTransition");
            startActivity(intent, options.toBundle());
        });
        
        // Explore Recipes button
        findViewById(R.id.btnExploreRecipes).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, MealListActivity.class);
            View btnExploreRecipes = findViewById(R.id.btnExploreRecipes);
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this, btnExploreRecipes, "buttonTransition");
            startActivity(intent, options.toBundle());
        });
        
        // Meal plan edit buttons
        setupMealPlanEditButtons();
    }
    
    /**
     * Set up click listeners for meal plan edit buttons
     */
    private void setupMealPlanEditButtons() {
        // Get today's date for the intent
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date today = calendar.getTime();
        
        // Breakfast edit button
        ivBreakfastEdit.setOnClickListener(v -> {
            navigateToMealPlanWithType("breakfast");
        });
        
        // Lunch edit button
        ivLunchEdit.setOnClickListener(v -> {
            navigateToMealPlanWithType("lunch");
        });
        
        // Dinner edit button
        ivDinnerEdit.setOnClickListener(v -> {
            navigateToMealPlanWithType("dinner");
        });
    }
    
    /**
     * Navigate to MealPlanHomeActivity with the specified meal type
     * @param mealType The meal type to edit (breakfast, lunch, dinner)
     */
    private void navigateToMealPlanWithType(String mealType) {
        Intent intent = new Intent(HomeActivity.this, MealPlanHomeActivity.class);
        intent.putExtra("meal_type", mealType);
        startActivity(intent);
    }
    
    private void applyAnimations() {
        // Apply animations to views
        // Get the welcome card directly instead of using parent references
        MaterialCardView welcomeCard = findViewById(R.id.tvWelcome).getRootView().findViewById(R.id.welcomeCard);
        if (welcomeCard != null) {
            welcomeCard.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
        } else {
            // Fallback to animating just the welcome text
            findViewById(R.id.tvWelcome).startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
        }
        
        cardMealPlanning.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));
        cardGroceryList.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));
        
        rvRecentRecipes.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
    }
    
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == R.id.navigation_home) {
            // Already on home screen
            return true;
        } else if (itemId == R.id.navigation_recipe) {
            Intent intent = new Intent(this, MealListActivity.class);
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(this);
            startActivity(intent, options.toBundle());
            return true;
        } else if (itemId == R.id.navigation_meal_plan) {
            Intent intent = new Intent(this, MealPlanHomeActivity.class);
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(this);
            startActivity(intent, options.toBundle());
            return true;
        } else if (itemId == R.id.navigation_grocery) {
            Intent intent = new Intent(this, GroceryListActivity.class);
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(this);
            startActivity(intent, options.toBundle());
            return true;
        } else if (itemId == R.id.navigation_profile) {
            Intent intent = new Intent(this, ProfileActivity.class);
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this, ivProfile, "profileTransition");
            startActivity(intent, options.toBundle());
            return true;
        }
        
        return false;
    }

    @Override
    public void onBackPressed() {
        // Show exit confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Exit Application")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Exit the app
                    finishAffinity();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Dismiss the dialog and stay in the app
                    dialog.dismiss();
                })
                .show();
    }
} 