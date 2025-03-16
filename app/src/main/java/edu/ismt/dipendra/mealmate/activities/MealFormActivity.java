package edu.ismt.dipendra.mealmate.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.adapters.IngredientAdapter;
import edu.ismt.dipendra.mealmate.adapters.InstructionStepAdapter;
import edu.ismt.dipendra.mealmate.firebase.FirebaseHelper;
import edu.ismt.dipendra.mealmate.model.Meal;

public class MealFormActivity extends AppCompatActivity implements IngredientAdapter.OnIngredientClickListener, InstructionStepAdapter.OnStepClickListener {

    private static final String TAG = "MealFormActivity";
    private static final int REQUEST_IMAGE_PICK = 1;

    private MaterialToolbar toolbar;
    private TextInputLayout textInputLayoutName;
    private TextInputLayout textInputLayoutDescription;
    private TextInputLayout textInputLayoutInstructions;
    private TextInputLayout textInputLayoutPrepTime;
    private TextInputLayout textInputLayoutCookTime;
    private TextInputLayout textInputLayoutServings;
    private TextInputLayout textInputLayoutIngredient;
    private TextInputLayout textInputLayoutImageUrl;
    private TextInputLayout textInputLayoutStep;
    private TextInputEditText editTextName;
    private TextInputEditText editTextDescription;
    private TextInputEditText editTextInstructions;
    private TextInputEditText editTextPrepTime;
    private TextInputEditText editTextCookTime;
    private TextInputEditText editTextServings;
    private TextInputEditText editTextIngredient;
    private TextInputEditText editTextImageUrl;
    private TextInputEditText editTextStep;
    private MaterialButton buttonAddIngredient;
    private MaterialButton buttonAddStep;
    private MaterialButton buttonPickImage;
    private MaterialButton buttonUseUrl;
    private MaterialButton buttonSave;
    private ImageView imageViewMeal;
    private RecyclerView recyclerViewIngredients;
    private RecyclerView recyclerViewSteps;
    private CircularProgressIndicator progressIndicator;
    private View layoutImagePlaceholder;

    private FirebaseHelper firebaseHelper;
    private IngredientAdapter ingredientAdapter;
    private InstructionStepAdapter stepAdapter;
    private List<String> ingredientList;
    private List<String> stepList;
    private boolean isEditMode = false;
    private String mealId;
    private Date createdAt;
    private String currentImageUrl;
    private Uri selectedImageUri;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_form);

        initViews();
        setupToolbar();
        initObjects();
        setupRecyclerView();
        checkForEditMode();
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        textInputLayoutName = findViewById(R.id.textInputLayoutName);
        textInputLayoutDescription = findViewById(R.id.textInputLayoutDescription);
        textInputLayoutInstructions = findViewById(R.id.textInputLayoutInstructions);
        textInputLayoutPrepTime = findViewById(R.id.textInputLayoutPrepTime);
        textInputLayoutCookTime = findViewById(R.id.textInputLayoutCookTime);
        textInputLayoutServings = findViewById(R.id.textInputLayoutServings);
        textInputLayoutIngredient = findViewById(R.id.textInputLayoutIngredient);
        textInputLayoutImageUrl = findViewById(R.id.textInputLayoutImageUrl);
        textInputLayoutStep = findViewById(R.id.textInputLayoutStep);
        editTextName = findViewById(R.id.editTextName);
        editTextDescription = findViewById(R.id.editTextDescription);
        editTextInstructions = findViewById(R.id.editTextInstructions);
        editTextPrepTime = findViewById(R.id.editTextPrepTime);
        editTextCookTime = findViewById(R.id.editTextCookTime);
        editTextServings = findViewById(R.id.editTextServings);
        editTextIngredient = findViewById(R.id.editTextIngredient);
        editTextImageUrl = findViewById(R.id.editTextImageUrl);
        editTextStep = findViewById(R.id.editTextStep);
        buttonAddIngredient = findViewById(R.id.buttonAddIngredient);
        buttonAddStep = findViewById(R.id.buttonAddStep);
        buttonPickImage = findViewById(R.id.buttonPickImage);
        buttonUseUrl = findViewById(R.id.buttonUseUrl);
        buttonSave = findViewById(R.id.buttonSave);
        imageViewMeal = findViewById(R.id.imageViewMeal);
        layoutImagePlaceholder = findViewById(R.id.layoutImagePlaceholder);
        recyclerViewIngredients = findViewById(R.id.recyclerViewIngredients);
        recyclerViewSteps = findViewById(R.id.recyclerViewSteps);
        progressIndicator = findViewById(R.id.progressIndicator);
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
        ingredientList = new ArrayList<>();
        stepList = new ArrayList<>();
    }
    
    private void setupRecyclerView() {
        recyclerViewIngredients.setLayoutManager(new LinearLayoutManager(this));
        ingredientAdapter = new IngredientAdapter(this, ingredientList, this);
        recyclerViewIngredients.setAdapter(ingredientAdapter);
        
        recyclerViewSteps.setLayoutManager(new LinearLayoutManager(this));
        stepAdapter = new InstructionStepAdapter(this, stepList, this);
        recyclerViewSteps.setAdapter(stepAdapter);
    }

    private void checkForEditMode() {
        if (getIntent().hasExtra("meal_id")) {
            isEditMode = true;
            mealId = getIntent().getStringExtra("meal_id");
            String name = getIntent().getStringExtra("meal_name");
            String description = getIntent().getStringExtra("meal_description");
            String instructions = getIntent().getStringExtra("meal_instructions");
            int prepTime = getIntent().getIntExtra("meal_prep_time", 0);
            int cookTime = getIntent().getIntExtra("meal_cook_time", 0);
            int servings = getIntent().getIntExtra("meal_servings", 1);
            currentImageUrl = getIntent().getStringExtra("meal_image_url");
            
            // Get ingredients
            ArrayList<String> ingredients = getIntent().getStringArrayListExtra("meal_ingredients");
            if (ingredients != null) {
                ingredientList.addAll(ingredients);
                ingredientAdapter.notifyDataSetChanged();
            }
            
            // Get instructions and parse into steps if possible
            if (instructions != null && !instructions.isEmpty()) {
                // Check if instructions can be split into steps
                if (instructions.contains("\n")) {
                    String[] steps = instructions.split("\n");
                    for (String step : steps) {
                        if (!step.trim().isEmpty()) {
                            stepList.add(step.trim());
                        }
                    }
                    stepAdapter.notifyDataSetChanged();
                } else {
                    // Just set as text
                    editTextInstructions.setText(instructions);
                }
            }
            
            // Try to get the creation date if available
            if (getIntent().hasExtra("meal_created_at")) {
                long createdAtMillis = getIntent().getLongExtra("meal_created_at", 0);
                if (createdAtMillis > 0) {
                    createdAt = new Date(createdAtMillis);
                }
            }
            
            // Set the data to the views
            editTextName.setText(name);
            editTextDescription.setText(description);
            editTextPrepTime.setText(String.valueOf(prepTime));
            editTextCookTime.setText(String.valueOf(cookTime));
            editTextServings.setText(String.valueOf(servings));
            
            // Set the image URL if available
            if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                editTextImageUrl.setText(currentImageUrl);
                
                // Load image if available
                Glide.with(this)
                        .load(currentImageUrl)
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.placeholder_meal)
                                .error(R.drawable.placeholder_meal))
                        .into(imageViewMeal);
                imageViewMeal.setVisibility(View.VISIBLE);
                layoutImagePlaceholder.setVisibility(View.GONE);
            }
            
            // Update toolbar title
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.edit_meal);
            }
        }
    }

    private void setupListeners() {
        buttonAddIngredient.setOnClickListener(v -> addIngredient());
        buttonAddStep.setOnClickListener(v -> addStep());
        buttonPickImage.setOnClickListener(v -> pickImage());
        buttonUseUrl.setOnClickListener(v -> useUrl());
        buttonSave.setOnClickListener(v -> saveMeal());
    }
    
    private void addIngredient() {
        String ingredient = editTextIngredient.getText().toString().trim();
        if (!TextUtils.isEmpty(ingredient)) {
            ingredientList.add(ingredient);
            ingredientAdapter.notifyItemInserted(ingredientList.size() - 1);
            editTextIngredient.setText("");
            textInputLayoutIngredient.setError(null);
        } else {
            textInputLayoutIngredient.setError(getString(R.string.error_ingredient_empty));
        }
    }
    
    private void addStep() {
        String step = editTextStep.getText().toString().trim();
        if (!TextUtils.isEmpty(step)) {
            stepList.add(step);
            stepAdapter.notifyItemInserted(stepList.size() - 1);
            editTextStep.setText("");
            textInputLayoutStep.setError(null);
        } else {
            textInputLayoutStep.setError(getString(R.string.error_step_empty));
        }
    }
    
    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private void useUrl() {
        String imageUrl = editTextImageUrl.getText().toString().trim();
        if (!TextUtils.isEmpty(imageUrl)) {
            // Validate the URL
            if (firebaseHelper.isValidImageUrl(imageUrl)) {
                // Clear any selected image from device
                selectedImageUri = null;
                
                // Set the current image URL
                currentImageUrl = imageUrl;
                
                // Load and display the image from URL
                Glide.with(this)
                        .load(imageUrl)
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.placeholder_meal)
                                .error(R.drawable.placeholder_meal))
                        .into(imageViewMeal);
                
                imageViewMeal.setVisibility(View.VISIBLE);
                layoutImagePlaceholder.setVisibility(View.GONE);
                Toast.makeText(this, R.string.image_url_set, Toast.LENGTH_SHORT).show();
                textInputLayoutImageUrl.setError(null);
            } else {
                textInputLayoutImageUrl.setError(getString(R.string.error_invalid_image_url));
            }
        } else {
            textInputLayoutImageUrl.setError(getString(R.string.error_image_url));
        }
    }

    private void saveMeal() {
        if (!validateInputs()) {
            return;
        }
        
        progressIndicator.setVisibility(View.VISIBLE);
        buttonSave.setEnabled(false);
        
        String name = editTextName.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        
        // Get instructions from either steps list or text field
        String instructions;
        if (stepList.size() > 0) {
            // Convert steps to instructions with line breaks
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < stepList.size(); i++) {
                sb.append(i + 1).append(". ").append(stepList.get(i));
                if (i < stepList.size() - 1) {
                    sb.append("\n");
                }
            }
            instructions = sb.toString();
        } else {
            // Use text from instructions field
            instructions = editTextInstructions.getText().toString().trim();
        }
        
        int prepTime = Integer.parseInt(editTextPrepTime.getText().toString().trim());
        int cookTime = Integer.parseInt(editTextCookTime.getText().toString().trim());
        int servings = Integer.parseInt(editTextServings.getText().toString().trim());
        
        // Use a default category instead of getting it from the spinner
        String category = "Other";
        
        // Create the meal object first to ensure it's saved to Firebase
        final Meal meal;
        if (isEditMode) {
            meal = new Meal();
            meal.setId(mealId);
            meal.setName(name);
            meal.setDescription(description);
            meal.setInstructions(instructions);
            meal.setPreparationTime(prepTime);
            meal.setCookingTime(cookTime);
            meal.setServings(servings);
            meal.setCategory(category);
            meal.setIngredients(ingredientList);
            
            // Preserve the creation date if we have it
            if (createdAt != null) {
                meal.setCreatedAt(createdAt);
            }
        } else {
            meal = new Meal(name, description, ingredientList, instructions, 
                    prepTime, cookTime, servings, category, firebaseHelper.getCurrentUserId());
        }
        
        // Set the user ID
        meal.setUserId(firebaseHelper.getCurrentUserId());
        
        // Handle image upload or URL setting
        String imageUrlFromField = editTextImageUrl.getText().toString().trim();
        if (!TextUtils.isEmpty(imageUrlFromField) && selectedImageUri == null) {
            // Validate the URL
            if (firebaseHelper.isValidImageUrl(imageUrlFromField)) {
                // Use the URL directly
                meal.setImageUrl(imageUrlFromField);
                saveToFirebase(meal);
                textInputLayoutImageUrl.setError(null);
            } else {
                progressIndicator.setVisibility(View.GONE);
                buttonSave.setEnabled(true);
                textInputLayoutImageUrl.setError(getString(R.string.error_invalid_image_url));
            }
        } 
        // If we have a selected image from device, upload it
        else if (selectedImageUri != null) {
            uploadImageAndSaveMeal(meal);
        } 
        // If we have a current image URL (from editing), use it
        else if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
            meal.setImageUrl(currentImageUrl);
            saveToFirebase(meal);
        } 
        // No image
        else {
            meal.setImageUrl(null);
            saveToFirebase(meal);
        }
    }
    
    /**
     * Uploads the selected image and then saves the meal to Firebase
     */
    private void uploadImageAndSaveMeal(final Meal meal) {
        if (selectedImageUri != null) {
            // Upload the image first
            firebaseHelper.uploadImage(selectedImageUri, new FirebaseHelper.FirebaseCallback() {
                @Override
                public void onSuccess(Object result) {
                    String imageUrl = (String) result;
                    meal.setImageUrl(imageUrl);
                    saveToFirebase(meal);
                }

                @Override
                public void onFailure(Exception e) {
                    progressIndicator.setVisibility(View.GONE);
                    buttonSave.setEnabled(true);
                    Toast.makeText(MealFormActivity.this, 
                            getString(R.string.error_upload_image, e.getMessage()), 
                            Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            saveToFirebase(meal);
        }
    }
    
    /**
     * Saves the meal to Firebase
     */
    private void saveToFirebase(final Meal meal) {
        if (isEditMode) {
            // Update existing meal in the recipe collection
            firebaseHelper.updateMealInRecipe(meal, new FirebaseHelper.FirebaseCallback() {
                @Override
                public void onSuccess(Object result) {
                    finishSaveOperation(meal.getId(), R.string.meal_updated);
                }

                @Override
                public void onFailure(Exception e) {
                    progressIndicator.setVisibility(View.GONE);
                    buttonSave.setEnabled(true);
                    Toast.makeText(MealFormActivity.this, 
                            getString(R.string.error_update_meal, e.getMessage()), 
                            Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Add new meal to the recipe collection
            firebaseHelper.addMealToRecipe(meal, new FirebaseHelper.FirebaseCallback() {
                @Override
                public void onSuccess(Object result) {
                    String newMealId = meal.getId();
                    finishSaveOperation(newMealId, R.string.meal_added);
                }

                @Override
                public void onFailure(Exception e) {
                    progressIndicator.setVisibility(View.GONE);
                    buttonSave.setEnabled(true);
                    Toast.makeText(MealFormActivity.this, 
                            getString(R.string.error_add_meal, e.getMessage()), 
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    
    /**
     * Finishes the save operation after successful Firebase save
     */
    private void finishSaveOperation(String mealId, int successMessageResId) {
        progressIndicator.setVisibility(View.GONE);
        buttonSave.setEnabled(true);
        
        Toast.makeText(MealFormActivity.this, successMessageResId, Toast.LENGTH_SHORT).show();
        
        // Return the meal ID to the calling activity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("meal_id", mealId);
        resultIntent.putExtra("refresh_meals", true); // Add flag to force refresh
        setResult(RESULT_OK, resultIntent);
        
        // Finish the activity to return to MealListActivity
        finish();
    }

    private boolean validateInputs() {
        boolean isValid = true;
        
        // Validate name
        if (TextUtils.isEmpty(editTextName.getText())) {
            textInputLayoutName.setError(getString(R.string.error_meal_name));
            isValid = false;
        } else {
            textInputLayoutName.setError(null);
        }
        
        // Validate instructions (either steps or text)
        if (stepList.isEmpty() && TextUtils.isEmpty(editTextInstructions.getText().toString().trim())) {
            textInputLayoutInstructions.setError(getString(R.string.error_meal_instructions));
            isValid = false;
        } else {
            textInputLayoutInstructions.setError(null);
        }
        
        // Validate ingredients
        if (ingredientList.isEmpty()) {
            textInputLayoutIngredient.setError(getString(R.string.error_meal_ingredients));
            isValid = false;
        } else {
            textInputLayoutIngredient.setError(null);
        }
        
        // Validate preparation time
        String prepTimeStr = editTextPrepTime.getText().toString().trim();
        if (TextUtils.isEmpty(prepTimeStr)) {
            textInputLayoutPrepTime.setError(getString(R.string.error_prep_time));
            isValid = false;
        } else {
            try {
                int prepTime = Integer.parseInt(prepTimeStr);
                if (prepTime < 0) {
                    textInputLayoutPrepTime.setError(getString(R.string.error_prep_time));
                    isValid = false;
                } else {
                    textInputLayoutPrepTime.setError(null);
                }
            } catch (NumberFormatException e) {
                textInputLayoutPrepTime.setError(getString(R.string.error_prep_time));
                isValid = false;
            }
        }
        
        // Validate cooking time
        String cookTimeStr = editTextCookTime.getText().toString().trim();
        if (TextUtils.isEmpty(cookTimeStr)) {
            textInputLayoutCookTime.setError(getString(R.string.error_cook_time));
            isValid = false;
        } else {
            try {
                int cookTime = Integer.parseInt(cookTimeStr);
                if (cookTime < 0) {
                    textInputLayoutCookTime.setError(getString(R.string.error_cook_time));
                    isValid = false;
                } else {
                    textInputLayoutCookTime.setError(null);
                }
            } catch (NumberFormatException e) {
                textInputLayoutCookTime.setError(getString(R.string.error_cook_time));
                isValid = false;
            }
        }
        
        // Validate servings
        String servingsStr = editTextServings.getText().toString().trim();
        if (TextUtils.isEmpty(servingsStr)) {
            textInputLayoutServings.setError(getString(R.string.error_servings));
            isValid = false;
        } else {
            try {
                int servings = Integer.parseInt(servingsStr);
                if (servings <= 0) {
                    textInputLayoutServings.setError(getString(R.string.error_servings));
                    isValid = false;
                } else {
                    textInputLayoutServings.setError(null);
                }
            } catch (NumberFormatException e) {
                textInputLayoutServings.setError(getString(R.string.error_servings));
                isValid = false;
            }
        }
        
        return isValid;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                // Display the selected image
                Glide.with(this)
                        .load(selectedImageUri)
                        .into(imageViewMeal);
                imageViewMeal.setVisibility(View.VISIBLE);
                layoutImagePlaceholder.setVisibility(View.GONE);
                
                // Clear the image URL field
                editTextImageUrl.setText("");
                currentImageUrl = null;
            }
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
    public void onDeleteClick(int position) {
        ingredientList.remove(position);
        ingredientAdapter.notifyItemRemoved(position);
        ingredientAdapter.notifyItemRangeChanged(position, ingredientList.size());
    }

    @Override
    public void onStepDelete(int position) {
        stepList.remove(position);
        stepAdapter.notifyItemRemoved(position);
        stepAdapter.notifyItemRangeChanged(position, stepList.size());
    }
} 