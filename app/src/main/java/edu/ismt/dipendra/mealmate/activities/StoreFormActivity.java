package edu.ismt.dipendra.mealmate.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.firebase.FirebaseHelper;
import edu.ismt.dipendra.mealmate.model.GeotaggedStore;

public class StoreFormActivity extends AppCompatActivity implements OnMapReadyCallback {
    
    private static final String TAG = "StoreFormActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final float DEFAULT_ZOOM = 15f;
    
    private Toolbar toolbar;
    private TextInputLayout tilStoreName;
    private TextInputEditText etStoreName;
    private TextInputLayout tilStoreAddress;
    private TextInputEditText etStoreAddress;
    private Button btnPickLocation;
    private Button btnCurrentLocation;
    private TextView tvLocationCoordinates;
    private Button btnSaveStore;
    private ProgressBar progressBar;
    private ProgressBar mapProgressBar;
    
    private FirebaseHelper firebaseHelper;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng selectedLocation;
    private String storeId;
    private GeotaggedStore existingStore;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_form);
        
        firebaseHelper = FirebaseHelper.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        initViews();
        setupToolbar();
        setupMapFragment();
        setupListeners();
        
        // Check if we're editing an existing store
        if (getIntent().hasExtra("STORE_ID")) {
            storeId = getIntent().getStringExtra("STORE_ID");
            loadStore(storeId);
        }
    }
    
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tilStoreName = findViewById(R.id.tilStoreName);
        etStoreName = findViewById(R.id.etStoreName);
        tilStoreAddress = findViewById(R.id.tilStoreAddress);
        etStoreAddress = findViewById(R.id.etStoreAddress);
        btnPickLocation = findViewById(R.id.btnPickLocation);
        btnCurrentLocation = findViewById(R.id.btnCurrentLocation);
        tvLocationCoordinates = findViewById(R.id.tvLocationCoordinates);
        btnSaveStore = findViewById(R.id.btnSaveStore);
        progressBar = findViewById(R.id.progressBar);
        mapProgressBar = findViewById(R.id.mapProgressBar);
    }
    
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(storeId == null ? R.string.add_store : R.string.edit_store);
        }
    }
    
    private void setupMapFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }
    
    private void setupListeners() {
        btnPickLocation.setOnClickListener(v -> {
            if (googleMap != null) {
                Toast.makeText(this, "Tap on the map to select a location", Toast.LENGTH_SHORT).show();
            }
        });
        
        btnCurrentLocation.setOnClickListener(v -> {
            if (hasLocationPermission()) {
                getCurrentLocation();
            } else {
                requestLocationPermission();
            }
        });
        
        btnSaveStore.setOnClickListener(v -> {
            if (validateForm()) {
                saveStore();
            }
        });
    }
    
    private void loadStore(String storeId) {
        showLoading(true);
        
        firebaseHelper.getGeotaggedStoreById(storeId, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                showLoading(false);
                
                if (result instanceof GeotaggedStore) {
                    existingStore = (GeotaggedStore) result;
                    populateForm(existingStore);
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                Toast.makeText(StoreFormActivity.this, 
                        "Failed to load store: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
    
    private void populateForm(GeotaggedStore store) {
        etStoreName.setText(store.getName());
        etStoreAddress.setText(store.getAddress());
        
        selectedLocation = new LatLng(store.getLatitude(), store.getLongitude());
        updateLocationUI();
        
        if (googleMap != null) {
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions().position(selectedLocation).title(store.getName()));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, DEFAULT_ZOOM));
        }
    }
    
    private boolean validateForm() {
        boolean valid = true;
        
        String name = etStoreName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            tilStoreName.setError(getString(R.string.error_store_name));
            valid = false;
        } else {
            tilStoreName.setError(null);
        }
        
        if (selectedLocation == null) {
            Toast.makeText(this, R.string.error_store_location, Toast.LENGTH_SHORT).show();
            valid = false;
        }
        
        return valid;
    }
    
    private void saveStore() {
        showLoading(true);
        
        String name = etStoreName.getText().toString().trim();
        String address = etStoreAddress.getText().toString().trim();
        
        GeotaggedStore store;
        if (existingStore != null) {
            store = existingStore;
        } else {
            store = new GeotaggedStore();
            store.setId(UUID.randomUUID().toString());
        }
        
        store.setName(name);
        store.setAddress(address);
        store.setLatitude(selectedLocation.latitude);
        store.setLongitude(selectedLocation.longitude);
        store.setUserId(firebaseHelper.getCurrentUserId());
        
        firebaseHelper.addGeotaggedStore(store, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                showLoading(false);
                Toast.makeText(StoreFormActivity.this, 
                        existingStore != null ? R.string.store_updated : R.string.store_added, 
                        Toast.LENGTH_SHORT).show();
                
                // Set result with refresh flag to ensure StoreListActivity is refreshed
                Intent resultIntent = new Intent();
                resultIntent.putExtra("store_id", store.getId());
                resultIntent.putExtra("refresh_stores", true);
                setResult(RESULT_OK, resultIntent);
                
                finish();
            }
            
            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                Toast.makeText(StoreFormActivity.this, 
                        getString(existingStore != null ? R.string.error_update_store : R.string.error_add_store, 
                                e.getMessage()), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSaveStore.setEnabled(!isLoading);
    }
    
    private void showMapLoading(boolean isLoading) {
        mapProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
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
        
        showMapLoading(true);
        
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    showMapLoading(false);
                    
                    if (location != null) {
                        selectedLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        updateLocationUI();
                        
                        if (googleMap != null) {
                            googleMap.clear();
                            googleMap.addMarker(new MarkerOptions().position(selectedLocation));
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, DEFAULT_ZOOM));
                        }
                        
                        // Get address from location
                        getAddressFromLocation(location);
                    } else {
                        Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    showMapLoading(false);
                    Toast.makeText(this, "Error getting location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void getAddressFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(), location.getLongitude(), 1);
            
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    sb.append(address.getAddressLine(i));
                    if (i < address.getMaxAddressLineIndex()) {
                        sb.append(", ");
                    }
                }
                
                etStoreAddress.setText(sb.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void updateLocationUI() {
        if (selectedLocation != null) {
            tvLocationCoordinates.setText(String.format(Locale.getDefault(), 
                    "Latitude: %.6f, Longitude: %.6f", 
                    selectedLocation.latitude, selectedLocation.longitude));
            tvLocationCoordinates.setVisibility(View.VISIBLE);
        } else {
            tvLocationCoordinates.setVisibility(View.GONE);
        }
    }
    
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == 
                PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }
        
        // Set default location to London if no location is selected
        if (selectedLocation == null) {
            selectedLocation = new LatLng(51.5074, -0.1278);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 10f));
        } else {
            googleMap.addMarker(new MarkerOptions().position(selectedLocation));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, DEFAULT_ZOOM));
        }
        
        googleMap.setOnMapClickListener(latLng -> {
            selectedLocation = latLng;
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions().position(latLng));
            updateLocationUI();
            
            // Get address from location
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    StringBuilder sb = new StringBuilder();
                    
                    for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                        sb.append(address.getAddressLine(i));
                        if (i < address.getMaxAddressLineIndex()) {
                            sb.append(", ");
                        }
                    }
                    
                    etStoreAddress.setText(sb.toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
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
                
                if (googleMap != null && ActivityCompat.checkSelfPermission(this, 
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    googleMap.setMyLocationEnabled(true);
                }
            } else {
                Toast.makeText(this, R.string.location_permission_required, Toast.LENGTH_SHORT).show();
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
} 