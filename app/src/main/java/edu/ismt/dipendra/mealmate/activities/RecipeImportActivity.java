package edu.ismt.dipendra.mealmate.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.adapters.IngredientAdapter;
import edu.ismt.dipendra.mealmate.firebase.FirebaseHelper;
import edu.ismt.dipendra.mealmate.model.GroceryItem;
import edu.ismt.dipendra.mealmate.model.Meal;
import edu.ismt.dipendra.mealmate.utils.Constants;

public class RecipeImportActivity extends AppCompatActivity {
    private static final String TAG = "RecipeImportActivity";
    
    private Toolbar toolbar;
    private TextInputLayout tilRecipeUrl;
    private TextInputEditText etRecipeUrl;
    private Button btnExtractRecipe;
    private TextView tvRecipeSource;
    private TextInputEditText etRecipeName;
    private TextInputEditText etRecipeDescription;
    private RecyclerView rvIngredients;
    private Button btnAddIngredient;
    private TextInputEditText etRecipeInstructions;
    private TextInputEditText etPrepTime;
    private TextInputEditText etCookTime;
    private TextInputEditText etServings;
    private AutoCompleteTextView actvCategory;
    private Button btnSaveRecipe;
    private ProgressBar progressBar;
    
    private FirebaseHelper firebaseHelper;
    private IngredientAdapter ingredientAdapter;
    private List<String> ingredients = new ArrayList<>();
    private String sourceUrl = "";
    private String sourceDomain = "";
    
    private final Executor executor = Executors.newSingleThreadExecutor();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_import);
        
        firebaseHelper = FirebaseHelper.getInstance();
        
        initViews();
        setupToolbar();
        setupCategoryDropdown();
        setupIngredientsList();
        setupListeners();
        
        // Handle intent if activity was started from a share action
        handleIntent(getIntent());
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }
    
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tilRecipeUrl = findViewById(R.id.tilRecipeUrl);
        etRecipeUrl = findViewById(R.id.etRecipeUrl);
        btnExtractRecipe = findViewById(R.id.btnExtractRecipe);
        tvRecipeSource = findViewById(R.id.tvRecipeSource);
        etRecipeName = findViewById(R.id.etRecipeName);
        etRecipeDescription = findViewById(R.id.etRecipeDescription);
        rvIngredients = findViewById(R.id.rvIngredients);
        btnAddIngredient = findViewById(R.id.btnAddIngredient);
        etRecipeInstructions = findViewById(R.id.etRecipeInstructions);
        etPrepTime = findViewById(R.id.etPrepTime);
        etCookTime = findViewById(R.id.etCookTime);
        etServings = findViewById(R.id.etServings);
        actvCategory = findViewById(R.id.actvCategory);
        btnSaveRecipe = findViewById(R.id.btnSaveRecipe);
        progressBar = findViewById(R.id.progressIndicator);
    }
    
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }
    
    private void setupCategoryDropdown() {
        String[] categories = {
                getString(R.string.category_breakfast),
                getString(R.string.category_lunch),
                getString(R.string.category_dinner),
                getString(R.string.category_dessert),
                getString(R.string.category_snack),
                getString(R.string.category_other)
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                categories
        );
        
        actvCategory.setAdapter(adapter);
        actvCategory.setText(categories[2], false); // Default to dinner
    }
    
    private void setupIngredientsList() {
        ingredientAdapter = new IngredientAdapter(this, ingredients);
        rvIngredients.setLayoutManager(new LinearLayoutManager(this));
        rvIngredients.setAdapter(ingredientAdapter);
    }
    
    private void setupListeners() {
        btnExtractRecipe.setOnClickListener(v -> {
            String url = etRecipeUrl.getText().toString().trim();
            if (!TextUtils.isEmpty(url)) {
                extractRecipeFromUrl(url);
            } else {
                tilRecipeUrl.setError(getString(R.string.import_recipe_url_error));
            }
        });
        
        btnAddIngredient.setOnClickListener(v -> {
            ingredients.add("");
            ingredientAdapter.notifyItemInserted(ingredients.size() - 1);
        });
        
        btnSaveRecipe.setOnClickListener(v -> {
            if (validateForm()) {
                saveRecipe();
            }
        });
    }
    
    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();
        
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    // Try to extract URL from shared text
                    String url = extractUrlFromText(sharedText);
                    if (url != null) {
                        etRecipeUrl.setText(url);
                        extractRecipeFromUrl(url);
                    }
                }
            }
        }
    }
    
    private String extractUrlFromText(String text) {
        // Simple URL extraction - can be improved with regex
        if (text.contains("http://") || text.contains("https://")) {
            String[] words = text.split("\\s+");
            for (String word : words) {
                if (word.startsWith("http://") || word.startsWith("https://")) {
                    return word;
                }
            }
        }
        return null;
    }
    
    private void extractRecipeFromUrl(String url) {
        showLoading(true);
        
        executor.execute(() -> {
            try {
                Document doc = Jsoup.connect(url).get();
                
                // Extract recipe data using common patterns
                String title = extractTitle(doc);
                String description = extractDescription(doc);
                List<String> extractedIngredients = extractIngredients(doc);
                String instructions = extractInstructions(doc);
                int prepTime = extractPrepTime(doc);
                int cookTime = extractCookTime(doc);
                int servings = extractServings(doc);
                
                // Get source domain for attribution
                Uri uri = Uri.parse(url);
                sourceDomain = uri.getHost();
                if (sourceDomain.startsWith("www.")) {
                    sourceDomain = sourceDomain.substring(4);
                }
                sourceUrl = url;
                
                runOnUiThread(() -> {
                    showLoading(false);
                    
                    // Update UI with extracted data
                    etRecipeName.setText(title);
                    etRecipeDescription.setText(description);
                    
                    // Update ingredients
                    ingredients.clear();
                    ingredients.addAll(extractedIngredients);
                    if (ingredients.isEmpty()) {
                        ingredients.add(""); // Add empty ingredient if none found
                    }
                    ingredientAdapter.notifyDataSetChanged();
                    
                    etRecipeInstructions.setText(instructions);
                    
                    if (prepTime > 0) {
                        etPrepTime.setText(String.valueOf(prepTime));
                    }
                    
                    if (cookTime > 0) {
                        etCookTime.setText(String.valueOf(cookTime));
                    }
                    
                    if (servings > 0) {
                        etServings.setText(String.valueOf(servings));
                    }
                    
                    // Show source attribution
                    tvRecipeSource.setText(getString(R.string.import_recipe_source, sourceDomain));
                    tvRecipeSource.setVisibility(View.VISIBLE);
                    
                    Toast.makeText(RecipeImportActivity.this, 
                            R.string.import_recipe_edit_before_save, 
                            Toast.LENGTH_LONG).show();
                });
                
            } catch (IOException e) {
                Log.e(TAG, "Error extracting recipe: " + e.getMessage());
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(RecipeImportActivity.this, 
                            R.string.import_recipe_url_error, 
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private String extractTitle(Document doc) {
        // Try common schema.org markup first
        Elements elements = doc.select("h1.recipe-title, h1.entry-title, h1[itemprop=name], meta[property=og:title]");
        if (!elements.isEmpty()) {
            if (elements.first().tagName().equals("meta")) {
                return elements.first().attr("content");
            } else {
                return elements.first().text();
            }
        }
        
        // Fallback to page title
        String title = doc.title();
        if (title.contains("|")) {
            title = title.substring(0, title.indexOf("|")).trim();
        } else if (title.contains("-")) {
            title = title.substring(0, title.indexOf("-")).trim();
        }
        return title;
    }
    
    private String extractDescription(Document doc) {
        Elements elements = doc.select("meta[name=description], meta[property=og:description], div[itemprop=description], p.recipe-description");
        if (!elements.isEmpty()) {
            if (elements.first().tagName().equals("meta")) {
                return elements.first().attr("content");
            } else {
                return elements.first().text();
            }
        }
        return "";
    }
    
    private List<String> extractIngredients(Document doc) {
        List<String> result = new ArrayList<>();
        
        // Try common patterns for ingredients
        Elements elements = doc.select("li[itemprop=recipeIngredient], li[itemprop=ingredients], li.ingredient, .ingredients-list li");
        
        if (!elements.isEmpty()) {
            for (Element element : elements) {
                String ingredient = element.text().trim();
                if (!ingredient.isEmpty()) {
                    result.add(ingredient);
                }
            }
        }
        
        return result;
    }
    
    private String extractInstructions(Document doc) {
        // Try structured data first
        Elements elements = doc.select("[itemprop=recipeInstructions], .recipe-instructions, .instructions");
        
        if (!elements.isEmpty()) {
            Element instructionsElement = elements.first();
            
            // Check if instructions are in list items
            Elements steps = instructionsElement.select("li");
            if (!steps.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                int stepNum = 1;
                for (Element step : steps) {
                    sb.append(stepNum++).append(". ").append(step.text()).append("\n\n");
                }
                return sb.toString().trim();
            } else {
                return instructionsElement.text();
            }
        }
        
        return "";
    }
    
    private int extractPrepTime(Document doc) {
        Elements elements = doc.select("[itemprop=prepTime], .prep-time");
        if (!elements.isEmpty()) {
            String timeText = elements.first().text();
            return extractTimeInMinutes(timeText);
        }
        return 0;
    }
    
    private int extractCookTime(Document doc) {
        Elements elements = doc.select("[itemprop=cookTime], .cook-time");
        if (!elements.isEmpty()) {
            String timeText = elements.first().text();
            return extractTimeInMinutes(timeText);
        }
        return 0;
    }
    
    private int extractServings(Document doc) {
        Elements elements = doc.select("[itemprop=recipeYield], .recipe-yield, .servings");
        if (!elements.isEmpty()) {
            String servingsText = elements.first().text();
            // Try to extract number from text like "Serves 4" or "4 servings"
            try {
                return Integer.parseInt(servingsText.replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
    
    private int extractTimeInMinutes(String timeText) {
        // Extract numbers from text like "15 mins" or "1 hour 30 minutes"
        int minutes = 0;
        
        if (timeText.contains("hour") || timeText.contains("hr")) {
            String[] parts = timeText.split("\\s+");
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].matches("\\d+") && i + 1 < parts.length) {
                    if (parts[i + 1].startsWith("hour") || parts[i + 1].startsWith("hr")) {
                        minutes += Integer.parseInt(parts[i]) * 60;
                    } else if (parts[i + 1].startsWith("min")) {
                        minutes += Integer.parseInt(parts[i]);
                    }
                }
            }
        } else if (timeText.contains("min")) {
            String[] parts = timeText.split("\\s+");
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].matches("\\d+")) {
                    minutes += Integer.parseInt(parts[i]);
                    break;
                }
            }
        }
        
        return minutes;
    }
    
    private boolean validateForm() {
        boolean valid = true;
        
        String name = etRecipeName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            etRecipeName.setError(getString(R.string.error_meal_name));
            valid = false;
        }
        
        // Filter out empty ingredients
        List<String> validIngredients = new ArrayList<>();
        for (String ingredient : ingredients) {
            if (!TextUtils.isEmpty(ingredient.trim())) {
                validIngredients.add(ingredient.trim());
            }
        }
        
        if (validIngredients.isEmpty()) {
            Toast.makeText(this, R.string.error_meal_ingredients, Toast.LENGTH_SHORT).show();
            valid = false;
        } else {
            ingredients = validIngredients;
        }
        
        String instructions = etRecipeInstructions.getText().toString().trim();
        if (TextUtils.isEmpty(instructions)) {
            etRecipeInstructions.setError(getString(R.string.error_meal_instructions));
            valid = false;
        }
        
        return valid;
    }
    
    private void saveRecipe() {
        showLoading(true);
        
        // Create meal object
        Meal meal = new Meal();
        meal.setId(UUID.randomUUID().toString());
        meal.setName(etRecipeName.getText().toString().trim());
        meal.setDescription(etRecipeDescription.getText().toString().trim());
        meal.setIngredients(new ArrayList<>(ingredients));
        meal.setInstructions(etRecipeInstructions.getText().toString().trim());
        
        // Parse numeric fields
        try {
            String prepTimeStr = etPrepTime.getText().toString().trim();
            if (!TextUtils.isEmpty(prepTimeStr)) {
                meal.setPreparationTime(Integer.parseInt(prepTimeStr));
            }
            
            String cookTimeStr = etCookTime.getText().toString().trim();
            if (!TextUtils.isEmpty(cookTimeStr)) {
                meal.setCookingTime(Integer.parseInt(cookTimeStr));
            }
            
            String servingsStr = etServings.getText().toString().trim();
            if (!TextUtils.isEmpty(servingsStr)) {
                meal.setServings(Integer.parseInt(servingsStr));
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing numeric fields: " + e.getMessage());
        }
        
        meal.setCategory(actvCategory.getText().toString());
        meal.setUserId(firebaseHelper.getCurrentUserId());
        meal.setSourceUrl(sourceUrl);
        meal.setSourceName(sourceDomain);
        meal.setCreatedAt(new Date());
        
        // Save meal to Firebase
        firebaseHelper.addMeal(meal, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                showLoading(false);
                Toast.makeText(RecipeImportActivity.this, 
                        R.string.import_recipe_success, 
                        Toast.LENGTH_SHORT).show();
                finish();
            }
            
            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                Toast.makeText(RecipeImportActivity.this, 
                        getString(R.string.import_recipe_error) + ": " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnExtractRecipe.setEnabled(!isLoading);
        btnSaveRecipe.setEnabled(!isLoading);
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