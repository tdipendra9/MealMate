package edu.ismt.dipendra.mealmate.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.adapters.MealReminderAdapter;
import edu.ismt.dipendra.mealmate.firebase.FirebaseHelper;
import edu.ismt.dipendra.mealmate.model.Meal;
import edu.ismt.dipendra.mealmate.model.MealReminder;
import edu.ismt.dipendra.mealmate.utils.ReminderManager;

public class MealReminderActivity extends AppCompatActivity implements MealReminderAdapter.OnReminderActionListener {
    
    private static final String TAG = "MealReminderActivity";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1002;
    
    private Toolbar toolbar;
    private RecyclerView rvReminders;
    private TextView tvNoReminders;
    private ProgressBar progressBar;
    private FloatingActionButton fabAddReminder;
    
    private FirebaseHelper firebaseHelper;
    private MealReminderAdapter adapter;
    private List<MealReminder> reminders = new ArrayList<>();
    private List<Meal> meals = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_reminder);
        
        firebaseHelper = FirebaseHelper.getInstance();
        
        initViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();
        
        // Check notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission()) {
                requestNotificationPermission();
            }
        }
        
        // Load reminders and meals
        loadReminders();
        loadMeals();
    }
    
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        rvReminders = findViewById(R.id.rvReminders);
        tvNoReminders = findViewById(R.id.tvNoReminders);
        progressBar = findViewById(R.id.progressBar);
        fabAddReminder = findViewById(R.id.fabAddReminder);
    }
    
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.meal_reminders);
        }
    }
    
    private void setupRecyclerView() {
        adapter = new MealReminderAdapter(this, reminders, this);
        rvReminders.setLayoutManager(new LinearLayoutManager(this));
        rvReminders.setAdapter(adapter);
    }
    
    private void setupListeners() {
        fabAddReminder.setOnClickListener(v -> {
            if (meals.isEmpty()) {
                Toast.makeText(this, "No meals available to set reminders for", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Show meal selection dialog
            showMealSelectionDialog();
        });
    }
    
    private void loadReminders() {
        showLoading(true);
        
        firebaseHelper.getUserMealReminders(new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                showLoading(false);
                
                if (result instanceof List) {
                    reminders.clear();
                    reminders.addAll((List<MealReminder>) result);
                    adapter.notifyDataSetChanged();
                    
                    updateEmptyView();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                Toast.makeText(MealReminderActivity.this, 
                        getString(R.string.error_load_reminders, e.getMessage()), 
                        Toast.LENGTH_SHORT).show();
                updateEmptyView();
            }
        });
    }
    
    private void loadMeals() {
        firebaseHelper.getUserMeals(new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof List) {
                    meals.clear();
                    meals.addAll((List<Meal>) result);
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(MealReminderActivity.this, 
                        getString(R.string.error_load_meals, e.getMessage()), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateEmptyView() {
        if (reminders.isEmpty()) {
            tvNoReminders.setVisibility(View.VISIBLE);
            rvReminders.setVisibility(View.GONE);
        } else {
            tvNoReminders.setVisibility(View.GONE);
            rvReminders.setVisibility(View.VISIBLE);
        }
    }
    
    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
    
    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == 
                    PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
    
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 
                    NOTIFICATION_PERMISSION_REQUEST_CODE);
        }
    }
    
    private void showMealSelectionDialog() {
        // Create list of meal names
        String[] mealNames = new String[meals.size()];
        for (int i = 0; i < meals.size(); i++) {
            mealNames[i] = meals.get(i).getName();
        }
        
        // Show dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select a meal")
                .setItems(mealNames, (dialog, which) -> {
                    Meal selectedMeal = meals.get(which);
                    ReminderManager.showSetReminderDialog(this, selectedMeal);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    @Override
    public void onReminderEdit(MealReminder reminder) {
        // Find the meal for this reminder
        final Meal meal = findMealById(reminder.getMealId());
        
        if (meal != null) {
            // Delete the old reminder
            firebaseHelper.deleteMealReminder(reminder.getId(), new FirebaseHelper.FirebaseCallback() {
                @Override
                public void onSuccess(Object result) {
                    // Show set reminder dialog
                    ReminderManager.showSetReminderDialog(MealReminderActivity.this, meal);
                    
                    // Reload reminders
                    loadReminders();
                }
                
                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(MealReminderActivity.this, 
                            getString(R.string.error_delete_reminder, e.getMessage()), 
                            Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Meal not found", Toast.LENGTH_SHORT).show();
        }
    }
    
    private Meal findMealById(String mealId) {
        for (Meal m : meals) {
            if (m.getId().equals(mealId)) {
                return m;
            }
        }
        return null;
    }
    
    @Override
    public void onReminderDelete(MealReminder reminder) {
        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.delete_reminder)
                .setMessage("Are you sure you want to delete this reminder?")
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    // Delete reminder
                    firebaseHelper.deleteMealReminder(reminder.getId(), new FirebaseHelper.FirebaseCallback() {
                        @Override
                        public void onSuccess(Object result) {
                            // Cancel scheduled notification
                            edu.ismt.dipendra.mealmate.services.ReminderNotificationService.cancelReminder(
                                    MealReminderActivity.this, reminder.getId());
                            
                            // Show success message
                            Toast.makeText(MealReminderActivity.this, 
                                    R.string.reminder_deleted, 
                                    Toast.LENGTH_SHORT).show();
                            
                            // Reload reminders
                            loadReminders();
                        }
                        
                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(MealReminderActivity.this, 
                                    getString(R.string.error_delete_reminder, e.getMessage()), 
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadReminders();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.notification_permission_required, Toast.LENGTH_SHORT).show();
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