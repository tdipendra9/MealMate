package edu.ismt.dipendra.mealmate.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.adapters.MealAdapter;
import edu.ismt.dipendra.mealmate.firebase.FirebaseHelper;
import edu.ismt.dipendra.mealmate.model.Meal;
import edu.ismt.dipendra.mealmate.utils.ReminderManager;

public class MealListActivity extends AppCompatActivity implements MealAdapter.OnMealClickListener {

    private static final int REQUEST_ADD_MEAL = 1;
    private static final int REQUEST_EDIT_MEAL = 2;
    private static final String TAG = "MealListActivity";

    private MaterialToolbar toolbar;
    private RecyclerView recyclerViewMeals;
    private FloatingActionButton fabAddMeal;
    private CircularProgressIndicator progressIndicator;
    private TextView textViewEmpty;

    private FirebaseHelper firebaseHelper;
    private MealAdapter mealAdapter;
    private List<Meal> mealList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_list);

        initViews();
        setupToolbar();
        initObjects();
        setupRecyclerView();
        loadMeals();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerViewMeals = findViewById(R.id.recyclerViewMeals);
        fabAddMeal = findViewById(R.id.fabAddMeal);
        progressIndicator = findViewById(R.id.progressIndicator);
        textViewEmpty = findViewById(R.id.textViewEmpty);

        fabAddMeal.setOnClickListener(v -> {
            Intent intent = new Intent(MealListActivity.this, MealFormActivity.class);
            startActivityForResult(intent, REQUEST_ADD_MEAL);
        });
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
    }

    private void initObjects() {
        firebaseHelper = FirebaseHelper.getInstance();
        mealList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        recyclerViewMeals.setLayoutManager(new LinearLayoutManager(this));
        mealAdapter = new MealAdapter(this, mealList, this);
        recyclerViewMeals.setAdapter(mealAdapter);
    }

    private void loadMeals() {
        progressIndicator.setVisibility(View.VISIBLE);
        textViewEmpty.setVisibility(View.GONE);
        recyclerViewMeals.setVisibility(View.GONE); // Hide recycler view until we have data

        Log.d(TAG, "Loading meals from Firebase recipe collection...");
        
        // Use the new getRecipes method to load meals from the recipe collection
        firebaseHelper.getRecipes(new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                progressIndicator.setVisibility(View.GONE);
                
                if (result instanceof List) {
                    List<Meal> newMeals = (List<Meal>) result;
                    Log.d(TAG, "Successfully received " + newMeals.size() + " meals from Firebase recipe collection");
                    
                    // Update the class field
                    mealList.clear();
                    mealList.addAll(newMeals);
                    
                    // Update the adapter with the new meals
                    mealAdapter.updateMeals(mealList);
                    
                    // Update visibility based on whether there are meals
                    if (mealList.isEmpty()) {
                        Log.d(TAG, "No meals found, showing empty view");
                        textViewEmpty.setVisibility(View.VISIBLE);
                        recyclerViewMeals.setVisibility(View.GONE);
                    } else {
                        Log.d(TAG, "Meals found, showing recycler view");
                        textViewEmpty.setVisibility(View.GONE);
                        recyclerViewMeals.setVisibility(View.VISIBLE);
                    }
                    
                    // Log the meals for debugging
                    for (Meal meal : mealList) {
                        Log.d(TAG, "Meal: " + meal.getId() + " - " + meal.getName() + 
                                " - Category: " + meal.getCategory() + 
                                " - Ingredients: " + (meal.getIngredients() != null ? meal.getIngredients().size() : "null"));
                    }
                } else {
                    Log.e(TAG, "Result is not a list of meals: " + (result != null ? result.getClass().getName() : "null"));
                    textViewEmpty.setVisibility(View.VISIBLE);
                    recyclerViewMeals.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressIndicator.setVisibility(View.GONE);
                Log.e(TAG, "Failed to load meals", e);
                textViewEmpty.setVisibility(View.VISIBLE);
                recyclerViewMeals.setVisibility(View.GONE);
                
                // Check if the error is related to Firestore indexes
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("FAILED_PRECONDITION") && 
                    errorMessage.contains("index")) {
                    // This is an index error
                    Toast.makeText(MealListActivity.this, 
                            "Database index issue. Please contact the developer.", 
                            Toast.LENGTH_LONG).show();
                    
                    // Log the full error with the index creation URL
                    Log.e(TAG, "Firestore index required: " + errorMessage);
                } else {
                    // Generic error
                    Toast.makeText(MealListActivity.this, 
                            getString(R.string.error_load_meals, e.getMessage()), 
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onEditClick(Meal meal) {
        Intent intent = new Intent(MealListActivity.this, MealFormActivity.class);
        intent.putExtra("meal_id", meal.getId());
        intent.putExtra("meal_name", meal.getName());
        intent.putExtra("meal_description", meal.getDescription());
        intent.putExtra("meal_instructions", meal.getInstructions());
        intent.putExtra("meal_preparation_time", meal.getPreparationTime());
        intent.putExtra("meal_cooking_time", meal.getCookingTime());
        intent.putExtra("meal_servings", meal.getServings());
        intent.putExtra("meal_category", meal.getCategory());
        intent.putExtra("meal_image_url", meal.getImageUrl());
        
        // Pass the ingredients as a string array
        ArrayList<String> ingredients = new ArrayList<>(meal.getIngredients());
        intent.putStringArrayListExtra("meal_ingredients", ingredients);
        
        // Pass the creation date if available
        if (meal.getCreatedAt() != null) {
            intent.putExtra("meal_created_at", meal.getCreatedAt().getTime());
        }
        
        startActivityForResult(intent, REQUEST_EDIT_MEAL);
    }

    @Override
    public void onDeleteClick(Meal meal) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_meal)
                .setMessage(getString(R.string.confirm_delete_meal, meal.getName()))
                .setPositiveButton(android.R.string.yes, (dialog, which) -> deleteMeal(meal))
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    @Override
    public void onSetReminderClick(Meal meal) {
        ReminderManager.showSetReminderDialog(this, meal);
    }

    @Override
    public void onMealClick(Meal meal) {
        // Add debug logging
        Log.d(TAG, "onMealClick: Meal ID = " + meal.getId() + ", Name = " + meal.getName());
        
        if (meal.getId() == null || meal.getId().isEmpty()) {
            Log.e(TAG, "onMealClick: Meal ID is null or empty!");
            Toast.makeText(this, "Error: Meal ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Navigate to MealDetailActivity with just the meal ID
        // This is more reliable than passing the entire object
        Intent intent = new Intent(MealListActivity.this, MealDetailActivity.class);
        intent.putExtra("meal_id", meal.getId());
        Log.d(TAG, "onMealClick: Starting MealDetailActivity with meal ID: " + meal.getId());
        startActivity(intent);
        // Add entry animation
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void deleteMeal(Meal meal) {
        progressIndicator.setVisibility(View.VISIBLE);
        
        // Use the deleteMealFromRecipe method to delete from the recipe collection
        firebaseHelper.deleteMealFromRecipe(meal.getId(), new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                progressIndicator.setVisibility(View.GONE);
                Toast.makeText(MealListActivity.this, R.string.meal_deleted, Toast.LENGTH_SHORT).show();
                // No need to reload meals if using a real-time listener
                loadMeals(); // For now, we'll reload manually
            }

            @Override
            public void onFailure(Exception e) {
                progressIndicator.setVisibility(View.GONE);
                Toast.makeText(MealListActivity.this, 
                        getString(R.string.error_delete_meal, e.getMessage()), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        
        if (resultCode == RESULT_OK) {
            if (data != null) {
                // Get the meal ID from the result
                String mealId = data.getStringExtra("meal_id");
                boolean refreshMeals = data.getBooleanExtra("refresh_meals", false);
                
                if (mealId != null && !mealId.isEmpty()) {
                    Log.d(TAG, "Received meal ID: " + mealId);
                }
                
                if (refreshMeals) {
                    Log.d(TAG, "Refresh flag received, forcing data reload from Firebase");
                }
                
                // Show a success message based on the request code
                if (requestCode == REQUEST_ADD_MEAL) {
                    Toast.makeText(this, R.string.meal_added, Toast.LENGTH_SHORT).show();
                } else if (requestCode == REQUEST_EDIT_MEAL) {
                    Toast.makeText(this, R.string.meal_updated, Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.w(TAG, "Received RESULT_OK but data is null");
            }
            
            // Show loading indicator
            progressIndicator.setVisibility(View.VISIBLE);
            
            // Make sure the recyclerView is visible
            recyclerViewMeals.setVisibility(View.VISIBLE);
            textViewEmpty.setVisibility(View.GONE);
            
            // Force a refresh of the list
            loadMeals();
        } else if (resultCode == RESULT_CANCELED) {
            Log.d(TAG, "Operation was canceled by the user");
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Refreshing meal list");
        loadMeals();
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