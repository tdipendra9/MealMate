package edu.ismt.dipendra.mealmate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Locale;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.firebase.FirebaseHelper;
import edu.ismt.dipendra.mealmate.model.MealPlanItem;

public class MealPlanDetailActivity extends AppCompatActivity {

    private static final String TAG = "MealPlanDetailActivity";

    private Toolbar toolbar;
    private CollapsingToolbarLayout collapsingToolbar;
    private ImageView ivMealImage;
    private TextView tvMealName;
    private Chip chipMealType;
    private TextView tvCalories;
    private TextView tvIngredients;
    private TextView tvInstructions;
    private TextView tvServings;
    private TextView tvDate;
    private MaterialButton btnEdit;
    private MaterialButton btnDelete;
    private FloatingActionButton fabShare;
    private ProgressBar progressBar;

    private FirebaseHelper firebaseHelper;
    private String mealPlanId;
    private MealPlanItem mealPlan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_plan_detail);

        // Get meal plan ID from intent
        if (getIntent().hasExtra("meal_plan_id")) {
            mealPlanId = getIntent().getStringExtra("meal_plan_id");
        } else {
            Toast.makeText(this, "Meal plan ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        
        firebaseHelper = FirebaseHelper.getInstance();
        
        setupListeners();
        loadMealPlanDetails();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        collapsingToolbar = findViewById(R.id.collapsingToolbar);
        ivMealImage = findViewById(R.id.ivMealImage);
        tvMealName = findViewById(R.id.tvMealName);
        chipMealType = findViewById(R.id.chipMealType);
        tvCalories = findViewById(R.id.tvCalories);
        tvIngredients = findViewById(R.id.tvIngredients);
        tvInstructions = findViewById(R.id.tvInstructions);
        tvServings = findViewById(R.id.tvServings);
        tvDate = findViewById(R.id.tvDate);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        fabShare = findViewById(R.id.fabShare);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(""); // Title will be set in the collapsing toolbar
        }
    }

    private void setupListeners() {
        btnEdit.setOnClickListener(v -> {
            // TODO: Implement edit functionality
            Toast.makeText(this, "Edit functionality not implemented yet", Toast.LENGTH_SHORT).show();
        });
        
        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
        
        fabShare.setOnClickListener(v -> shareMealPlan());
    }

    private void loadMealPlanDetails() {
        showLoading(true);
        
        firebaseHelper.getMealPlanItemById(mealPlanId, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                showLoading(false);
                
                if (result instanceof MealPlanItem) {
                    mealPlan = (MealPlanItem) result;
                    updateUI();
                }
            }

            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                Toast.makeText(MealPlanDetailActivity.this, 
                        "Failed to load meal plan details: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void updateUI() {
        // Set meal name in both the TextView and CollapsingToolbarLayout
        tvMealName.setText(mealPlan.getMealName());
        collapsingToolbar.setTitle(mealPlan.getMealName());
        
        // Load image if available
        if (mealPlan.getImageUrl() != null && !mealPlan.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(mealPlan.getImageUrl())
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.placeholder_meal)
                            .error(R.drawable.placeholder_meal))
                    .into(ivMealImage);
        } else {
            // Load placeholder
            ivMealImage.setImageResource(R.drawable.placeholder_meal);
        }
        
        // Set meal type
        if (mealPlan.getMealType() != null && !mealPlan.getMealType().isEmpty()) {
            chipMealType.setText(mealPlan.getMealType());
            chipMealType.setVisibility(View.VISIBLE);
        } else {
            chipMealType.setVisibility(View.GONE);
        }
        
        // Set calories if available
        if (mealPlan.getCalories() > 0) {
            String calories = String.format(Locale.getDefault(), 
                    "%d kcal", mealPlan.getCalories());
            tvCalories.setText(calories);
            tvCalories.setVisibility(View.VISIBLE);
        } else {
            tvCalories.setVisibility(View.GONE);
        }
        
        // Set ingredients
        if (mealPlan.getIngredients() != null && !mealPlan.getIngredients().isEmpty()) {
            tvIngredients.setText(mealPlan.getIngredients());
        } else {
            tvIngredients.setText("No ingredients listed");
        }
        
        // Set instructions
        if (mealPlan.getInstructions() != null && !mealPlan.getInstructions().isEmpty()) {
            tvInstructions.setText(mealPlan.getInstructions());
        } else {
            tvInstructions.setText("No instructions provided");
        }
        
        // Set servings
        if (mealPlan.getServings() > 0) {
            tvServings.setText(String.valueOf(mealPlan.getServings()));
        } else {
            tvServings.setText("1");
        }
        
        // Set date
        if (mealPlan.getDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            tvDate.setText(sdf.format(mealPlan.getDate()));
        } else {
            tvDate.setText("No date specified");
        }
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_meal_plan)
                .setMessage(R.string.confirm_delete_meal_plan)
                .setPositiveButton(R.string.yes, (dialog, which) -> deleteMealPlan())
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void deleteMealPlan() {
        showLoading(true);
        
        firebaseHelper.deleteMealPlanItem(mealPlanId, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                showLoading(false);
                Toast.makeText(MealPlanDetailActivity.this, 
                        R.string.meal_plan_deleted, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                Toast.makeText(MealPlanDetailActivity.this, 
                        getString(R.string.error_deleting_meal_plan, e.getMessage()), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void shareMealPlan() {
        if (mealPlan == null) return;
        
        StringBuilder shareText = new StringBuilder();
        shareText.append(mealPlan.getMealName()).append("\n\n");
        
        // Add meal type
        if (mealPlan.getMealType() != null && !mealPlan.getMealType().isEmpty()) {
            shareText.append("Meal Type: ").append(mealPlan.getMealType()).append("\n\n");
        }
        
        // Add ingredients
        if (mealPlan.getIngredients() != null && !mealPlan.getIngredients().isEmpty()) {
            shareText.append("Ingredients:\n").append(mealPlan.getIngredients()).append("\n\n");
        }
        
        // Add instructions
        if (mealPlan.getInstructions() != null && !mealPlan.getInstructions().isEmpty()) {
            shareText.append("Instructions:\n").append(mealPlan.getInstructions()).append("\n\n");
        }
        
        // Add calories and servings
        if (mealPlan.getCalories() > 0) {
            shareText.append("Calories: ").append(mealPlan.getCalories()).append(" kcal\n");
        }
        
        if (mealPlan.getServings() > 0) {
            shareText.append("Servings: ").append(mealPlan.getServings()).append("\n\n");
        }
        
        // Add date
        if (mealPlan.getDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            shareText.append("Date: ").append(sdf.format(mealPlan.getDate())).append("\n\n");
        }
        
        shareText.append("Shared from MealMate App");
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Meal Plan: " + mealPlan.getMealName());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
        startActivity(Intent.createChooser(shareIntent, "Share Meal Plan"));
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mealPlanId != null) {
            loadMealPlanDetails();
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