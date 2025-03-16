package edu.ismt.dipendra.mealmate.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.firebase.FirebaseHelper;
import edu.ismt.dipendra.mealmate.model.GroceryItem;
import edu.ismt.dipendra.mealmate.model.GroceryList;

import java.util.List;

public class GroceryItemDetailActivity extends AppCompatActivity {

    private TextView tvItemName, tvCategory, tvQuantity, tvPrice, tvStoreName, tvDelegatedTo, tvNotes;
    private CheckBox cbItemPurchased;
    private MaterialButton btnFindStore, btnContact, btnEdit, btnDelete;
    private MaterialCardView cardStore, cardDelegation, cardNotes;
    
    private FirebaseHelper firebaseHelper;
    private GroceryItem groceryItem;
    private String itemId;
    private GroceryList groceryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grocery_item_detail);
        
        // Initialize Firebase helper
        firebaseHelper = FirebaseHelper.getInstance();
        
        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Item Details");
        }
        
        // Initialize views
        initViews();
        
        // Get item ID from intent
        itemId = getIntent().getStringExtra("item_id");
        if (itemId == null || itemId.isEmpty()) {
            Toast.makeText(this, "Error: Item not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Load grocery list first, then find the item
        loadGroceryList();
        
        // Set up listeners
        setupListeners();
    }
    
    private void initViews() {
        tvItemName = findViewById(R.id.tvItemName);
        tvCategory = findViewById(R.id.tvCategory);
        tvQuantity = findViewById(R.id.tvQuantity);
        tvPrice = findViewById(R.id.tvPrice);
        tvStoreName = findViewById(R.id.tvStoreName);
        tvDelegatedTo = findViewById(R.id.tvDelegatedTo);
        tvNotes = findViewById(R.id.tvNotes);
        
        cbItemPurchased = findViewById(R.id.cbItemPurchased);
        
        btnFindStore = findViewById(R.id.btnFindStore);
        btnContact = findViewById(R.id.btnContact);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        
        cardStore = findViewById(R.id.cardStore);
        cardDelegation = findViewById(R.id.cardDelegation);
        cardNotes = findViewById(R.id.cardNotes);
    }
    
    private void loadGroceryList() {
        // Get the current user ID
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Log for debugging
        System.out.println("Loading grocery list for user: " + userId + " to find item: " + itemId);
        
        // Load the grocery list from Firebase
        firebaseHelper.getGroceryLists(userId, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof List<?>) {
                    List<?> lists = (List<?>) result;
                    System.out.println("Received " + lists.size() + " grocery lists");
                    
                    if (!lists.isEmpty()) {
                        if (lists.get(0) instanceof GroceryList) {
                            // For simplicity, we'll use the first grocery list
                            groceryList = (GroceryList) lists.get(0);
                            System.out.println("Using grocery list: " + groceryList.getName() + " with " + groceryList.getItems().size() + " items");
                            // Find the item in the grocery list
                            findGroceryItem();
                        } else {
                            System.out.println("First item is not a GroceryList: " + lists.get(0).getClass().getName());
                            Toast.makeText(GroceryItemDetailActivity.this, 
                                    "Error: Invalid grocery list format", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        System.out.println("No grocery lists found");
                        Toast.makeText(GroceryItemDetailActivity.this, 
                                "Error: No grocery lists found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    System.out.println("Result is not a List: " + (result != null ? result.getClass().getName() : "null"));
                    Toast.makeText(GroceryItemDetailActivity.this, 
                            "Error: Grocery list data format incorrect", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                System.out.println("Error loading grocery list: " + e.getMessage());
                e.printStackTrace();
                
                Toast.makeText(GroceryItemDetailActivity.this, 
                        "Error loading grocery list: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
    
    private void findGroceryItem() {
        if (groceryList == null) {
            System.out.println("Error: Grocery list is null");
            Toast.makeText(this, "Error: Grocery list not loaded", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        if (groceryList.getItems() == null) {
            System.out.println("Error: Grocery list items is null");
            Toast.makeText(this, "Error: Grocery list has no items", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        if (groceryList.getItems().isEmpty()) {
            System.out.println("Error: Grocery list items is empty");
            Toast.makeText(this, "Error: Grocery list has no items", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        System.out.println("Searching for item with ID: " + itemId + " in " + groceryList.getItems().size() + " items");
        
        // Find the item in the grocery list
        for (GroceryItem item : groceryList.getItems()) {
            if (item == null) {
                System.out.println("Warning: Found null item in grocery list");
                continue;
            }
            
            System.out.println("Checking item: " + item.getName() + " with ID: " + item.getId());
            if (item.getId() != null && item.getId().equals(itemId)) {
                System.out.println("Found matching item: " + item.getName());
                groceryItem = item;
                updateUI();
                return;
            }
        }
        
        // If we get here, the item wasn't found
        System.out.println("Item not found in grocery list");
        Toast.makeText(this, "Error: Item not found in grocery list", Toast.LENGTH_SHORT).show();
        finish();
    }
    
    private void updateUI() {
        if (groceryItem == null) return;
        
        // Set basic item details
        tvItemName.setText(groceryItem.getName());
        tvCategory.setText("Category: " + groceryItem.getCategory());
        tvQuantity.setText(groceryItem.getQuantity() + " " + groceryItem.getUnit());
        
        // Set price if available
        if (groceryItem.getPrice() > 0) {
            tvPrice.setText(String.format("$%.2f", groceryItem.getPrice()));
        } else {
            tvPrice.setText("Not set");
        }
        
        // Set purchased status
        cbItemPurchased.setChecked(groceryItem.isPurchased());
        
        // Show/hide store information
        if (groceryItem.hasStore()) {
            cardStore.setVisibility(View.VISIBLE);
            tvStoreName.setText(groceryItem.getStoreName());
        } else {
            cardStore.setVisibility(View.GONE);
        }
        
        // Show/hide delegation information
        if (groceryItem.getDelegatedTo() != null && !groceryItem.getDelegatedTo().isEmpty()) {
            cardDelegation.setVisibility(View.VISIBLE);
            tvDelegatedTo.setText("Delegated to: " + groceryItem.getDelegatedTo());
        } else {
            cardDelegation.setVisibility(View.GONE);
        }
        
        // Show/hide notes
        if (groceryItem.getNote() != null && !groceryItem.getNote().isEmpty()) {
            cardNotes.setVisibility(View.VISIBLE);
            tvNotes.setText(groceryItem.getNote());
        } else {
            cardNotes.setVisibility(View.GONE);
        }
    }
    
    private void setupListeners() {
        // Purchased checkbox
        cbItemPurchased.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (groceryItem != null) {
                groceryItem.setPurchased(isChecked);
                updateGroceryItem();
            }
        });
        
        // Find store button
        btnFindStore.setOnClickListener(v -> {
            if (groceryItem != null && groceryItem.hasStore()) {
                // Open map with store location
                Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(groceryItem.getStoreName()));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    Toast.makeText(this, "No map application found", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // Contact button
        btnContact.setOnClickListener(v -> {
            if (groceryItem != null && groceryItem.getDelegatedTo() != null) {
                // Try to determine if delegatedTo is a phone number or email
                String delegatedTo = groceryItem.getDelegatedTo();
                if (delegatedTo.matches(".*\\d.*")) {
                    // Contains digits, likely a phone number
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + delegatedTo));
                    startActivity(intent);
                } else if (delegatedTo.contains("@")) {
                    // Contains @, likely an email
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:" + delegatedTo));
                    intent.putExtra(Intent.EXTRA_SUBJECT, "About grocery item: " + groceryItem.getName());
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Contact information not recognized", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // Edit button
        btnEdit.setOnClickListener(v -> {
            if (groceryItem != null) {
                // Show edit dialog directly in this activity
                showEditItemDialog();
            }
        });
        
        // Delete button
        btnDelete.setOnClickListener(v -> {
            if (groceryItem != null) {
                showDeleteConfirmationDialog();
            }
        });
    }
    
    private void showEditItemDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_grocery_item, null);
        
        // Find views in the dialog
        androidx.appcompat.widget.AppCompatEditText etItemName = dialogView.findViewById(R.id.etDialogItemName);
        androidx.appcompat.widget.AppCompatEditText etAmount = dialogView.findViewById(R.id.etDialogAmount);
        androidx.appcompat.widget.AppCompatAutoCompleteTextView actvUnit = dialogView.findViewById(R.id.actvDialogUnit);
        androidx.appcompat.widget.AppCompatAutoCompleteTextView actvCategory = dialogView.findViewById(R.id.actvDialogCategory);
        androidx.appcompat.widget.AppCompatEditText etPrice = dialogView.findViewById(R.id.etDialogPrice);
        android.widget.Button btnCancel = dialogView.findViewById(R.id.btnDialogCancel);
        android.widget.Button btnSave = dialogView.findViewById(R.id.btnDialogSave);
        
        // Set current values
        etItemName.setText(groceryItem.getName());
        etAmount.setText(String.valueOf(groceryItem.getQuantity()));
        actvUnit.setText(groceryItem.getUnit());
        actvCategory.setText(groceryItem.getCategory());
        etPrice.setText(groceryItem.getPrice() > 0 ? String.valueOf(groceryItem.getPrice()) : "");
        
        // Create and show the dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit Item")
                .setView(dialogView)
                .create();
        
        // Set up button listeners
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnSave.setOnClickListener(v -> {
            // Get values from dialog
            String name = etItemName.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();
            String unit = actvUnit.getText().toString().trim();
            String category = actvCategory.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();
            
            // Validate inputs
            if (name.isEmpty()) {
                etItemName.setError("Name is required");
                return;
            }
            
            if (amountStr.isEmpty()) {
                etAmount.setError("Amount is required");
                return;
            }
            
            if (unit.isEmpty()) {
                actvUnit.setError("Unit is required");
                return;
            }
            
            if (category.isEmpty()) {
                actvCategory.setError("Category is required");
                return;
            }
            
            // Parse numeric values
            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                etAmount.setError("Invalid amount");
                return;
            }
            
            double price = 0.0;
            if (!priceStr.isEmpty()) {
                try {
                    price = Double.parseDouble(priceStr);
                } catch (NumberFormatException e) {
                    etPrice.setError("Invalid price");
                    return;
                }
            }
            
            // Update grocery item
            groceryItem.setName(name);
            groceryItem.setQuantity(amount);
            groceryItem.setUnit(unit);
            groceryItem.setCategory(category);
            groceryItem.setPrice(price);
            
            // Update UI and save changes
            updateUI();
            updateGroceryItem();
            
            // Dismiss dialog
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Delete", (dialog, which) -> deleteGroceryItem())
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void deleteGroceryItem() {
        if (groceryItem == null || groceryList == null) return;
        
        // Remove the item from the grocery list
        groceryList.removeItem(groceryItem);
        
        // Save the updated grocery list
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firebaseHelper.saveGroceryList(groceryList, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                Toast.makeText(GroceryItemDetailActivity.this, 
                        "Item deleted successfully", Toast.LENGTH_SHORT).show();
                
                // Set result with refresh flag to ensure GroceryListActivity is refreshed
                Intent resultIntent = new Intent();
                resultIntent.putExtra("refresh_grocery_list", true);
                setResult(RESULT_OK, resultIntent);
                
                finish();
            }
            
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(GroceryItemDetailActivity.this, 
                        "Error deleting item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateGroceryItem() {
        if (groceryItem == null || groceryList == null) return;
        
        // Update the item in the grocery list
        for (int i = 0; i < groceryList.getItems().size(); i++) {
            GroceryItem item = groceryList.getItems().get(i);
            if (item.getId() != null && item.getId().equals(groceryItem.getId())) {
                groceryList.getItems().set(i, groceryItem);
                break;
            }
        }
        
        // Save the updated grocery list
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firebaseHelper.saveGroceryList(groceryList, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                Toast.makeText(GroceryItemDetailActivity.this, 
                        "Item updated successfully", Toast.LENGTH_SHORT).show();
                
                // Set result with refresh flag to ensure GroceryListActivity is refreshed
                Intent resultIntent = new Intent();
                resultIntent.putExtra("item_id", groceryItem.getId());
                resultIntent.putExtra("refresh_grocery_list", true);
                setResult(RESULT_OK, resultIntent);
            }
            
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(GroceryItemDetailActivity.this, 
                        "Error updating item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                // Revert UI changes
                updateUI();
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning from edit
        if (itemId != null && !itemId.isEmpty()) {
            loadGroceryList();
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