package edu.ismt.dipendra.mealmate.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.adapters.GroceryCategoryAdapter;
import edu.ismt.dipendra.mealmate.adapters.GroceryCategoryAdapter.OnGesturePerformedListener;
import edu.ismt.dipendra.mealmate.adapters.GroceryCategoryAdapter.OnItemClickListener;
import edu.ismt.dipendra.mealmate.firebase.FirebaseHelper;
import edu.ismt.dipendra.mealmate.model.GroceryItem;
import edu.ismt.dipendra.mealmate.model.GroceryList;

public class GroceryListActivity extends AppCompatActivity implements SensorEventListener, OnGesturePerformedListener, OnItemClickListener {

    private static final int REQUEST_ITEM_DETAIL = 100;
    
    private RecyclerView rvGroceryCategories;
    private ExtendedFloatingActionButton fabAddItem;
    private CircularProgressIndicator progressIndicator;
    private TextView tvTotalPrice;
    private MaterialButtonToggleGroup toggleGroup;

    private FirebaseHelper firebaseHelper;
    private GroceryList groceryList;
    private GroceryCategoryAdapter adapter;
    private String currentSortOrder = SORT_CATEGORY;
    private String currentFilter = FILTER_ALL;
    
    // Categories
    private static final String CATEGORY_VEGETABLES = "Vegetables";
    private static final String CATEGORY_GRAINS = "Grains";
    private static final String CATEGORY_PROTEINS = "Proteins";
    private static final String CATEGORY_FRUITS = "Fruits";
    private static final String CATEGORY_DAIRY = "Dairy";
    private static final String CATEGORY_BEVERAGES = "Beverages";
    private static final String CATEGORY_SNACKS = "Snacks";
    private static final String CATEGORY_CONDIMENTS = "Condiments";
    private static final String CATEGORY_BAKING = "Baking";
    private static final String CATEGORY_FROZEN = "Frozen";
    private static final String CATEGORY_CANNED = "Canned";
    private static final String CATEGORY_OTHERS = "Others";
    
    // Units for dropdown
    private static final String[] UNITS = new String[] {
            "piece", "kg", "g", "lb", "oz", "cup", "tbsp", "tsp", "ml", "l", "bunch", "can", "box", "package"
    };
    
    // Categories for dropdown
    private static final String[] CATEGORIES = new String[] {
            CATEGORY_VEGETABLES, CATEGORY_FRUITS, CATEGORY_GRAINS, CATEGORY_PROTEINS, 
            CATEGORY_DAIRY, CATEGORY_BEVERAGES, CATEGORY_SNACKS, CATEGORY_CONDIMENTS, 
            CATEGORY_BAKING, CATEGORY_FROZEN, CATEGORY_CANNED, CATEGORY_OTHERS
    };

    // Constants for filter types
    private static final String FILTER_ALL = "all";
    private static final String FILTER_PURCHASED = "purchased";
    private static final String FILTER_NON_PURCHASED = "non-purchased";
    
    // Constants for sort orders
    private static final String SORT_CATEGORY = "category";
    private static final String SORT_NAME = "name";
    private static final String SORT_PRICE = "price";

    // Shake detection
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private static final float SHAKE_THRESHOLD = 12.0f;
    private static final int MIN_TIME_BETWEEN_SHAKES = 1000; // in milliseconds
    private long lastShakeTime = 0;
    private float lastX = 0;
    private float lastY = 0;
    private float lastZ = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grocery_list);
        
        firebaseHelper = FirebaseHelper.getInstance();
        
        // Initialize shake detection
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        
        initViews();
        setupToolbar();
        initGroceryList();
        setupListeners();
        setupFilters();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Register shake detector
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
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
                    
                    // Handle shake event - show dialog to clear purchased items
                    if (hasPurchasedItems()) {
                        runOnUiThread(this::showClearPurchasedItemsDialog);
        } else {
                        runOnUiThread(() -> Toast.makeText(this, 
                                "No purchased items to clear", Toast.LENGTH_SHORT).show());
                    }
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
    
    private boolean hasPurchasedItems() {
        if (groceryList != null) {
            for (GroceryItem item : groceryList.getItems()) {
                if (item.isPurchased()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private void showClearPurchasedItemsDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Clear Purchased Items")
                .setMessage("Shake detected! Do you want to remove all purchased items?")
                .setPositiveButton("Yes", (dialog, which) -> clearCompletedItems())
                .setNegativeButton("No", null)
                .show();
    }

    private void setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.grocery_list);
        }
    }
    
    private void initViews() {
        rvGroceryCategories = findViewById(R.id.rvGroceryCategories);
        fabAddItem = findViewById(R.id.fabAddItem);
        progressIndicator = findViewById(R.id.progressIndicator);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        toggleGroup = findViewById(R.id.toggleGroup);
        
        rvGroceryCategories.setLayoutManager(new LinearLayoutManager(this));
    }
    
    @SuppressLint("NonConstantResourceId")
    private void setupFilters() {
        // Set default selection to All
        toggleGroup.check(R.id.btnAll);
        
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnAll) {
                    currentFilter = FILTER_ALL;
                } else if (checkedId == R.id.btnPurchased) {
                    currentFilter = FILTER_PURCHASED;
                } else if (checkedId == R.id.btnNonPurchased) {
                    currentFilter = FILTER_NON_PURCHASED;
                }
                
                // Apply the filter
                filterItems(currentFilter);
                
                // Log the current filter for debugging
                System.out.println("Filter changed to: " + currentFilter);
            }
        });
    }
    
    private void filterItems(String filter) {
        if (adapter != null) {
            // Set the filter on the adapter
            adapter.setFilter(filter);
            
            // Update the total price
            updateTotalPrice();
            
            // Show a toast message about the current view
            String message = "";
            switch (filter) {
                case FILTER_PURCHASED:
                    message = "Showing purchased items";
                    break;
                case FILTER_NON_PURCHASED:
                    message = "Showing items to buy";
                    break;
                case FILTER_ALL:
                    message = "Showing all items";
                    break;
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_grocery_list, menu);
        return true;
    }
    
            @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.action_share) {
            shareGroceryList();
            return true;
        } else if (itemId == R.id.action_delegate) {
            showDelegateDialog();
            return true;
        } else if (itemId == R.id.sort_category) {
            currentSortOrder = SORT_CATEGORY;
            sortItems();
            return true;
        } else if (itemId == R.id.sort_name) {
            currentSortOrder = SORT_NAME;
            sortItems();
            return true;
        } else if (itemId == R.id.sort_price) {
            currentSortOrder = SORT_PRICE;
            sortItems();
            return true;
        } else if (itemId == R.id.action_clear_completed) {
            clearCompletedItems();
            return true;
        } else if (itemId == R.id.action_export) {
            exportGroceryList();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void sortItems() {
        if (groceryList != null) {
            List<GroceryItem> items = groceryList.getAllItems();
            
            switch (currentSortOrder) {
                case SORT_NAME:
                    Collections.sort(items, (a, b) -> a.getName().compareTo(b.getName()));
                    break;
                case SORT_PRICE:
                    Collections.sort(items, (a, b) -> Double.compare(b.getPrice(), a.getPrice()));
                    break;
                default: // category
                    Collections.sort(items, (a, b) -> a.getCategory().compareTo(b.getCategory()));
                    break;
            }
            
            groceryList.updateItems(items);
            setupAdapter();
        }
    }
    
    private void shareGroceryList() {
        StringBuilder shareText = new StringBuilder("My Grocery List:\n\n");
        for (GroceryItem item : groceryList.getAllItems()) {
            shareText.append("• ").append(item.getName())
                    .append(" (").append(item.getQuantity())
                    .append(" ").append(item.getUnit()).append(")\n");
        }
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Grocery List");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
        startActivity(Intent.createChooser(shareIntent, "Share Grocery List"));
    }
    
    private void showDelegateDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_delegate_list, null);
        RadioGroup rgShareMethod = dialogView.findViewById(R.id.rgShareMethod);
        TextInputLayout tilContact = dialogView.findViewById(R.id.tilContact);
        TextInputEditText etContact = dialogView.findViewById(R.id.etContact);
        CheckBox cbIncludePurchased = dialogView.findViewById(R.id.cbIncludePurchased);
        
        // Set up listeners for radio buttons to change input type
        rgShareMethod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbSMS) {
                tilContact.setHint("Enter Phone Number");
                tilContact.setStartIconDrawable(android.R.drawable.ic_menu_call);
                etContact.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
            } else if (checkedId == R.id.rbEmail) {
                tilContact.setHint("Enter Email Address");
                tilContact.setStartIconDrawable(android.R.drawable.ic_dialog_email);
                etContact.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            } else {
                tilContact.setHint("Share with other apps");
                tilContact.setStartIconDrawable(android.R.drawable.ic_menu_share);
                etContact.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
                etContact.setText(""); // Clear the text as it's not needed for other apps
                etContact.setEnabled(false);
            }
        });
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Share Grocery List")
                .setView(dialogView)
                .setPositiveButton("Share", (dialog, which) -> {
                    int selectedId = rgShareMethod.getCheckedRadioButtonId();
                    String contactInfo = etContact.getText().toString().trim();
                    boolean includePurchased = cbIncludePurchased.isChecked();
                    
                    if (selectedId == R.id.rbSMS && !contactInfo.isEmpty()) {
                        shareBySMS(contactInfo, includePurchased);
                    } else if (selectedId == R.id.rbEmail && !contactInfo.isEmpty()) {
                        shareByEmail(contactInfo, includePurchased);
                    } else if (selectedId == R.id.rbOther) {
                        shareWithOtherApps(includePurchased);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void shareBySMS(String phoneNumber, boolean includePurchased) {
        progressIndicator.setVisibility(View.VISIBLE);
        
        // Format the grocery list as a text message
        StringBuilder message = new StringBuilder("Grocery List:\n\n");
        
        // Group items by category
        Map<String, List<GroceryItem>> categorizedItems = new HashMap<>();
        for (GroceryItem item : groceryList.getItems()) {
            // Skip purchased items if not included
            if (item.isPurchased() && !includePurchased) {
                continue;
            }
            
            if (!categorizedItems.containsKey(item.getCategory())) {
                categorizedItems.put(item.getCategory(), new ArrayList<>());
            }
            categorizedItems.get(item.getCategory()).add(item);
        }
        
        // Build the message with categories and items
        for (Map.Entry<String, List<GroceryItem>> entry : categorizedItems.entrySet()) {
            message.append("📋 ").append(entry.getKey()).append(":\n");
            for (GroceryItem item : entry.getValue()) {
                message.append("• ").append(item.getName())
                      .append(" (").append(item.getQuantity()).append(" ").append(item.getUnit()).append(")");
                
                if (item.getPrice() > 0) {
                    message.append(" - $").append(String.format("%.2f", item.getPrice()));
                }
                
                message.append("\n");
            }
            message.append("\n");
        }
        
        // Send SMS
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:" + phoneNumber));
        intent.putExtra("sms_body", message.toString());
        
        progressIndicator.setVisibility(View.GONE);
        
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void shareByEmail(String email, boolean includePurchased) {
        progressIndicator.setVisibility(View.VISIBLE);
        
        // Format the grocery list for email
        StringBuilder message = new StringBuilder("Grocery List:\n\n");
        
        // Group items by category
        Map<String, List<GroceryItem>> categorizedItems = new HashMap<>();
        for (GroceryItem item : groceryList.getItems()) {
            // Skip purchased items if not included
            if (item.isPurchased() && !includePurchased) {
                continue;
            }
            
            if (!categorizedItems.containsKey(item.getCategory())) {
                categorizedItems.put(item.getCategory(), new ArrayList<>());
            }
            categorizedItems.get(item.getCategory()).add(item);
        }
        
        // Build the message with categories and items
        for (Map.Entry<String, List<GroceryItem>> entry : categorizedItems.entrySet()) {
            message.append(entry.getKey()).append(":\n");
            for (GroceryItem item : entry.getValue()) {
                message.append("- ").append(item.getName())
                      .append(" (").append(item.getQuantity()).append(" ").append(item.getUnit()).append(")");
                
                if (item.getPrice() > 0) {
                    message.append(" - $").append(String.format("%.2f", item.getPrice()));
                }
                
                message.append("\n");
            }
            message.append("\n");
        }
        
        // Send email
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + email));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Grocery List");
        intent.putExtra(Intent.EXTRA_TEXT, message.toString());
        
        progressIndicator.setVisibility(View.GONE);
        
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void shareWithOtherApps(boolean includePurchased) {
        progressIndicator.setVisibility(View.VISIBLE);
        
        // Format the grocery list for sharing
        StringBuilder message = new StringBuilder("Grocery List:\n\n");
        
        // Group items by category
        Map<String, List<GroceryItem>> categorizedItems = new HashMap<>();
        for (GroceryItem item : groceryList.getItems()) {
            // Skip purchased items if not included
            if (item.isPurchased() && !includePurchased) {
                continue;
            }
            
            if (!categorizedItems.containsKey(item.getCategory())) {
                categorizedItems.put(item.getCategory(), new ArrayList<>());
            }
            categorizedItems.get(item.getCategory()).add(item);
        }
        
        // Build the message with categories and items
        for (Map.Entry<String, List<GroceryItem>> entry : categorizedItems.entrySet()) {
            message.append(entry.getKey()).append(":\n");
            for (GroceryItem item : entry.getValue()) {
                message.append("- ").append(item.getName())
                      .append(" (").append(item.getQuantity()).append(" ").append(item.getUnit()).append(")");
                
                if (item.getPrice() > 0) {
                    message.append(" - $").append(String.format("%.2f", item.getPrice()));
                }
                
                message.append("\n");
            }
            message.append("\n");
        }
        
        // Share with other apps
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Grocery List");
        intent.putExtra(Intent.EXTRA_TEXT, message.toString());
        
        progressIndicator.setVisibility(View.GONE);
        
        try {
            startActivity(Intent.createChooser(intent, "Share Grocery List via"));
        } catch (Exception e) {
            Toast.makeText(this, "Failed to share: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void clearCompletedItems() {
        // Check if there are any purchased items to clear
        int purchasedCount = 0;
        for (GroceryItem item : groceryList.getItems()) {
            if (item.isPurchased()) {
                purchasedCount++;
            }
        }
        
        if (purchasedCount == 0) {
            Toast.makeText(this, "No purchased items to clear", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Make a final copy of purchasedCount for use in the inner class
        final int finalPurchasedCount = purchasedCount;
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Clear Purchased Items")
                .setMessage("Are you sure you want to remove all " + finalPurchasedCount + " purchased items?")
                .setPositiveButton("Clear", (dialog, which) -> {
                    // Show progress indicator
                    progressIndicator.setVisibility(View.VISIBLE);
                    
                    // Remove all purchased items
                    groceryList.removePurchasedItems();
                    
                    // Save to Firebase
                    firebaseHelper.saveGroceryList(groceryList, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                            progressIndicator.setVisibility(View.GONE);
                            setupAdapter();
                            updateTotalPrice();
                            
                            // Show success message with count
                            String message = finalPurchasedCount + " purchased " + 
                                    (finalPurchasedCount == 1 ? "item" : "items") + " removed";
                            Toast.makeText(GroceryListActivity.this, message, Toast.LENGTH_SHORT).show();
                            
                            // Provide haptic feedback
                            provideHapticFeedback(200);
            }

            @Override
            public void onFailure(Exception e) {
                            progressIndicator.setVisibility(View.GONE);
                Toast.makeText(GroceryListActivity.this,
                                    "Failed to remove purchased items", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void exportGroceryList() {
        try {
            File file = new File(getExternalFilesDir(null), "grocery_list.csv");
            FileWriter writer = new FileWriter(file);
            writer.append("Name,Category,Quantity,Unit,Price,Completed\n");
            
            for (GroceryItem item : groceryList.getAllItems()) {
                writer.append(String.format("%s,%s,%s,%s,%.2f,%s\n",
                        item.getName(),
                        item.getCategory(),
                        item.getQuantity(),
                        item.getUnit(),
                        item.getPrice(),
                        item.isCompleted()));
            }
            
            writer.flush();
            writer.close();
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Grocery List Export");
            shareIntent.putExtra(Intent.EXTRA_STREAM, 
                    FileProvider.getUriForFile(this, 
                            "edu.ismt.dipendra.mealmate.fileprovider", file));
            startActivity(Intent.createChooser(shareIntent, "Export Grocery List"));
            
        } catch (IOException e) {
            Toast.makeText(this, "Failed to export list", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    
    private void initGroceryList() {
        // Show progress indicator
        progressIndicator.setVisibility(View.VISIBLE);
        
        // Create a new grocery list
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        groceryList = new GroceryList("My Grocery List", userId);
        
        // Add sample items to the grocery list
        addSampleItems();
        
        // Save the grocery list to Firebase first
        System.out.println("Saving new grocery list to Firebase");
        firebaseHelper.saveGroceryList(groceryList, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                System.out.println("Successfully saved new grocery list to Firebase");
                progressIndicator.setVisibility(View.GONE);
                
                // Setup adapter
                setupAdapter();
                
                // Update total price
                updateTotalPrice();
            }
            
            @Override
            public void onFailure(Exception e) {
                System.out.println("Failed to save new grocery list to Firebase: " + e.getMessage());
                e.printStackTrace();
                progressIndicator.setVisibility(View.GONE);
                
                // Even if saving fails, still setup the UI with local data
                setupAdapter();
                updateTotalPrice();
                
                Toast.makeText(GroceryListActivity.this, 
                        "Warning: Failed to save grocery list to Firebase", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void addSampleItems() {
        // Add vegetables
        groceryList.addItem(new GroceryItem("Carrots", CATEGORY_VEGETABLES, 2, "bunch", 10));
        groceryList.addItem(new GroceryItem("Broccoli", CATEGORY_VEGETABLES, 1, "head",12));
        groceryList.addItem(new GroceryItem("Spinach", CATEGORY_VEGETABLES, 200, "g",5));
        groceryList.addItem(new GroceryItem("Bell Peppers", CATEGORY_VEGETABLES, 3, "piece",4));
        groceryList.addItem(new GroceryItem("Tomatoes", CATEGORY_VEGETABLES, 5, "piece",2));
        
        // Add fruits
        groceryList.addItem(new GroceryItem("Apples", CATEGORY_FRUITS, 6, "piece",3));
        groceryList.addItem(new GroceryItem("Bananas", CATEGORY_FRUITS, 1, "bunch",6));
        groceryList.addItem(new GroceryItem("Oranges", CATEGORY_FRUITS, 4, "piece",2));
        groceryList.addItem(new GroceryItem("Strawberries", CATEGORY_FRUITS, 1, "package",4));
        groceryList.addItem(new GroceryItem("Grapes", CATEGORY_FRUITS, 500, "g",5));
        
        // Add grains
        groceryList.addItem(new GroceryItem("Rice", CATEGORY_GRAINS, 1, "kg",5));
        groceryList.addItem(new GroceryItem("Pasta", CATEGORY_GRAINS, 500, "g",5));
        groceryList.addItem(new GroceryItem("Quinoa", CATEGORY_GRAINS, 250, "g",6));
        groceryList.addItem(new GroceryItem("Oats", CATEGORY_GRAINS, 1, "package",7));
        groceryList.addItem(new GroceryItem("Bread", CATEGORY_GRAINS, 1, "loaf",9));
        
        // Add proteins
        groceryList.addItem(new GroceryItem("Chicken Breast", CATEGORY_PROTEINS, 500, "g",7));
        groceryList.addItem(new GroceryItem("Ground Beef", CATEGORY_PROTEINS, 1, "lb",8));
        groceryList.addItem(new GroceryItem("Tofu", CATEGORY_PROTEINS, 1, "package",9));
        groceryList.addItem(new GroceryItem("Eggs", CATEGORY_PROTEINS, 12, "piece",8));
        groceryList.addItem(new GroceryItem("Salmon", CATEGORY_PROTEINS, 2, "fillet",9));
        
        // Add dairy
        groceryList.addItem(new GroceryItem("Milk", CATEGORY_DAIRY, 1, "l",4));
        groceryList.addItem(new GroceryItem("Cheese", CATEGORY_DAIRY, 200, "g",5));
        groceryList.addItem(new GroceryItem("Yogurt", CATEGORY_DAIRY, 500, "g",2));
        groceryList.addItem(new GroceryItem("Butter", CATEGORY_DAIRY, 250, "g",7));
        groceryList.addItem(new GroceryItem("Cream", CATEGORY_DAIRY, 200, "ml",4));
        
        // Add beverages
        groceryList.addItem(new GroceryItem("Coffee", CATEGORY_BEVERAGES, 500, "g",3));
        groceryList.addItem(new GroceryItem("Tea", CATEGORY_BEVERAGES, 20, "bags",1));
        groceryList.addItem(new GroceryItem("Orange Juice", CATEGORY_BEVERAGES, 1, "l",4));
        groceryList.addItem(new GroceryItem("Soda", CATEGORY_BEVERAGES, 2, "l",5));
        groceryList.addItem(new GroceryItem("Water", CATEGORY_BEVERAGES, 6, "bottles",2));
        
        // Add snacks
        groceryList.addItem(new GroceryItem("Potato Chips", CATEGORY_SNACKS, 1, "bag",2));
        groceryList.addItem(new GroceryItem("Popcorn", CATEGORY_SNACKS, 3, "packages",6));
        groceryList.addItem(new GroceryItem("Chocolate", CATEGORY_SNACKS, 2, "bars",7));
        groceryList.addItem(new GroceryItem("Nuts", CATEGORY_SNACKS, 200, "g",5));
        groceryList.addItem(new GroceryItem("Crackers", CATEGORY_SNACKS, 1, "box",4));
        
        // Add condiments
        groceryList.addItem(new GroceryItem("Ketchup", CATEGORY_CONDIMENTS, 1, "bottle",4));
        groceryList.addItem(new GroceryItem("Mustard", CATEGORY_CONDIMENTS, 1, "jar",6));
        groceryList.addItem(new GroceryItem("Mayonnaise", CATEGORY_CONDIMENTS, 1, "jar",6));
        groceryList.addItem(new GroceryItem("Soy Sauce", CATEGORY_CONDIMENTS, 1, "bottle",3));
        groceryList.addItem(new GroceryItem("Hot Sauce", CATEGORY_CONDIMENTS, 1, "bottle",4));
        
        // Add baking
        groceryList.addItem(new GroceryItem("Flour", CATEGORY_BAKING, 1, "kg",6));
        groceryList.addItem(new GroceryItem("Sugar", CATEGORY_BAKING, 500, "g",8));
        groceryList.addItem(new GroceryItem("Baking Powder", CATEGORY_BAKING, 1, "package",1));
        groceryList.addItem(new GroceryItem("Vanilla Extract", CATEGORY_BAKING, 1, "bottle",2));
        groceryList.addItem(new GroceryItem("Chocolate Chips", CATEGORY_BAKING, 200, "g",4));
        
        // Add frozen
        groceryList.addItem(new GroceryItem("Frozen Peas", CATEGORY_FROZEN, 500, "g",6));
        groceryList.addItem(new GroceryItem("Ice Cream", CATEGORY_FROZEN, 1, "tub",7));
        groceryList.addItem(new GroceryItem("Frozen Pizza", CATEGORY_FROZEN, 2, "pieces",8));
        groceryList.addItem(new GroceryItem("Frozen Berries", CATEGORY_FROZEN, 400, "g",5));
        groceryList.addItem(new GroceryItem("Frozen Fish", CATEGORY_FROZEN, 500, "g",4));
        
        // Add canned
        groceryList.addItem(new GroceryItem("Canned Beans", CATEGORY_CANNED, 2, "cans",6));
        groceryList.addItem(new GroceryItem("Canned Tuna", CATEGORY_CANNED, 3, "cans",5));
        groceryList.addItem(new GroceryItem("Canned Corn", CATEGORY_CANNED, 1, "can",2));
        groceryList.addItem(new GroceryItem("Canned Soup", CATEGORY_CANNED, 2, "cans",4));
        groceryList.addItem(new GroceryItem("Canned Tomatoes", CATEGORY_CANNED, 2, "cans",6));
        
        // Add others
        groceryList.addItem(new GroceryItem("Olive Oil", CATEGORY_OTHERS, 1, "bottle",8));
        groceryList.addItem(new GroceryItem("Salt", CATEGORY_OTHERS, 1, "package",8));
        groceryList.addItem(new GroceryItem("Black Pepper", CATEGORY_OTHERS, 1, "package",6));
        groceryList.addItem(new GroceryItem("Garlic", CATEGORY_OTHERS, 1, "head",5));
        groceryList.addItem(new GroceryItem("Honey", CATEGORY_OTHERS, 1, "jar",4));
    }
    
    private void setupAdapter() {
        // Group items by category
        Map<String, List<GroceryItem>> categorizedItems = new HashMap<>();
        for (GroceryItem item : groceryList.getItems()) {
            if (shouldShowItem(item)) {
                if (!categorizedItems.containsKey(item.getCategory())) {
                    categorizedItems.put(item.getCategory(), new ArrayList<>());
                }
                categorizedItems.get(item.getCategory()).add(item);
            }
        }
        
        // Sort categories
        List<String> sortedCategories = new ArrayList<>(categorizedItems.keySet());
        Collections.sort(sortedCategories);
        
        // Create adapter
        adapter = new GroceryCategoryAdapter(this, sortedCategories, categorizedItems, currentFilter);
        adapter.setOnGroceryItemChangeListener(new GroceryCategoryAdapter.OnGroceryItemChangeListener() {
            @Override
            public void onItemPurchasedChanged(GroceryItem item, boolean purchased) {
                // Update all properties related to purchase status
                item.setPurchased(purchased);
                item.setStatus(purchased ? "PURCHASED" : "AVAILABLE");
                item.setCompleted(purchased);
                
                // Update the total price immediately for better user experience
                updateTotalPrice();
                
                // Then update the item in the database
                updateGroceryItem(item);
            }
            
            @Override
            public void onItemDeleted(GroceryItem item) {
                deleteGroceryItem(item);
            }
            
            @Override
            public void onItemEdited(GroceryItem item) {
                showEditItemDialog(item);
            }
        });
        
        // Set gesture listener for haptic feedback
        adapter.setOnGesturePerformedListener(this);
        
        // Set item click listener
        adapter.setOnItemClickListener(this);
        
        rvGroceryCategories.setAdapter(adapter);
    }
    
    private boolean shouldShowItem(GroceryItem item) {
        switch (currentFilter.toLowerCase()) {
            case FILTER_PURCHASED:
                return item.isPurchased();
            case FILTER_NON_PURCHASED:
                return !item.isPurchased();
            default:
                return true;
        }
    }
    
    private void updateGroceryItem(GroceryItem item) {
        progressIndicator.setVisibility(View.VISIBLE);
        
        // Update the item in the list
        if (item.getId() != null) {
            // Find and update the item by ID
            for (int i = 0; i < groceryList.getItems().size(); i++) {
                GroceryItem existingItem = groceryList.getItems().get(i);
                if (existingItem.getId() != null && existingItem.getId().equals(item.getId())) {
                    groceryList.getItems().set(i, item);
                    break;
                }
            }
        } else {
            // If ID is null, find by name and category
            for (int i = 0; i < groceryList.getItems().size(); i++) {
                GroceryItem existingItem = groceryList.getItems().get(i);
                if (existingItem.getName().equals(item.getName()) && 
                    existingItem.getCategory().equals(item.getCategory())) {
                    // Copy the ID from the existing item to prevent future null ID issues
                    item.setId(existingItem.getId());
                    groceryList.getItems().set(i, item);
                    break;
                }
            }
        }
        
        // Log the item state for debugging
        System.out.println("Updating item: " + item.getName() + 
                           ", Purchased: " + item.isPurchased() + 
                           ", Status: " + item.getStatus() + 
                           ", Completed: " + item.isCompleted());
        
        // Save to Firebase
        firebaseHelper.saveGroceryList(groceryList, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                progressIndicator.setVisibility(View.GONE);
                // Update UI based on filter
                setupAdapter();
                // Update total price
                updateTotalPrice();
                // Show a toast message
                Toast.makeText(GroceryListActivity.this,
                               item.isPurchased() ? "Item marked as purchased" : "Item marked as not purchased", 
                        Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onFailure(Exception e) {
                progressIndicator.setVisibility(View.GONE);
                Toast.makeText(GroceryListActivity.this, "Failed to update item", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void deleteGroceryItem(GroceryItem item) {
        progressIndicator.setVisibility(View.VISIBLE);
        
        // Remove the item from the grocery list
        groceryList.removeItem(item);
        
        // Save to Firebase
        firebaseHelper.saveGroceryList(groceryList, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                progressIndicator.setVisibility(View.GONE);
                // Update UI based on filter
                setupAdapter();
            }
            
            @Override
            public void onFailure(Exception e) {
                progressIndicator.setVisibility(View.GONE);
                Toast.makeText(GroceryListActivity.this, "Failed to delete item", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void setupListeners() {
        // Add item button
        fabAddItem.setOnClickListener(v -> showAddItemDialog());
        
        // We'll set the listener in setupAdapter() instead of here
        // since adapter is null at this point
    }
    
    /**
     * Provides haptic feedback for gestures
     * @param duration Duration of vibration in milliseconds
     */
    private void provideHapticFeedback(int duration) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                android.os.VibrationEffect vibrationEffect = 
                        android.os.VibrationEffect.createOneShot(
                                duration, android.os.VibrationEffect.DEFAULT_AMPLITUDE);
                android.os.Vibrator vibrator = 
                        (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null) {
                    vibrator.vibrate(vibrationEffect);
                }
            } else {
                @SuppressWarnings("deprecation")
                android.os.Vibrator vibrator = 
                        (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null) {
                    vibrator.vibrate(duration);
                }
            }
        } catch (Exception e) {
            // Ignore vibration errors
            e.printStackTrace();
        }
    }
    
    private void setupCategorySpinner(AutoCompleteTextView actvCategory) {
        // Create a custom adapter that will always show all items
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<String>(this, 
                android.R.layout.simple_spinner_dropdown_item, CATEGORIES) {
            @Override
            public int getCount() {
                return CATEGORIES.length; // Show all items
            }
            
            @Override
            public String getItem(int position) {
                // Make sure we don't go out of bounds
                if (position >= 0 && position < CATEGORIES.length) {
                    return CATEGORIES[position];
                }
                return "";
            }
        };
        
        // Set the adapter
        actvCategory.setAdapter(categoryAdapter);
        
        // Set threshold to 0 so that the dropdown shows without typing
        actvCategory.setThreshold(0);
        
        // Force dropdown to show all items when clicked
        actvCategory.setOnClickListener(v -> {
            actvCategory.showDropDown();
        });
    }
    
    private void setupUnitSpinner(AutoCompleteTextView actvUnit) {
        // Create a custom adapter that will always show all items
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<String>(this, 
                android.R.layout.simple_spinner_dropdown_item, UNITS) {
            @Override
            public int getCount() {
                return UNITS.length; // Show all items
            }
            
            @Override
            public String getItem(int position) {
                // Make sure we don't go out of bounds
                if (position >= 0 && position < UNITS.length) {
                    return UNITS[position];
                }
                return "";
            }
        };
        
        // Set the adapter
        actvUnit.setAdapter(unitAdapter);
        
        // Set threshold to 0 so that the dropdown shows without typing
        actvUnit.setThreshold(0);
        
        // Force dropdown to show all items when clicked
        actvUnit.setOnClickListener(v -> {
            actvUnit.showDropDown();
        });
    }
    
    private void showAddItemDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_grocery_item, null);
        
        TextInputEditText etItemName = dialogView.findViewById(R.id.etDialogItemName);
        TextInputEditText etAmount = dialogView.findViewById(R.id.etDialogAmount);
        AutoCompleteTextView actvUnit = dialogView.findViewById(R.id.actvDialogUnit);
        AutoCompleteTextView actvCategory = dialogView.findViewById(R.id.actvDialogCategory);
        TextInputEditText etPrice = dialogView.findViewById(R.id.etDialogPrice);
        Button btnCancel = dialogView.findViewById(R.id.btnDialogCancel);
        Button btnAdd = dialogView.findViewById(R.id.btnDialogAdd);
        
        // Setup unit dropdown first
        setupUnitSpinner(actvUnit);
        
        // Setup category dropdown
        setupCategorySpinner(actvCategory);
        
        // Set default values after adapters are set
        actvUnit.post(() -> actvUnit.setText("piece", false));
        actvCategory.post(() -> actvCategory.setText(CATEGORY_OTHERS, false));
        
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnAdd.setOnClickListener(v -> {
            try {
                String name = etItemName.getText() != null ? etItemName.getText().toString().trim() : "";
                String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
                String unit = actvUnit.getText() != null ? actvUnit.getText().toString().trim() : "";
                String category = actvCategory.getText() != null ? actvCategory.getText().toString().trim() : "";
                String priceStr = etPrice.getText() != null ? etPrice.getText().toString().trim() : "";
                
                if (name.isEmpty()) {
                    etItemName.setError("Please enter item name");
            return;
        }

                if (amountStr.isEmpty()) {
                    etAmount.setError("Please enter amount");
                    return;
                }
                
                if (unit.isEmpty()) {
                    actvUnit.setError("Please enter unit");
                    return;
                }
                
                if (category.isEmpty()) {
                    actvCategory.setError("Please select a category");
            return;
        }

                double amount;
                try {
                    amount = Double.parseDouble(amountStr);
                } catch (NumberFormatException e) {
                    etAmount.setError("Please enter a valid number");
                    return;
                }
                
                double price = 0.0;
                if (!priceStr.isEmpty()) {
                    try {
                        price = Double.parseDouble(priceStr);
                    } catch (NumberFormatException e) {
                        etPrice.setError("Please enter a valid price");
                        return;
                    }
                }
                
                // Create new grocery item with price
                GroceryItem item = new GroceryItem(name, category, amount, unit, price);
                
                // Add item to grocery list
                groceryList.addItem(item);
                
                // Refresh adapter
                setupAdapter();
                
                // Update total price
                updateTotalPrice();
                
                // Dismiss dialog
                dialog.dismiss();
                
                // Show success message
                Toast.makeText(GroceryListActivity.this, "Item added successfully", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                // Log the error and show a friendly message
                e.printStackTrace();
                Toast.makeText(GroceryListActivity.this, "Error adding item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        dialog.show();
    }
    
    private void showEditItemDialog(GroceryItem item) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_grocery_item, null);
        
        TextInputEditText etItemName = dialogView.findViewById(R.id.etDialogItemName);
        TextInputEditText etAmount = dialogView.findViewById(R.id.etDialogAmount);
        AutoCompleteTextView actvUnit = dialogView.findViewById(R.id.actvDialogUnit);
        AutoCompleteTextView actvCategory = dialogView.findViewById(R.id.actvDialogCategory);
        TextInputEditText etPrice = dialogView.findViewById(R.id.etDialogPrice);
        Button btnCancel = dialogView.findViewById(R.id.btnDialogCancel);
        Button btnSave = dialogView.findViewById(R.id.btnDialogSave);
        
        // Set text values first
        etItemName.setText(item.getName());
        etAmount.setText(String.valueOf(item.getQuantity()));
        etPrice.setText(item.getPrice() > 0 ? String.valueOf(item.getPrice()) : "");
        
        // Setup unit dropdown
        setupUnitSpinner(actvUnit);
        
        // Setup category dropdown
        setupCategorySpinner(actvCategory);
        
        // Set dropdown values after adapters are set
        final String itemUnit = item.getUnit();
        final String itemCategory = item.getCategory();
        
        actvUnit.post(() -> actvUnit.setText(itemUnit, false));
        actvCategory.post(() -> actvCategory.setText(itemCategory, false));
        
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnSave.setOnClickListener(v -> {
            try {
                String name = etItemName.getText() != null ? etItemName.getText().toString().trim() : "";
                String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
                String unit = actvUnit.getText() != null ? actvUnit.getText().toString().trim() : "";
                String category = actvCategory.getText() != null ? actvCategory.getText().toString().trim() : "";
                String priceStr = etPrice.getText() != null ? etPrice.getText().toString().trim() : "";
                
                if (name.isEmpty()) {
                    etItemName.setError("Please enter item name");
                    return;
                }
                
                if (amountStr.isEmpty()) {
                    etAmount.setError("Please enter amount");
                    return;
                }
                
                if (unit.isEmpty()) {
                    actvUnit.setError("Please enter unit");
                    return;
                }
                
                if (category.isEmpty()) {
                    actvCategory.setError("Please select a category");
                    return;
                }
                
                double amount;
                try {
                    amount = Double.parseDouble(amountStr);
                } catch (NumberFormatException e) {
                    etAmount.setError("Please enter a valid number");
                    return;
                }
                
                double price = 0.0;
                if (!priceStr.isEmpty()) {
                    try {
                        price = Double.parseDouble(priceStr);
                    } catch (NumberFormatException e) {
                        etPrice.setError("Please enter a valid price");
                        return;
                    }
                }
                
                // Update item
                item.setName(name);
                item.setQuantity(amount);
                item.setUnit(unit);
                item.setCategory(category);
                item.setPrice(price);
                
                // Refresh adapter
                setupAdapter();
                
                // Update total price
                updateTotalPrice();
                
                // Dismiss dialog
                dialog.dismiss();
                
                // Show success message
                Toast.makeText(GroceryListActivity.this, "Item updated successfully", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                // Log the error and show a friendly message
                e.printStackTrace();
                Toast.makeText(GroceryListActivity.this, "Error updating item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        dialog.show();
    }
    
    private void updateTotalPrice() {
        try {
            double total = 0.0;
            
            // Calculate total price based on current filter
            for (GroceryItem item : groceryList.getItems()) {
                // Only include items that match the current filter
                if (currentFilter.equals(FILTER_PURCHASED) && item.isPurchased()) {
                    total += item.getPrice();
                } else if (currentFilter.equals(FILTER_NON_PURCHASED) && !item.isPurchased()) {
                    total += item.getPrice();
                } else if (currentFilter.equals(FILTER_ALL)) {
                    total += item.getPrice();
                }
            }
            
            // Update total price text with appropriate label
            String totalLabel;
            if (currentFilter.equals(FILTER_PURCHASED)) {
                totalLabel = "Purchased Total:";
            } else if (currentFilter.equals(FILTER_NON_PURCHASED)) {
                totalLabel = "To Buy Total:";
            } else {
                totalLabel = "Total:";
            }
            
            tvTotalPrice.setText(String.format("%s $%.2f", totalLabel, total));
        } catch (Exception e) {
            e.printStackTrace();
            tvTotalPrice.setText("Total: $0.00");
        }
    }

    private void applyFilter(String filter) {
        switch (filter.toLowerCase()) {
            case FILTER_PURCHASED:
                // Handle purchased items
                break;
            case FILTER_NON_PURCHASED:
                // Handle non-purchased items
                break;
            case FILTER_ALL:
            default:
                // Handle all items
                break;
        }
    }

    /**
     * Callback for gesture performed events
     * @param duration Duration of vibration in milliseconds
     */
            @Override
    public void onGesturePerformed(int duration) {
        provideHapticFeedback(duration);
            }

            @Override
    public void onItemClicked(GroceryItem item) {
        // Navigate to the detail view for this item
        Intent intent = new Intent(this, GroceryItemDetailActivity.class);
        intent.putExtra("item_id", item.getId());
        startActivityForResult(intent, REQUEST_ITEM_DETAIL);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_ITEM_DETAIL && resultCode == RESULT_OK) {
            // Check if we need to refresh the grocery list
            boolean refreshList = data != null && data.getBooleanExtra("refresh_grocery_list", false);
            
            if (refreshList) {
                // Log that we're refreshing due to the flag
                Log.d("GroceryListActivity", "Refreshing grocery list due to refresh flag");
            }
            
            // Refresh the grocery list if an item was updated or deleted
            loadGroceryList();
        }
    }

    private void loadGroceryList() {
        // Show progress indicator
        progressIndicator.setVisibility(View.VISIBLE);
        
        // Get the current user ID
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Log for debugging
        System.out.println("Loading grocery list for user: " + userId);
        
        // Load the grocery list from Firebase
        firebaseHelper.getGroceryLists(userId, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                progressIndicator.setVisibility(View.GONE);
                
                if (result instanceof List<?>) {
                    List<?> lists = (List<?>) result;
                    System.out.println("Received " + lists.size() + " grocery lists");
                    
                    if (!lists.isEmpty()) {
                        // Check if the first item is a GroceryList
                        if (lists.get(0) instanceof GroceryList) {
                            // For simplicity, we'll use the first grocery list
                            groceryList = (GroceryList) lists.get(0);
                            System.out.println("Using grocery list: " + groceryList.getName() + 
                                    " with " + (groceryList.getItems() != null ? groceryList.getItems().size() : 0) + " items");
                            
                            // Check if the items list is null or empty
                            if (groceryList.getItems() == null || groceryList.getItems().isEmpty()) {
                                System.out.println("Grocery list has no items, adding sample items");
                                addSampleItems();
                                saveGroceryListToFirebase();
                            }
                            
                            setupAdapter();
                            updateTotalPrice();
                        } else {
                            System.out.println("First item is not a GroceryList: " + lists.get(0).getClass().getName());
                            initGroceryList();
                        }
                    } else {
                        System.out.println("No grocery lists found, initializing a new one");
                        // If no grocery list exists, initialize a new one
                        initGroceryList();
                    }
                } else {
                    System.out.println("Result is not a List: " + (result != null ? result.getClass().getName() : "null"));
                    Toast.makeText(GroceryListActivity.this, 
                            "Error: Grocery list data format incorrect", Toast.LENGTH_SHORT).show();
                    // If data format is incorrect, initialize a new one
                    initGroceryList();
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressIndicator.setVisibility(View.GONE);
                System.out.println("Error loading grocery list: " + e.getMessage());
                e.printStackTrace();
                
                Toast.makeText(GroceryListActivity.this,
                        "Error loading grocery list: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                
                // If we can't load the list, initialize a new one
                initGroceryList();
            }
        });
    }

    private void saveGroceryListToFirebase() {
        System.out.println("Saving grocery list to Firebase");
        firebaseHelper.saveGroceryList(groceryList, new FirebaseHelper.FirebaseCallback() {
    @Override
            public void onSuccess(Object result) {
                System.out.println("Successfully saved grocery list to Firebase");
    }

    @Override
            public void onFailure(Exception e) {
                System.out.println("Failed to save grocery list to Firebase: " + e.getMessage());
                e.printStackTrace();
                Toast.makeText(GroceryListActivity.this, 
                        "Warning: Failed to save grocery list to Firebase", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
