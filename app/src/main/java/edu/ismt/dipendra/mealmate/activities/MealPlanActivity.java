package edu.ismt.dipendra.mealmate.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.firebase.FirebaseHelper;
import edu.ismt.dipendra.mealmate.model.Meal;
import edu.ismt.dipendra.mealmate.model.MealPlan;

public class MealPlanActivity extends AppCompatActivity {

    private static final String TAG = "MealPlanActivity";
    private static final String BREAKFAST = "breakfast";
    private static final String LUNCH = "lunch";
    private static final String DINNER = "dinner";
    private static final String SNACKS = "snacks";

    private Toolbar toolbar;
    private TextView tvSelectedDate;
    private Button btnSelectDate;
    private MaterialCardView cardBreakfast, cardLunch, cardDinner, cardSnacks;
    private TextView tvBreakfastMeal, tvLunchMeal, tvDinnerMeal, tvSnacksMeal;
    private TextView tvBreakfastCalories, tvLunchCalories, tvDinnerCalories, tvSnacksCalories;
    private TextView tvNoMealPlan;
    private ProgressBar progressBar;
    private FloatingActionButton fabAddMealPlan;

    private FirebaseHelper firebaseHelper;
    private Calendar selectedDate;
    private MealPlan currentMealPlan;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_plan);

        initViews();
        setupToolbar();
        
        firebaseHelper = FirebaseHelper.getInstance();
        dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
        
        // Initialize with current date
        selectedDate = Calendar.getInstance();
        updateDateDisplay();
        
        setupListeners();
        loadMealPlan();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        
        cardBreakfast = findViewById(R.id.cardBreakfast);
        cardLunch = findViewById(R.id.cardLunch);
        cardDinner = findViewById(R.id.cardDinner);
        cardSnacks = findViewById(R.id.cardSnacks);
        
        tvBreakfastMeal = findViewById(R.id.tvBreakfastMeal);
        tvLunchMeal = findViewById(R.id.tvLunchMeal);
        tvDinnerMeal = findViewById(R.id.tvDinnerMeal);
        tvSnacksMeal = findViewById(R.id.tvSnacksMeal);
        
        tvBreakfastCalories = findViewById(R.id.tvBreakfastCalories);
        tvLunchCalories = findViewById(R.id.tvLunchCalories);
        tvDinnerCalories = findViewById(R.id.tvDinnerCalories);
        tvSnacksCalories = findViewById(R.id.tvSnacksCalories);
        
        tvNoMealPlan = findViewById(R.id.tvNoMealPlan);
        progressBar = findViewById(R.id.progressBar);
        fabAddMealPlan = findViewById(R.id.fabAddMealPlan);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.meal_plan);
        }
    }

    private void setupListeners() {
        btnSelectDate.setOnClickListener(v -> showDatePicker());
        
        fabAddMealPlan.setOnClickListener(v -> showAddMealDialog());
        
        cardBreakfast.setOnClickListener(v -> handleCardClick(BREAKFAST));
        cardLunch.setOnClickListener(v -> handleCardClick(LUNCH));
        cardDinner.setOnClickListener(v -> handleCardClick(DINNER));
        cardSnacks.setOnClickListener(v -> handleCardClick(SNACKS));
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    
                    updateDateDisplay();
                    loadMealPlan();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        
        datePickerDialog.show();
    }

    private void updateDateDisplay() {
        String formattedDate = dateFormat.format(selectedDate.getTime());
        tvSelectedDate.setText(getString(R.string.meal_plan_date, formattedDate));
    }

    private void loadMealPlan() {
        showLoading(true);
        
        if (firebaseHelper == null) {
            Log.e(TAG, "FirebaseHelper is null");
            Toast.makeText(this, "Error: Firebase helper not initialized", Toast.LENGTH_SHORT).show();
            showLoading(false);
            return;
        }

        if (selectedDate == null) {
            Log.e(TAG, "Selected date is null");
            Toast.makeText(this, "Error: No date selected", Toast.LENGTH_SHORT).show();
            showLoading(false);
            return;
        }

        Log.d(TAG, "Loading meal plan for date: " + selectedDate.getTime());
        
        firebaseHelper.getMealPlanByDate(selectedDate.getTime(), new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                showLoading(false);
                
                if (result == null) {
                    Log.w(TAG, "Meal plan result is null");
                    tvNoMealPlan.setVisibility(View.VISIBLE);
                    return;
                }
                
                if (result instanceof MealPlan) {
                    currentMealPlan = (MealPlan) result;
                    Log.d(TAG, "Meal plan loaded successfully: " + currentMealPlan.toString());
                    updateMealPlanDisplay();
                } else {
                    Log.e(TAG, "Result is not a MealPlan instance: " + result.getClass().getName());
                    Toast.makeText(MealPlanActivity.this, 
                            "Error: Invalid meal plan data format", 
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                Log.e(TAG, "Failed to load meal plan", e);
                Toast.makeText(MealPlanActivity.this, 
                        getString(R.string.error_load_meal_plans, e.getMessage()), 
                        Toast.LENGTH_SHORT).show();
                tvNoMealPlan.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateMealPlanDisplay() {
        boolean hasMeals = false;
        
        // Update Breakfast
        if (currentMealPlan.hasMeal(BREAKFAST)) {
            tvBreakfastMeal.setText(currentMealPlan.getMealName(BREAKFAST));
            loadMealDetails(BREAKFAST, currentMealPlan.getMealId(BREAKFAST), tvBreakfastCalories);
            hasMeals = true;
        } else {
            tvBreakfastMeal.setText(R.string.no_meal_selected);
            tvBreakfastCalories.setVisibility(View.GONE);
        }
        
        // Update Lunch
        if (currentMealPlan.hasMeal(LUNCH)) {
            tvLunchMeal.setText(currentMealPlan.getMealName(LUNCH));
            loadMealDetails(LUNCH, currentMealPlan.getMealId(LUNCH), tvLunchCalories);
            hasMeals = true;
        } else {
            tvLunchMeal.setText(R.string.no_meal_selected);
            tvLunchCalories.setVisibility(View.GONE);
        }
        
        // Update Dinner
        if (currentMealPlan.hasMeal(DINNER)) {
            tvDinnerMeal.setText(currentMealPlan.getMealName(DINNER));
            loadMealDetails(DINNER, currentMealPlan.getMealId(DINNER), tvDinnerCalories);
            hasMeals = true;
        } else {
            tvDinnerMeal.setText(R.string.no_meal_selected);
            tvDinnerCalories.setVisibility(View.GONE);
        }
        
        // Update Snacks
        if (currentMealPlan.hasMeal(SNACKS)) {
            tvSnacksMeal.setText(currentMealPlan.getMealName(SNACKS));
            loadMealDetails(SNACKS, currentMealPlan.getMealId(SNACKS), tvSnacksCalories);
            hasMeals = true;
        } else {
            tvSnacksMeal.setText(R.string.no_meal_selected);
            tvSnacksCalories.setVisibility(View.GONE);
        }
        
        // Show/hide empty view
        tvNoMealPlan.setVisibility(hasMeals ? View.GONE : View.VISIBLE);
    }

    private void loadMealDetails(String category, String mealId, TextView caloriesTextView) {
        firebaseHelper.getMealById(mealId, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof Meal) {
                    Meal meal = (Meal) result;
                    
                    // Update calories if available
                    if (meal.getCalories() > 0) {
                        String calories = String.format(Locale.getDefault(), 
                                getString(R.string.calories_format), meal.getCalories());
                        caloriesTextView.setText(calories);
                        caloriesTextView.setVisibility(View.VISIBLE);
                    } else {
                        caloriesTextView.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                caloriesTextView.setVisibility(View.GONE);
            }
        });
    }

    private void handleCardClick(String category) {
        if (currentMealPlan.hasMeal(category)) {
            // Open meal details
            String mealId = currentMealPlan.getMealId(category);
            Intent intent = new Intent(this, MealDetailActivity.class);
            intent.putExtra("meal_id", mealId);
            startActivity(intent);
            // Add entry animation
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } else {
            // Show add meal dialog
            showAddMealDialog(category);
        }
    }

    private void showAddMealDialog() {
        final String[] categories = {BREAKFAST, LUNCH, DINNER, SNACKS};
        final String[] categoryNames = {
                getString(R.string.breakfast),
                getString(R.string.lunch),
                getString(R.string.dinner),
                getString(R.string.snacks)
        };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_meal_type)
                .setItems(categoryNames, (dialog, which) -> {
                    String selectedCategory = categories[which];
                    showAddMealDialog(selectedCategory);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showAddMealDialog(String category) {
        Intent intent = new Intent(this,MealFormActivity.class);
        intent.putExtra("category", category);
        intent.putExtra("date", selectedDate.getTimeInMillis());
        if (currentMealPlan.getId() != null) {
            intent.putExtra("meal_plan_id", currentMealPlan.getId());
        }
        startActivity(intent);
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMealPlan();
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