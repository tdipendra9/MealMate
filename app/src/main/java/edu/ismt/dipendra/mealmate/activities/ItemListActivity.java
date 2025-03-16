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
import edu.ismt.dipendra.mealmate.adapters.ItemAdapter;
import edu.ismt.dipendra.mealmate.firebase.FirebaseHelper;
import edu.ismt.dipendra.mealmate.model.Item;

public class ItemListActivity extends AppCompatActivity implements ItemAdapter.OnItemClickListener {

    private static final int REQUEST_ADD_ITEM = 1;
    private static final int REQUEST_EDIT_ITEM = 2;

    private MaterialToolbar toolbar;
    private RecyclerView recyclerViewItems;
    private FloatingActionButton fabAddItem;
    private CircularProgressIndicator progressIndicator;
    private TextView textViewEmpty;

    private FirebaseHelper firebaseHelper;
    private ItemAdapter itemAdapter;
    private List<Item> itemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_list);

        initViews();
        setupToolbar();
        initObjects();
        setupRecyclerView();
        loadItems();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerViewItems = findViewById(R.id.recyclerViewItems);
        fabAddItem = findViewById(R.id.fabAddItem);
        progressIndicator = findViewById(R.id.progressIndicator);
        textViewEmpty = findViewById(R.id.textViewEmpty);

        fabAddItem.setOnClickListener(v -> {
            Intent intent = new Intent(ItemListActivity.this, ItemFormActivity.class);
            startActivityForResult(intent, REQUEST_ADD_ITEM);
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
        itemList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        recyclerViewItems.setLayoutManager(new LinearLayoutManager(this));
        itemAdapter = new ItemAdapter(this, itemList, this);
        recyclerViewItems.setAdapter(itemAdapter);
    }

    private void loadItems() {
        progressIndicator.setVisibility(View.VISIBLE);
        
        // Don't hide the recyclerView here, only show empty text if there are no items
        // recyclerViewItems.setVisibility(View.GONE);
        textViewEmpty.setVisibility(View.GONE);

        firebaseHelper.getUserItems(new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                progressIndicator.setVisibility(View.GONE);
                
                if (result instanceof List) {
                    List<Item> newItems = (List<Item>) result;
                    
                    // Update the class field
                    itemList.clear();
                    itemList.addAll(newItems);
                    
                    // Update the adapter with the new items
                    itemAdapter.updateItems(itemList);
                    
                    // Update visibility based on whether there are items
                    if (itemList.isEmpty()) {
                        textViewEmpty.setVisibility(View.VISIBLE);
                        recyclerViewItems.setVisibility(View.GONE);
                    } else {
                        textViewEmpty.setVisibility(View.GONE);
                        recyclerViewItems.setVisibility(View.VISIBLE);
                    }
                    
                    // Log the items for debugging
                    for (Item item : itemList) {
                        Log.d("ItemListActivity", "Item: " + item.getId() + " - " + item.getName());
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressIndicator.setVisibility(View.GONE);
                Toast.makeText(ItemListActivity.this, 
                        getString(R.string.error_load_items, e.getMessage()), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onEditClick(Item item) {
        Intent intent = new Intent(ItemListActivity.this, ItemFormActivity.class);
        intent.putExtra("item_id", item.getId());
        intent.putExtra("item_name", item.getName());
        intent.putExtra("item_description", item.getDescription());
        intent.putExtra("item_price", item.getPrice());
        intent.putExtra("item_purchased", item.isPurchased());
        intent.putExtra("item_category", item.getCategory());
        intent.putExtra("item_quantity", item.getQuantity());
        
        // Pass the creation date if available
        if (item.getCreatedAt() != null) {
            intent.putExtra("item_created_at", item.getCreatedAt().getTime());
        }
        
        startActivityForResult(intent, REQUEST_EDIT_ITEM);
    }

    @Override
    public void onDeleteClick(Item item) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_item)
                .setMessage(R.string.delete_confirmation)
                .setPositiveButton(R.string.yes, (dialog, which) -> deleteItem(item))
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void deleteItem(Item item) {
        progressIndicator.setVisibility(View.VISIBLE);
        
        firebaseHelper.deleteItem(item.getId(), new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                progressIndicator.setVisibility(View.GONE);
                Toast.makeText(ItemListActivity.this, R.string.item_deleted, Toast.LENGTH_SHORT).show();
                // No need to reload items, the snapshot listener will handle it
            }

            @Override
            public void onFailure(Exception e) {
                progressIndicator.setVisibility(View.GONE);
                Toast.makeText(ItemListActivity.this, 
                        getString(R.string.error_delete_item, e.getMessage()), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onPurchasedStatusChanged(Item item, boolean isChecked) {
        progressIndicator.setVisibility(View.VISIBLE);
        
        // Create a copy of the item to avoid modifying the original before saving
        Item updatedItem = new Item();
        updatedItem.setId(item.getId());
        updatedItem.setName(item.getName());
        updatedItem.setDescription(item.getDescription());
        updatedItem.setPrice(item.getPrice());
        updatedItem.setPurchased(isChecked);
        updatedItem.setCreatedAt(item.getCreatedAt());
        updatedItem.setUserId(item.getUserId());
        updatedItem.setCategory(item.getCategory());
        updatedItem.setQuantity(item.getQuantity());
        
        firebaseHelper.updateItem(updatedItem, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                progressIndicator.setVisibility(View.GONE);
                Toast.makeText(ItemListActivity.this, R.string.item_purchased_status_changed, Toast.LENGTH_SHORT).show();
                // No need to reload items, the snapshot listener will handle it
            }

            @Override
            public void onFailure(Exception e) {
                progressIndicator.setVisibility(View.GONE);
                Toast.makeText(ItemListActivity.this, 
                        getString(R.string.error_update_item, e.getMessage()), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onItemClick(Item item) {
        // Navigate to ItemDetailActivity
        Intent intent = new Intent(ItemListActivity.this, ItemDetailActivity.class);
        intent.putExtra("item_id", item.getId());
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            // Get the item ID from the result
            String itemId = data.getStringExtra("item_id");
            if (itemId != null && !itemId.isEmpty()) {
                Log.d("ItemListActivity", "Received item ID: " + itemId);
            }
            
            // Make sure the recyclerView is visible
            recyclerViewItems.setVisibility(View.VISIBLE);
            textViewEmpty.setVisibility(View.GONE);
            
            // Force a refresh of the list
            loadItems();
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