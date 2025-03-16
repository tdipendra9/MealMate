package edu.ismt.dipendra.mealmate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Date;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.firebase.FirebaseHelper;
import edu.ismt.dipendra.mealmate.model.Item;

public class ItemFormActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextInputLayout textInputLayoutName;
    private TextInputLayout textInputLayoutDescription;
    private TextInputLayout textInputLayoutPrice;
    private TextInputLayout textInputLayoutQuantity;
    private TextInputEditText editTextName;
    private TextInputEditText editTextDescription;
    private TextInputEditText editTextPrice;
    private TextInputEditText editTextQuantity;
    private Spinner spinnerCategory;
    private CheckBox checkBoxPurchased;
    private MaterialButton buttonSave;
    private CircularProgressIndicator progressIndicator;

    private FirebaseHelper firebaseHelper;
    private boolean isEditMode = false;
    private String itemId;
    private Date createdAt;
    
    // Categories for grocery items
    private final String[] categories = {"Vegetables", "Fruits", "Dairy", "Grains", "Proteins", "Snacks", "Beverages", "Other"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_form);

        initViews();
        setupToolbar();
        initObjects();
        setupCategorySpinner();
        checkForEditMode();
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        textInputLayoutName = findViewById(R.id.textInputLayoutName);
        textInputLayoutDescription = findViewById(R.id.textInputLayoutDescription);
        textInputLayoutPrice = findViewById(R.id.textInputLayoutPrice);
        textInputLayoutQuantity = findViewById(R.id.textInputLayoutQuantity);
        editTextName = findViewById(R.id.editTextName);
        editTextDescription = findViewById(R.id.editTextDescription);
        editTextPrice = findViewById(R.id.editTextPrice);
        editTextQuantity = findViewById(R.id.editTextQuantity);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        checkBoxPurchased = findViewById(R.id.checkBoxPurchased);
        buttonSave = findViewById(R.id.buttonSave);
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
    }
    
    private void setupCategorySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void checkForEditMode() {
        if (getIntent().hasExtra("item_id")) {
            isEditMode = true;
            itemId = getIntent().getStringExtra("item_id");
            String name = getIntent().getStringExtra("item_name");
            String description = getIntent().getStringExtra("item_description");
            double price = getIntent().getDoubleExtra("item_price", 0.0);
            boolean purchased = getIntent().getBooleanExtra("item_purchased", false);
            String category = getIntent().getStringExtra("item_category");
            int quantity = getIntent().getIntExtra("item_quantity", 1);
            
            // Try to get the creation date if available
            if (getIntent().hasExtra("item_created_at")) {
                long createdAtMillis = getIntent().getLongExtra("item_created_at", 0);
                if (createdAtMillis > 0) {
                    createdAt = new Date(createdAtMillis);
                }
            }
            
            // Set the data to the views
            editTextName.setText(name);
            editTextDescription.setText(description);
            editTextPrice.setText(String.valueOf(price));
            editTextQuantity.setText(String.valueOf(quantity));
            checkBoxPurchased.setChecked(purchased);
            
            // Set category spinner
            if (category != null && !category.isEmpty()) {
                for (int i = 0; i < categories.length; i++) {
                    if (categories[i].equals(category)) {
                        spinnerCategory.setSelection(i);
                        break;
                    }
                }
            }
            
            // Update toolbar title
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.edit_item);
            }
        }
    }

    private void setupListeners() {
        buttonSave.setOnClickListener(v -> saveItem());
    }

    private void saveItem() {
        if (!validateInputs()) {
            return;
        }
        
        progressIndicator.setVisibility(View.VISIBLE);
        buttonSave.setEnabled(false);
        
        String name = editTextName.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        double price = Double.parseDouble(editTextPrice.getText().toString().trim());
        int quantity = Integer.parseInt(editTextQuantity.getText().toString().trim());
        boolean purchased = checkBoxPurchased.isChecked();
        String category = spinnerCategory.getSelectedItem().toString();
        
        if (isEditMode) {
            // Update existing item
            Item item = new Item();
            item.setId(itemId);
            item.setName(name);
            item.setDescription(description);
            item.setPrice(price);
            item.setQuantity(quantity);
            item.setPurchased(purchased);
            item.setCategory(category);
            
            // Preserve the creation date if we have it
            if (createdAt != null) {
                item.setCreatedAt(createdAt);
            }
            
            // Set the user ID
            item.setUserId(firebaseHelper.getCurrentUserId());
            
            firebaseHelper.updateItem(item, new FirebaseHelper.FirebaseCallback() {
                @Override
                public void onSuccess(Object result) {
                    progressIndicator.setVisibility(View.GONE);
                    buttonSave.setEnabled(true);
                    Toast.makeText(ItemFormActivity.this, R.string.item_updated, Toast.LENGTH_SHORT).show();
                    
                    // Return the updated item ID to the calling activity
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("item_id", item.getId());
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }

                @Override
                public void onFailure(Exception e) {
                    progressIndicator.setVisibility(View.GONE);
                    buttonSave.setEnabled(true);
                    Toast.makeText(ItemFormActivity.this, 
                            getString(R.string.error_update_item, e.getMessage()), 
                            Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Add new item
            Item item = new Item(name, description, price, firebaseHelper.getCurrentUserId(), category, quantity);
            item.setPurchased(purchased);
            
            firebaseHelper.addItem(item, new FirebaseHelper.FirebaseCallback() {
                @Override
                public void onSuccess(Object result) {
                    progressIndicator.setVisibility(View.GONE);
                    buttonSave.setEnabled(true);
                    
                    // Get the new item ID from the result
                    String newItemId = (String) result;
                    
                    Toast.makeText(ItemFormActivity.this, R.string.item_added, Toast.LENGTH_SHORT).show();
                    
                    // Return the new item ID to the calling activity
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("item_id", newItemId);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }

                @Override
                public void onFailure(Exception e) {
                    progressIndicator.setVisibility(View.GONE);
                    buttonSave.setEnabled(true);
                    Toast.makeText(ItemFormActivity.this, 
                            getString(R.string.error_add_item, e.getMessage()), 
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private boolean validateInputs() {
        boolean isValid = true;
        
        // Validate name
        if (TextUtils.isEmpty(editTextName.getText())) {
            textInputLayoutName.setError(getString(R.string.error_item_name));
            isValid = false;
        } else {
            textInputLayoutName.setError(null);
        }
        
        // Validate price
        String priceStr = editTextPrice.getText().toString().trim();
        if (TextUtils.isEmpty(priceStr)) {
            textInputLayoutPrice.setError(getString(R.string.error_item_price));
            isValid = false;
        } else {
            try {
                double price = Double.parseDouble(priceStr);
                if (price < 0) {
                    textInputLayoutPrice.setError(getString(R.string.error_item_price));
                    isValid = false;
                } else {
                    textInputLayoutPrice.setError(null);
                }
            } catch (NumberFormatException e) {
                textInputLayoutPrice.setError(getString(R.string.error_item_price));
                isValid = false;
            }
        }
        
        // Validate quantity
        String quantityStr = editTextQuantity.getText().toString().trim();
        if (TextUtils.isEmpty(quantityStr)) {
            textInputLayoutQuantity.setError(getString(R.string.error_item_quantity));
            isValid = false;
        } else {
            try {
                int quantity = Integer.parseInt(quantityStr);
                if (quantity <= 0) {
                    textInputLayoutQuantity.setError(getString(R.string.error_item_quantity));
                    isValid = false;
                } else {
                    textInputLayoutQuantity.setError(null);
                }
            } catch (NumberFormatException e) {
                textInputLayoutQuantity.setError(getString(R.string.error_item_quantity));
                isValid = false;
            }
        }
        
        return isValid;
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