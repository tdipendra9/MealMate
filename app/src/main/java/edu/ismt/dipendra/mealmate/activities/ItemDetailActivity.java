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

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.Locale;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.firebase.FirebaseHelper;
import edu.ismt.dipendra.mealmate.model.Item;

public class ItemDetailActivity extends AppCompatActivity {

    private static final String TAG = "ItemDetailActivity";

    private Toolbar toolbar;
    private CollapsingToolbarLayout collapsingToolbar;
    private ImageView ivItemImage;
    private TextView tvItemName;
    private Chip chipCategory;
    private TextView tvPrice;
    private TextView tvQuantity;
    private TextView tvDescription;
    private TextView tvCreatedDate;
    private SwitchMaterial switchPurchased;
    private MaterialButton btnEdit;
    private MaterialButton btnDelete;
    private FloatingActionButton fabShare;
    private ProgressBar progressBar;

    private FirebaseHelper firebaseHelper;
    private String itemId;
    private Item item;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        // Get item ID from intent
        if (getIntent().hasExtra("item_id")) {
            itemId = getIntent().getStringExtra("item_id");
        } else {
            Toast.makeText(this, "Item ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        
        firebaseHelper = FirebaseHelper.getInstance();
        
        setupListeners();
        loadItemDetails();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        collapsingToolbar = findViewById(R.id.collapsingToolbar);
        ivItemImage = findViewById(R.id.ivItemImage);
        tvItemName = findViewById(R.id.tvItemName);
        chipCategory = findViewById(R.id.chipCategory);
        tvPrice = findViewById(R.id.tvPrice);
        tvQuantity = findViewById(R.id.tvQuantity);
        tvDescription = findViewById(R.id.tvDescription);
        tvCreatedDate = findViewById(R.id.tvCreatedDate);
        switchPurchased = findViewById(R.id.switchPurchased);
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
            Intent intent = new Intent(ItemDetailActivity.this, ItemFormActivity.class);
            intent.putExtra("item_id", itemId);
            intent.putExtra("item_name", item.getName());
            intent.putExtra("item_description", item.getDescription());
            intent.putExtra("item_price", item.getPrice());
            intent.putExtra("item_category", item.getCategory());
            intent.putExtra("item_quantity", item.getQuantity());
            startActivity(intent);
        });
        
        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
        
        switchPurchased.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (item != null && item.isPurchased() != isChecked) {
                updatePurchasedStatus(isChecked);
            }
        });
        
        fabShare.setOnClickListener(v -> shareItem());
    }

    private void loadItemDetails() {
        showLoading(true);
        
        firebaseHelper.getItemById(itemId, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                showLoading(false);
                
                if (result instanceof Item) {
                    item = (Item) result;
                    updateUI();
                }
            }

            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                Toast.makeText(ItemDetailActivity.this, 
                        "Failed to load item details: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void updateUI() {
        // Set item name in both the TextView and CollapsingToolbarLayout
        tvItemName.setText(item.getName());
        collapsingToolbar.setTitle(item.getName());
        
        // Set category
        if (item.getCategory() != null && !item.getCategory().isEmpty()) {
            chipCategory.setText(item.getCategory());
            chipCategory.setVisibility(View.VISIBLE);
        } else {
            chipCategory.setVisibility(View.GONE);
        }
        
        // Set price
        String priceText = String.format(Locale.getDefault(), 
                getString(R.string.price_format), item.getPrice());
        tvPrice.setText(priceText);
        
        // Set quantity
        String quantityText = String.format(Locale.getDefault(), 
                getString(R.string.quantity_format), item.getQuantity());
        tvQuantity.setText(quantityText);
        
        // Set description
        if (item.getDescription() != null && !item.getDescription().isEmpty()) {
            tvDescription.setText(item.getDescription());
        } else {
            tvDescription.setText("No description available");
        }
        
        // Set created date
        if (item.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String dateText = "Added on: " + sdf.format(item.getCreatedAt());
            tvCreatedDate.setText(dateText);
            tvCreatedDate.setVisibility(View.VISIBLE);
        } else {
            tvCreatedDate.setVisibility(View.GONE);
        }
        
        // Set purchased status
        switchPurchased.setChecked(item.isPurchased());
    }

    private void updatePurchasedStatus(boolean isPurchased) {
        showLoading(true);
        
        // Create a copy of the item to avoid modifying the original before saving
        Item updatedItem = new Item();
        updatedItem.setId(item.getId());
        updatedItem.setName(item.getName());
        updatedItem.setDescription(item.getDescription());
        updatedItem.setPrice(item.getPrice());
        updatedItem.setPurchased(isPurchased);
        updatedItem.setCreatedAt(item.getCreatedAt());
        updatedItem.setUserId(item.getUserId());
        updatedItem.setCategory(item.getCategory());
        updatedItem.setQuantity(item.getQuantity());
        
        firebaseHelper.updateItem(updatedItem, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                showLoading(false);
                item.setPurchased(isPurchased); // Update the local item
                Toast.makeText(ItemDetailActivity.this, 
                        R.string.item_purchased_status_changed, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                // Revert the switch if update fails
                switchPurchased.setChecked(!isPurchased);
                Toast.makeText(ItemDetailActivity.this, 
                        "Failed to update status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_item)
                .setMessage(getString(R.string.delete_confirmation))
                .setPositiveButton(R.string.yes, (dialog, which) -> deleteItem())
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void deleteItem() {
        showLoading(true);
        
        firebaseHelper.deleteItem(itemId, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                showLoading(false);
                Toast.makeText(ItemDetailActivity.this, 
                        R.string.item_deleted, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                Toast.makeText(ItemDetailActivity.this, 
                        getString(R.string.error_delete_item, e.getMessage()), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void shareItem() {
        if (item == null) return;
        
        StringBuilder shareText = new StringBuilder();
        shareText.append(item.getName()).append("\n\n");
        
        if (item.getDescription() != null && !item.getDescription().isEmpty()) {
            shareText.append(item.getDescription()).append("\n\n");
        }
        
        // Add price and quantity
        shareText.append("Price: $").append(String.format(Locale.getDefault(), "%.2f", item.getPrice())).append("\n");
        shareText.append("Quantity: ").append(item.getQuantity()).append("\n\n");
        
        // Add category if available
        if (item.getCategory() != null && !item.getCategory().isEmpty()) {
            shareText.append("Category: ").append(item.getCategory()).append("\n\n");
        }
        
        shareText.append("Shared from MealMate App");
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Item: " + item.getName());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
        startActivity(Intent.createChooser(shareIntent, "Share Item"));
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (itemId != null) {
            loadItemDetails();
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