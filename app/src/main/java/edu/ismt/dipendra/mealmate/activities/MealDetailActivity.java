package edu.ismt.dipendra.mealmate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.adapters.MealDetailPagerAdapter;
import edu.ismt.dipendra.mealmate.firebase.FirebaseHelper;
import edu.ismt.dipendra.mealmate.model.Meal;
import edu.ismt.dipendra.mealmate.utils.ReminderManager;

public class MealDetailActivity extends AppCompatActivity implements MealDetailPagerAdapter.OnMealDetailActionListener {

    private static final String TAG = "MealDetailActivity";

    private Toolbar toolbar;
    private ViewPager2 viewPager;
    private ProgressBar progressBar;

    private FirebaseHelper firebaseHelper;
    private String mealId;
    private Meal currentMeal;
    private MealDetailPagerAdapter pagerAdapter;
    private List<Meal> meals = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_detail);

        // Get meal ID from intent
        if (getIntent().hasExtra("meal_id")) {
            mealId = getIntent().getStringExtra("meal_id");
            Log.d(TAG, "onCreate: Received meal ID: " + mealId);
        } else {
            Log.e(TAG, "onCreate: No meal ID in intent");
            Toast.makeText(this, "Meal ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        
        firebaseHelper = FirebaseHelper.getInstance();
        
        // Initialize the ViewPager adapter
        pagerAdapter = new MealDetailPagerAdapter(this, this);
        viewPager.setAdapter(pagerAdapter);
        
        // Set up custom animations for page transitions
        viewPager.setPageTransformer(new ViewPager2.PageTransformer() {
            @Override
            public void transformPage(@NonNull View page, float position) {
                if (position < -1) { // Page is far off-screen to the left
                    page.setAlpha(0f);
                } else if (position <= 1) { // Page is visible or partially visible
                    // Adjust the page's alpha based on position
                    page.setAlpha(1.0f - Math.abs(position) * 0.5f);
                    
                    // Apply a scale effect
                    float scaleFactor = Math.max(0.85f, 1 - Math.abs(position) * 0.2f);
                    page.setScaleX(scaleFactor);
                    page.setScaleY(scaleFactor);
                    
                    // Apply translation effect
                    page.setTranslationX(-position * page.getWidth() / 2);
                } else { // Page is far off-screen to the right
                    page.setAlpha(0f);
                }
            }
        });
        
        // Add page change listener to update the title
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentMeal = pagerAdapter.getMealAt(position);
                if (currentMeal != null) {
                    setTitle(currentMeal.getName());
                }
            }
        });
        
        // Show loading indicator while we get the data
        showLoading(true);
        
        // Check if the meal object was passed directly
        if (getIntent().hasExtra("meal_object")) {
            Log.d(TAG, "onCreate: Intent has meal_object extra");
            try {
                currentMeal = (Meal) getIntent().getSerializableExtra("meal_object");
                if (currentMeal != null) {
                    Log.d(TAG, "onCreate: Successfully retrieved meal object: ID = " + currentMeal.getId() + ", Name = " + currentMeal.getName());
                    // Use the passed meal object directly
                    showLoading(false);
                    addMealAndLoadMore(currentMeal);
                } else {
                    Log.e(TAG, "onCreate: meal_object extra is null after deserialization");
                    // Fallback to loading from Firebase
                    loadMealDetails();
                }
            } catch (Exception e) {
                Log.e(TAG, "onCreate: Error deserializing meal object", e);
                // Fallback to loading from Firebase
                loadMealDetails();
            }
        } else {
            Log.d(TAG, "onCreate: No meal_object in intent, loading from Firebase");
            // No meal object passed, load from Firebase
            loadMealDetails();
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        viewPager = findViewById(R.id.viewPager);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(""); // Title will be set when meal is loaded
        }
    }

    private void loadMealDetails() {
        Log.d(TAG, "loadMealDetails: Loading meal details for ID: " + mealId);
        showLoading(true);
        
        // Use the new method to get a meal from the recipe collection
        firebaseHelper.getMealFromRecipe(mealId, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "loadMealDetails onSuccess: Received result of type: " + (result != null ? result.getClass().getName() : "null"));
                showLoading(false);
                
                if (result instanceof Meal) {
                    currentMeal = (Meal) result;
                    Log.d(TAG, "loadMealDetails onSuccess: Successfully loaded meal: ID = " + currentMeal.getId() + ", Name = " + currentMeal.getName());
                    addMealAndLoadMore(currentMeal);
                } else {
                    Log.e(TAG, "loadMealDetails onSuccess: Result is not a Meal object");
                    Toast.makeText(MealDetailActivity.this, "Error: Invalid meal data", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "loadMealDetails onFailure: Failed to load meal", e);
                showLoading(false);
                Toast.makeText(MealDetailActivity.this, 
                        getString(R.string.error_load_meal, e.getMessage()), 
                        Toast.LENGTH_SHORT).show();
                
                // Try one more time with a direct Firestore query
                Log.d(TAG, "loadMealDetails: Trying direct Firestore query as fallback");
                firebaseHelper.getMealById(mealId, new FirebaseHelper.FirebaseCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        if (result instanceof Meal) {
                            currentMeal = (Meal) result;
                            Log.d(TAG, "loadMealDetails fallback onSuccess: Successfully loaded meal: ID = " + currentMeal.getId() + ", Name = " + currentMeal.getName());
                            addMealAndLoadMore(currentMeal);
                        } else {
                            Log.e(TAG, "loadMealDetails fallback onSuccess: Result is not a Meal object");
                            Toast.makeText(MealDetailActivity.this, "Error: Invalid meal data", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "loadMealDetails fallback onFailure: Failed to load meal", e);
                        Toast.makeText(MealDetailActivity.this, 
                                "Could not load meal details after multiple attempts", 
                                Toast.LENGTH_SHORT).show();
                        finish(); // Close the activity if we can't load the meal
                    }
                });
            }
        });
    }
    
    private void addMealAndLoadMore(Meal meal) {
        // Add the current meal to the adapter
        pagerAdapter.addMeal(meal);
        
        // Set the title to the current meal name
        setTitle(meal.getName());
        
        // Load more meals for swiping
        loadMoreMeals();
    }
    
    private void loadMoreMeals() {
        // Load more meals for swiping
        firebaseHelper.getRecipes(new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof List) {
                    List<Meal> allMeals = (List<Meal>) result;
                    
                    // Filter out the current meal as it's already added
                    for (Meal meal : allMeals) {
                        if (meal.getId() != null && !meal.getId().equals(mealId)) {
                            pagerAdapter.addMeal(meal);
                        }
                    }
                    
                    // Set the current meal as the initial item
                    int position = pagerAdapter.getPositionForMealId(mealId);
                    if (position >= 0) {
                        viewPager.setCurrentItem(position, false);
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load additional meals", e);
                // Not critical, so we don't show an error message
            }
        });
    }

    private void showDeleteConfirmationDialog(Meal meal) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_meal)
                .setMessage(getString(R.string.confirm_delete_meal, meal.getName()))
                .setPositiveButton(R.string.yes, (dialog, which) -> deleteMeal(meal))
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void deleteMeal(Meal meal) {
        showLoading(true);
        
        // Use the new method to delete from the recipe collection
        firebaseHelper.deleteMealFromRecipe(meal.getId(), new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                showLoading(false);
                Toast.makeText(MealDetailActivity.this, 
                        R.string.meal_deleted, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                Toast.makeText(MealDetailActivity.this, 
                        getString(R.string.error_delete_meal, e.getMessage()), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void shareMeal(Meal meal) {
        if (meal == null) return;
        
        StringBuilder shareText = new StringBuilder();
        shareText.append(meal.getName()).append("\n\n");
        
        if (meal.getDescription() != null && !meal.getDescription().isEmpty()) {
            shareText.append(meal.getDescription()).append("\n\n");
        }
        
        // Add ingredients
        shareText.append("Ingredients:\n");
        if (meal.getIngredients() != null && !meal.getIngredients().isEmpty()) {
            for (String ingredient : meal.getIngredients()) {
                shareText.append("• ").append(ingredient).append("\n");
            }
        }
        shareText.append("\n");
        
        // Add instructions
        shareText.append("Instructions:\n");
        if (meal.getInstructions() != null && !meal.getInstructions().isEmpty()) {
            shareText.append(meal.getInstructions()).append("\n\n");
        }
        
        // Add preparation info
        shareText.append("Preparation Time: ").append(meal.getPreparationTime()).append(" minutes\n");
        shareText.append("Cooking Time: ").append(meal.getCookingTime()).append(" minutes\n");
        shareText.append("Servings: ").append(meal.getServings()).append("\n\n");
        
        // Add source if available
        if (meal.isImported() && meal.getSourceUrl() != null && !meal.getSourceUrl().isEmpty()) {
            shareText.append("Source: ").append(meal.getSourceUrl()).append("\n");
        }
        
        shareText.append("Shared from MealMate App");
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Recipe: " + meal.getName());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
        startActivity(Intent.createChooser(shareIntent, "Share Recipe"));
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Only reload if we have a meal ID but no meal object
        // This prevents unnecessary Firebase calls when returning to this activity
        if (mealId != null && currentMeal == null) {
            loadMealDetails();
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Removed duplicate transition - will be handled in finish()
    }

    @Override
    public void finish() {
        super.finish();
        // Apply exit animation when finishing
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    // MealDetailPagerAdapter.OnMealDetailActionListener implementation
    @Override
    public void onEditClick(Meal meal) {
        Intent intent = new Intent(MealDetailActivity.this, MealFormActivity.class);
        intent.putExtra("meal_id", meal.getId());
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Meal meal) {
        showDeleteConfirmationDialog(meal);
    }

    @Override
    public void onSetReminderClick(Meal meal) {
        ReminderManager.showSetReminderDialog(this, meal);
    }

    @Override
    public void onShareClick(Meal meal) {
        shareMeal(meal);
    }
} 