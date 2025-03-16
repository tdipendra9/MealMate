package edu.ismt.dipendra.mealmate.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.adapters.StoreAdapter;
import edu.ismt.dipendra.mealmate.firebase.FirebaseHelper;
import edu.ismt.dipendra.mealmate.model.GeotaggedStore;
import edu.ismt.dipendra.mealmate.utils.GooglePlayServicesHelper;

public class StoreListActivity extends AppCompatActivity implements StoreAdapter.OnStoreClickListener {
    
    private static final String TAG = "StoreListActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    
    private Toolbar toolbar;
    private RecyclerView rvStores;
    private TextView tvNoStores;
    private ProgressBar progressBar;
    private FloatingActionButton fabAddStore;
    
    private FirebaseHelper firebaseHelper;
    private StoreAdapter storeAdapter;
    private List<GeotaggedStore> stores = new ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_list);
        
        // Check if Google Play Services is available before using location services
        if (!GooglePlayServicesHelper.checkPlayServicesWithDialog(this)) {
            // Google Play Services is not available or outdated
            // The helper will show the appropriate dialog
            // We can still continue with app initialization, but location features might not work
        }
        
        firebaseHelper = FirebaseHelper.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        initViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();
        
        // Load stores from Firebase
        loadStores();
    }
    
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        rvStores = findViewById(R.id.rvStores);
        tvNoStores = findViewById(R.id.tvNoStores);
        progressBar = findViewById(R.id.progressBar);
        fabAddStore = findViewById(R.id.fabAddStore);
    }
    
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.store_list_title);
        }
    }
    
    private void setupRecyclerView() {
        storeAdapter = new StoreAdapter(this, stores, this);
        rvStores.setLayoutManager(new LinearLayoutManager(this));
        rvStores.setAdapter(storeAdapter);
    }
    
    private void setupListeners() {
        fabAddStore.setOnClickListener(v -> {
            Intent intent = new Intent(StoreListActivity.this, StoreFormActivity.class);
            startActivity(intent);
        });
    }
    
    private void loadStores() {
        showLoading(true);
        
        firebaseHelper.getUserGeotaggedStores(new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                showLoading(false);
                
                if (result instanceof List) {
                    stores.clear();
                    stores.addAll((List<GeotaggedStore>) result);
                    storeAdapter.notifyDataSetChanged();
                    
                    updateEmptyView();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                Toast.makeText(StoreListActivity.this, 
                        getString(R.string.error_load_stores, e.getMessage()), 
                        Toast.LENGTH_SHORT).show();
                updateEmptyView();
            }
        });
    }
    
    private void updateEmptyView() {
        if (stores.isEmpty()) {
            tvNoStores.setVisibility(View.VISIBLE);
            rvStores.setVisibility(View.GONE);
        } else {
            tvNoStores.setVisibility(View.GONE);
            rvStores.setVisibility(View.VISIBLE);
        }
    }
    
    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
    
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == 
                PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
                LOCATION_PERMISSION_REQUEST_CODE);
    }
    
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != 
                PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        storeAdapter.setCurrentLocation(location);
                    }
                });
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadStores();
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
    public void onStoreClick(GeotaggedStore store) {
        // Show store details or assign to item
        if (getIntent().hasExtra("SELECT_STORE_MODE")) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("SELECTED_STORE_ID", store.getId());
            resultIntent.putExtra("SELECTED_STORE_NAME", store.getName());
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    }
    
    @Override
    public void onEditStore(GeotaggedStore store) {
        Intent intent = new Intent(this, StoreFormActivity.class);
        intent.putExtra("STORE_ID", store.getId());
        startActivity(intent);
    }
    
    @Override
    public void onDeleteStore(GeotaggedStore store) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_store)
                .setMessage(getString(R.string.delete_confirmation))
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    deleteStore(store);
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }
    
    private void deleteStore(GeotaggedStore store) {
        showLoading(true);
        
        firebaseHelper.deleteGeotaggedStore(store.getId(), new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                showLoading(false);
                Toast.makeText(StoreListActivity.this, 
                        R.string.store_deleted, 
                        Toast.LENGTH_SHORT).show();
                loadStores();
            }
            
            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                Toast.makeText(StoreListActivity.this, 
                        getString(R.string.error_delete_store, e.getMessage()), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Handle Google Play Services resolution result
        GooglePlayServicesHelper.handlePlayServicesResolutionResult(this, requestCode, resultCode);
        
        // Check if we need to refresh the store list
        if (resultCode == RESULT_OK && data != null) {
            boolean refreshStores = data.getBooleanExtra("refresh_stores", false);
            if (refreshStores) {
                // Reload the stores from Firebase
                loadStores();
            }
        }
    }
} 