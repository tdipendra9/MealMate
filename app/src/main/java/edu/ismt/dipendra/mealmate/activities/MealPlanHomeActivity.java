package edu.ismt.dipendra.mealmate.activities;

import android.app.ActivityOptions;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.adapters.MealPlanAdapter;
import edu.ismt.dipendra.mealmate.firebase.FirebaseHelper;
import edu.ismt.dipendra.mealmate.model.MealPlanItem;
import edu.ismt.dipendra.mealmate.model.Meal;

public class MealPlanHomeActivity extends AppCompatActivity implements MealPlanAdapter.OnMealPlanClickListener {

    private static final int REQUEST_ADD_MEAL_PLAN = 1;
    private static final int REQUEST_EDIT_MEAL_PLAN = 2;
    private static final String TAG = "MealPlanHomeActivity";

    private Toolbar toolbar;
    private TextView tvSelectedDayDate;
    private MaterialButton btnToday;
    
    // Week view container
    private View weekViewContainer;
    private ImageButton btnPreviousWeek;
    private ImageButton btnNextWeek;
    
    // Day selector views
    private MaterialCardView cardDay1;
    private MaterialCardView cardDay2;
    private MaterialCardView cardDay3;
    private MaterialCardView cardDay4;
    private MaterialCardView cardDay5;
    private MaterialCardView cardDay6;
    private MaterialCardView cardDay7;
    
    private TextView tvDay1Name;
    private TextView tvDay2Name;
    private TextView tvDay3Name;
    private TextView tvDay4Name;
    private TextView tvDay5Name;
    private TextView tvDay6Name;
    private TextView tvDay7Name;
    
    private TextView tvDay1Date;
    private TextView tvDay2Date;
    private TextView tvDay3Date;
    private TextView tvDay4Date;
    private TextView tvDay5Date;
    private TextView tvDay6Date;
    private TextView tvDay7Date;
    
    private RecyclerView recyclerViewBreakfast;
    private RecyclerView recyclerViewLunch;
    private RecyclerView recyclerViewDinner;
    private RecyclerView recyclerViewSnacks;
    private MaterialButton btnAddBreakfast;
    private MaterialButton btnAddLunch;
    private MaterialButton btnAddDinner;
    private MaterialButton btnAddSnacks;
    private CircularProgressIndicator progressIndicator;
    
    private FirebaseHelper firebaseHelper;
    private MealPlanAdapter breakfastAdapter;
    private MealPlanAdapter lunchAdapter;
    private MealPlanAdapter dinnerAdapter;
    private MealPlanAdapter snacksAdapter;
    private List<MealPlanItem> breakfastList;
    private List<MealPlanItem> lunchList;
    private List<MealPlanItem> dinnerList;
    private List<MealPlanItem> snacksList;
    private Date selectedDate;
    private Calendar calendar;
    private List<Date> weekDates;
    private int selectedDayIndex = 0;
    
    private GestureDetectorCompat gestureDetector;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_plan_home);
        
        initViews();
        setupToolbar();
        initObjects();
        setupRecyclerViews();
        setupListeners();
        setupGestureDetector();
        
        // Generate week dates and update UI
        generateWeekDates();
        updateDaySelectorUI();
        updateSelectedDayHighlight();
        
        // Load meal plans for the selected date
        loadMealPlans();
        
        // Apply animations
        applyAnimations();
    }
    
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvSelectedDayDate = findViewById(R.id.tvSelectedDayDate);
        btnToday = findViewById(R.id.btnToday);
        
        // Week view container
        weekViewContainer = findViewById(R.id.weekViewContainer);
        btnPreviousWeek = findViewById(R.id.btnPreviousWeek);
        btnNextWeek = findViewById(R.id.btnNextWeek);
        
        // Day selector views
        cardDay1 = findViewById(R.id.cardDay1);
        cardDay2 = findViewById(R.id.cardDay2);
        cardDay3 = findViewById(R.id.cardDay3);
        cardDay4 = findViewById(R.id.cardDay4);
        cardDay5 = findViewById(R.id.cardDay5);
        cardDay6 = findViewById(R.id.cardDay6);
        cardDay7 = findViewById(R.id.cardDay7);
        
        tvDay1Name = findViewById(R.id.tvDay1Name);
        tvDay2Name = findViewById(R.id.tvDay2Name);
        tvDay3Name = findViewById(R.id.tvDay3Name);
        tvDay4Name = findViewById(R.id.tvDay4Name);
        tvDay5Name = findViewById(R.id.tvDay5Name);
        tvDay6Name = findViewById(R.id.tvDay6Name);
        tvDay7Name = findViewById(R.id.tvDay7Name);
        
        tvDay1Date = findViewById(R.id.tvDay1Date);
        tvDay2Date = findViewById(R.id.tvDay2Date);
        tvDay3Date = findViewById(R.id.tvDay3Date);
        tvDay4Date = findViewById(R.id.tvDay4Date);
        tvDay5Date = findViewById(R.id.tvDay5Date);
        tvDay6Date = findViewById(R.id.tvDay6Date);
        tvDay7Date = findViewById(R.id.tvDay7Date);
        
        // Meal sections
        recyclerViewBreakfast = findViewById(R.id.recyclerViewBreakfast);
        recyclerViewLunch = findViewById(R.id.recyclerViewLunch);
        recyclerViewDinner = findViewById(R.id.recyclerViewDinner);
        recyclerViewSnacks = findViewById(R.id.recyclerViewSnacks);
        
        btnAddBreakfast = findViewById(R.id.btnAddBreakfast);
        btnAddLunch = findViewById(R.id.btnAddLunch);
        btnAddDinner = findViewById(R.id.btnAddDinner);
        btnAddSnacks = findViewById(R.id.btnAddSnacks);
        
        progressIndicator = findViewById(R.id.progressIndicator);
    }
    
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.meal_plan);
        }
    }
    
    private void initObjects() {
        firebaseHelper = FirebaseHelper.getInstance();
        
        breakfastList = new ArrayList<>();
        lunchList = new ArrayList<>();
        dinnerList = new ArrayList<>();
        snacksList = new ArrayList<>();
        
        calendar = Calendar.getInstance();
        selectedDate = calendar.getTime();
        weekDates = new ArrayList<>();
    }
    
    private void generateWeekDates() {
        weekDates.clear();
        
        // Clone the calendar to avoid modifying the original
        Calendar tempCalendar = (Calendar) calendar.clone();
        
        // Set to the start of the week (Sunday)
        tempCalendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        
        // Add 7 days to the list
        for (int i = 0; i < 7; i++) {
            weekDates.add(tempCalendar.getTime());
            tempCalendar.add(Calendar.DAY_OF_MONTH, 1);
        }
    }
    
    private void updateDaySelectorUI() {
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("d", Locale.getDefault());
        
        tvDay1Name.setText(dayFormat.format(weekDates.get(0)).toUpperCase());
        tvDay2Name.setText(dayFormat.format(weekDates.get(1)).toUpperCase());
        tvDay3Name.setText(dayFormat.format(weekDates.get(2)).toUpperCase());
        tvDay4Name.setText(dayFormat.format(weekDates.get(3)).toUpperCase());
        tvDay5Name.setText(dayFormat.format(weekDates.get(4)).toUpperCase());
        tvDay6Name.setText(dayFormat.format(weekDates.get(5)).toUpperCase());
        tvDay7Name.setText(dayFormat.format(weekDates.get(6)).toUpperCase());
        
        tvDay1Date.setText(dateFormat.format(weekDates.get(0)));
        tvDay2Date.setText(dateFormat.format(weekDates.get(1)));
        tvDay3Date.setText(dateFormat.format(weekDates.get(2)));
        tvDay4Date.setText(dateFormat.format(weekDates.get(3)));
        tvDay5Date.setText(dateFormat.format(weekDates.get(4)));
        tvDay6Date.setText(dateFormat.format(weekDates.get(5)));
        tvDay7Date.setText(dateFormat.format(weekDates.get(6)));
    }
    
    private void updateSelectedDayHighlight() {
        // Reset all cards to default state
        cardDay1.setCardBackgroundColor(getResources().getColor(R.color.white));
        cardDay2.setCardBackgroundColor(getResources().getColor(R.color.white));
        cardDay3.setCardBackgroundColor(getResources().getColor(R.color.white));
        cardDay4.setCardBackgroundColor(getResources().getColor(R.color.white));
        cardDay5.setCardBackgroundColor(getResources().getColor(R.color.white));
        cardDay6.setCardBackgroundColor(getResources().getColor(R.color.white));
        cardDay7.setCardBackgroundColor(getResources().getColor(R.color.white));
        
        tvDay1Name.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        tvDay2Name.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        tvDay3Name.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        tvDay4Name.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        tvDay5Name.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        tvDay6Name.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        tvDay7Name.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        
        tvDay1Date.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        tvDay2Date.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        tvDay3Date.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        tvDay4Date.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        tvDay5Date.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        tvDay6Date.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        tvDay7Date.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        
        // Highlight the selected day
        MaterialCardView selectedCard;
        TextView selectedDayName;
        TextView selectedDayDate;
        
        switch (selectedDayIndex) {
            case 0:
                selectedCard = cardDay1;
                selectedDayName = tvDay1Name;
                selectedDayDate = tvDay1Date;
                break;
            case 1:
                selectedCard = cardDay2;
                selectedDayName = tvDay2Name;
                selectedDayDate = tvDay2Date;
                break;
            case 2:
                selectedCard = cardDay3;
                selectedDayName = tvDay3Name;
                selectedDayDate = tvDay3Date;
                break;
            case 3:
                selectedCard = cardDay4;
                selectedDayName = tvDay4Name;
                selectedDayDate = tvDay4Date;
                break;
            case 4:
                selectedCard = cardDay5;
                selectedDayName = tvDay5Name;
                selectedDayDate = tvDay5Date;
                break;
            case 5:
                selectedCard = cardDay6;
                selectedDayName = tvDay6Name;
                selectedDayDate = tvDay6Date;
                break;
            case 6:
                selectedCard = cardDay7;
                selectedDayName = tvDay7Name;
                selectedDayDate = tvDay7Date;
                break;
            default:
                selectedCard = cardDay1;
                selectedDayName = tvDay1Name;
                selectedDayDate = tvDay1Date;
                break;
        }
        
        selectedCard.setCardBackgroundColor(getResources().getColor(R.color.colorPrimary));
        selectedDayName.setTextColor(getResources().getColor(R.color.white));
        selectedDayDate.setTextColor(getResources().getColor(R.color.white));
        
        // Update the selected date display
        SimpleDateFormat fullDateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
        tvSelectedDayDate.setText(fullDateFormat.format(selectedDate));
    }
    
    private void setupRecyclerViews() {
        // Breakfast
        breakfastAdapter = new MealPlanAdapter(this, breakfastList, this);
        recyclerViewBreakfast.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewBreakfast.setAdapter(breakfastAdapter);
        
        // Lunch
        lunchAdapter = new MealPlanAdapter(this, lunchList, this);
        recyclerViewLunch.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewLunch.setAdapter(lunchAdapter);
        
        // Dinner
        dinnerAdapter = new MealPlanAdapter(this, dinnerList, this);
        recyclerViewDinner.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewDinner.setAdapter(dinnerAdapter);
        
        // Snacks
        snacksAdapter = new MealPlanAdapter(this, snacksList, this);
        recyclerViewSnacks.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSnacks.setAdapter(snacksAdapter);
    }
    
    private void setupListeners() {
        // Day selector listeners
        cardDay1.setOnClickListener(v -> {
            selectedDayIndex = 0;
            selectedDate = weekDates.get(selectedDayIndex);
            updateSelectedDayHighlight();
            loadMealPlans();
        });
        
        cardDay2.setOnClickListener(v -> {
            selectedDayIndex = 1;
            selectedDate = weekDates.get(selectedDayIndex);
            updateSelectedDayHighlight();
            loadMealPlans();
        });
        
        cardDay3.setOnClickListener(v -> {
            selectedDayIndex = 2;
            selectedDate = weekDates.get(selectedDayIndex);
            updateSelectedDayHighlight();
            loadMealPlans();
        });
        
        cardDay4.setOnClickListener(v -> {
            selectedDayIndex = 3;
            selectedDate = weekDates.get(selectedDayIndex);
            updateSelectedDayHighlight();
            loadMealPlans();
        });
        
        cardDay5.setOnClickListener(v -> {
            selectedDayIndex = 4;
            selectedDate = weekDates.get(selectedDayIndex);
            updateSelectedDayHighlight();
            loadMealPlans();
        });
        
        cardDay6.setOnClickListener(v -> {
            selectedDayIndex = 5;
            selectedDate = weekDates.get(selectedDayIndex);
            updateSelectedDayHighlight();
            loadMealPlans();
        });
        
        cardDay7.setOnClickListener(v -> {
            selectedDayIndex = 6;
            selectedDate = weekDates.get(selectedDayIndex);
            updateSelectedDayHighlight();
            loadMealPlans();
        });
        
        // Week navigation
        btnPreviousWeek.setOnClickListener(v -> moveToPreviousWeek());
        btnNextWeek.setOnClickListener(v -> moveToNextWeek());
        btnToday.setOnClickListener(v -> goToToday());
        
        // Add meal buttons
        btnAddBreakfast.setOnClickListener(v -> {
            showRecipeSelectionDialog("Breakfast");
        });
        
        btnAddLunch.setOnClickListener(v -> {
            showRecipeSelectionDialog("Lunch");
        });
        
        btnAddDinner.setOnClickListener(v -> {
            showRecipeSelectionDialog("Dinner");
        });
        
        btnAddSnacks.setOnClickListener(v -> {
            showRecipeSelectionDialog("Snacks");
        });
    }
    
    private void applyAnimations() {
        // Apply animations to views
        weekViewContainer.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
        
        // Animate the date display directly
        tvSelectedDayDate.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));
        btnToday.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));
        
        View breakfastSection = findViewById(R.id.breakfastSection);
        View lunchSection = findViewById(R.id.lunchSection);
        View dinnerSection = findViewById(R.id.dinnerSection);
        View snacksSection = findViewById(R.id.snacksSection);
        
        breakfastSection.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
        lunchSection.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
        dinnerSection.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
        snacksSection.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
    }
    
    private void loadMealPlans() {
        if (progressIndicator != null) {
            progressIndicator.setVisibility(View.VISIBLE);
        }
        
        try {
            if (firebaseHelper == null) {
                firebaseHelper = FirebaseHelper.getInstance();
            }
            
            if (selectedDate == null) {
                selectedDate = Calendar.getInstance().getTime();
            }
            
            // Log attempt to load meal plans
            Log.d(TAG, "Attempting to load meal plans for date: " + selectedDate);
            
            // Clear existing lists
            breakfastList.clear();
            lunchList.clear();
            dinnerList.clear();
            snacksList.clear();
            
            // Show meal sections when loading starts
            showMealSections();
            
            // Reset loaded data counter
            loadedDataCount = 0;
            
            // Use FirebaseHelper to get meal plans from Firestore
            firebaseHelper.getMealPlanItemsByDate(selectedDate, new FirebaseHelper.FirebaseCallback() {
                @Override
                public void onSuccess(Object result) {
                    if (result instanceof List) {
                        List<?> items = (List<?>) result;
                        for (Object item : items) {
                            if (item instanceof MealPlanItem) {
                                MealPlanItem mealPlan = (MealPlanItem) item;
                                String mealType = mealPlan.getMealType();
                                
                                if (mealType != null) {
                                    switch (mealType.toLowerCase()) {
                                        case "breakfast":
                                            breakfastList.add(mealPlan);
                                            break;
                                        case "lunch":
                                            lunchList.add(mealPlan);
                                            break;
                                        case "dinner":
                                            dinnerList.add(mealPlan);
                                            break;
                                        case "snacks":
                                            snacksList.add(mealPlan);
                                            break;
                                    }
                                }
                            }
                        }
                        
                        // Update all adapters
                        breakfastAdapter.notifyDataSetChanged();
                        lunchAdapter.notifyDataSetChanged();
                        dinnerAdapter.notifyDataSetChanged();
                        snacksAdapter.notifyDataSetChanged();
                        
                        // Hide progress indicator
                        if (progressIndicator != null) {
                            progressIndicator.setVisibility(View.GONE);
                        }
                        
                        // Reset loaded data counter
                        loadedDataCount = 0;
                        
                        Log.d(TAG, "Loaded meal plans: " + 
                              "Breakfast=" + breakfastList.size() + 
                              ", Lunch=" + lunchList.size() + 
                              ", Dinner=" + dinnerList.size() + 
                              ", Snacks=" + snacksList.size());
                    }
                }
                
                @Override
                public void onFailure(Exception e) {
                    // Hide progress indicator
                    if (progressIndicator != null) {
                        progressIndicator.setVisibility(View.GONE);
                    }
                    
                    // Reset loaded data counter
                    loadedDataCount = 0;
                    
                    Log.e(TAG, "Error loading meal plans", e);
                    Toast.makeText(MealPlanHomeActivity.this,
                            getString(R.string.error_load_meal_plans, e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                }
            });
            
        } catch (Exception e) {
            if (progressIndicator != null) {
                progressIndicator.setVisibility(View.GONE);
            }
            
            Log.e(TAG, "Error in loadMealPlans", e);
            Toast.makeText(this,
                    getString(R.string.error_load_meal_plans, e.getMessage()),
                    Toast.LENGTH_SHORT).show();
        }
    }
    
    // Helper method to check if all data is loaded and hide progress indicator
    private int loadedDataCount = 0;
    private void checkAndHideProgressIndicator() {
        loadedDataCount++;
        if (loadedDataCount >= 4) { // 4 meal types (breakfast, lunch, dinner, snacks)
            if (progressIndicator != null) {
                progressIndicator.setVisibility(View.GONE);
            }
            loadedDataCount = 0;
        }
    }

    private void updateEmptyViews() {
        // This method is no longer needed with the new UI design
        // We'll just show empty RecyclerViews when there are no items
    }

    @Override
    public void onMealPlanClick(MealPlanItem mealPlan) {
        Intent intent = new Intent(MealPlanHomeActivity.this, MealPlanDetailActivity.class);
        intent.putExtra("meal_plan_id", mealPlan.getId());
        startActivityForResult(intent, REQUEST_EDIT_MEAL_PLAN);
    }

    @Override
    public void onMealPlanLongClick(MealPlanItem mealPlan) {
        showDeleteConfirmationDialog(mealPlan);
    }

    @Override
    public void onDeleteClick(MealPlanItem mealPlan) {
        showDeleteConfirmationDialog(mealPlan);
    }

    private void showDeleteConfirmationDialog(MealPlanItem mealPlan) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.delete_meal_plan);
        builder.setMessage(getString(R.string.confirm_delete_meal, mealPlan.getMealName()));
        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
            deleteMealPlan(mealPlan);
        });
        builder.setNegativeButton(R.string.no, null);
        builder.show();
    }

    private void deleteMealPlan(MealPlanItem mealPlan) {
        if (mealPlan == null || mealPlan.getId() == null) {
            Toast.makeText(this, "Invalid meal plan", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show loading indicator
        if (progressIndicator != null) {
            progressIndicator.setVisibility(View.VISIBLE);
        }
        
        // Use FirebaseHelper to delete the meal plan item
        firebaseHelper.deleteMealPlanItem(mealPlan.getId(), new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                // Hide loading indicator
                if (progressIndicator != null) {
                    progressIndicator.setVisibility(View.GONE);
                }
                
                Toast.makeText(MealPlanHomeActivity.this, 
                        getString(R.string.meal_deleted), Toast.LENGTH_SHORT).show();
                
                // Refresh the meal plans
                loadMealPlans();
            }
            
            @Override
            public void onFailure(Exception e) {
                // Hide loading indicator
                if (progressIndicator != null) {
                    progressIndicator.setVisibility(View.GONE);
                }
                
                Toast.makeText(MealPlanHomeActivity.this, 
                        getString(R.string.error_delete_meal, e.getMessage()), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_ADD_MEAL_PLAN || requestCode == REQUEST_EDIT_MEAL_PLAN) {
                // Refresh meal plans
                loadMealPlans();
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

    private void confirmDeleteMealPlan(MealPlanItem mealPlan) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.delete_meal_plan);
        builder.setMessage(getString(R.string.confirm_delete_meal, mealPlan.getMealName()));
        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
            deleteMealPlan(mealPlan);
        });
        builder.setNegativeButton(R.string.no, null);
        builder.show();
    }

    private void showMealDetails(MealPlanItem mealPlan) {
        // Create an intent to view the meal details
        Intent intent = new Intent(this, MealDetailActivity.class);
        intent.putExtra("meal_id", mealPlan.getMealId());
        startActivity(intent);
        // Add entry animation
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void showMealPlanOptions(MealPlanItem mealPlan) {
        // Create a dialog with options for the meal plan
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(mealPlan.getMealName());
        
        String[] options = {"View Details", "Delete"};
        
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // View Details
                    showMealDetails(mealPlan);
                    break;
                case 1: // Delete
                    showDeleteConfirmationDialog(mealPlan);
                    break;
            }
        });
        
        builder.show();
    }

    /**
     * Show all meal sections with their add buttons
     */
    private void showMealSections() {
        // Make sure all meal sections are visible
        View breakfastSection = findViewById(R.id.breakfastSection);
        View lunchSection = findViewById(R.id.lunchSection);
        View dinnerSection = findViewById(R.id.dinnerSection);
        View snacksSection = findViewById(R.id.snacksSection);
        
        if (breakfastSection != null) breakfastSection.setVisibility(View.VISIBLE);
        if (lunchSection != null) lunchSection.setVisibility(View.VISIBLE);
        if (dinnerSection != null) dinnerSection.setVisibility(View.VISIBLE);
        if (snacksSection != null) snacksSection.setVisibility(View.VISIBLE);
        
        // Make sure all add buttons are visible
        btnAddBreakfast.setVisibility(View.VISIBLE);
        btnAddLunch.setVisibility(View.VISIBLE);
        btnAddDinner.setVisibility(View.VISIBLE);
        btnAddSnacks.setVisibility(View.VISIBLE);
        
        // Show a toast message to indicate that the user can add meals for this date
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
        String formattedDate = dateFormat.format(selectedDate);
        Toast.makeText(this, "Add meals for " + formattedDate, Toast.LENGTH_SHORT).show();
    }

    /**
     * Move to the previous week
     */
    private void moveToPreviousWeek() {
        // Move the week dates back by 7 days
        Calendar cal = Calendar.getInstance();
        cal.setTime(weekDates.get(0)); // Get the first day of the current week
        cal.add(Calendar.DAY_OF_MONTH, -7); // Go back 7 days
        
        // Generate new week dates starting from this date
        weekDates.clear();
        for (int i = 0; i < 7; i++) {
            weekDates.add(cal.getTime());
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        // Update the UI
        updateDaySelectorUI();
        
        // Keep the same day of the week selected
        selectedDate = weekDates.get(selectedDayIndex);
        updateDateDisplay();
        updateDayViewDate();
        
        // Reload meal plans
        loadMealPlans();
        showMealSections();
    }
    
    /**
     * Move to the next week
     */
    private void moveToNextWeek() {
        // Move the week dates forward by 7 days
        Calendar cal = Calendar.getInstance();
        cal.setTime(weekDates.get(0)); // Get the first day of the current week
        cal.add(Calendar.DAY_OF_MONTH, 7); // Go forward 7 days
        
        // Generate new week dates starting from this date
        weekDates.clear();
        for (int i = 0; i < 7; i++) {
            weekDates.add(cal.getTime());
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        // Update the UI
        updateDaySelectorUI();
        
        // Keep the same day of the week selected
        selectedDate = weekDates.get(selectedDayIndex);
        updateDateDisplay();
        updateDayViewDate();
        
        // Reload meal plans
        loadMealPlans();
        showMealSections();
    }

    /**
     * Go to today's date
     */
    private void goToToday() {
        // Set the selected date to today
        selectedDate = Calendar.getInstance().getTime();
        
        // Update both Day and Week views
        updateDayViewDate();
        updateDateDisplay();
        
        // Sync the Week view with the selected date
        syncSelectedDateWithWeekView();
        
        loadMealPlans();
        showMealSections();
        
        // Show a toast message
        Toast.makeText(this, "Showing today's meal plans", Toast.LENGTH_SHORT).show();
    }

    /**
     * Update the day view date display
     */
    private void updateDayViewDate() {
        if (selectedDate != null && tvSelectedDayDate != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
            tvSelectedDayDate.setText(dateFormat.format(selectedDate));
        }
    }

    private void syncSelectedDateWithWeekView() {
        if (selectedDate == null || weekDates == null || weekDates.isEmpty()) {
            return;
        }
        
        // Check if the selected date is within the current week
        boolean dateFound = false;
        for (int i = 0; i < weekDates.size(); i++) {
            if (isSameDay(selectedDate, weekDates.get(i))) {
                selectedDayIndex = i;
                dateFound = true;
                break;
            }
        }
        
        // If the selected date is not in the current week, regenerate the week
        // to include the selected date
        if (!dateFound) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(selectedDate);
            
            // Find the first day of the week (Monday) for the selected date
            while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                cal.add(Calendar.DAY_OF_MONTH, -1);
            }
            
            // Generate 7 days starting from Monday
            weekDates.clear();
            for (int i = 0; i < 7; i++) {
                weekDates.add(cal.getTime());
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }
            
            // Find the selected date in the new week
            for (int i = 0; i < weekDates.size(); i++) {
                if (isSameDay(selectedDate, weekDates.get(i))) {
                    selectedDayIndex = i;
                    break;
                }
            }
            
            // Update the day selector UI
            updateDaySelectorUI();
        }
        
        // Highlight the selected day
        updateSelectedDayHighlight();
    }
    
    /**
     * Check if two dates are the same day
     */
    private boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
               cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Set up gesture detector for swipe navigation in day view
     */
    private void setupGestureDetector() {
        gestureDetector = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;
            
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                boolean result = false;
                try {
                    float diffX = e2.getX() - e1.getX();
                    float diffY = e2.getY() - e1.getY();
                    
                    // Check if the swipe was horizontal
                    if (Math.abs(diffX) > Math.abs(diffY) && 
                        Math.abs(diffX) > SWIPE_THRESHOLD && 
                        Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        
                        // Check if the day view is visible
                        if (weekViewContainer.getVisibility() == View.VISIBLE) {
                            if (diffX > 0) {
                                // Swipe right - go to previous week
                                moveToPreviousWeek();
                            } else {
                                // Swipe left - go to next week
                                moveToNextWeek();
                            }
                            result = true;
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in onFling", e);
                }
                return result;
            }
        });
        
        // Set touch listener on day view container
        weekViewContainer.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });
    }

    private void updateDateDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(sdf.format(selectedDate));
        }
    }

    private void showRecipeSelectionDialog(String mealType) {
        // Show loading indicator
        progressIndicator.setVisibility(View.VISIBLE);
        
        // Get recipes from Firebase
        firebaseHelper.getRecipes(new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                progressIndicator.setVisibility(View.GONE);
                
                if (result instanceof List) {
                    List<?> items = (List<?>) result;
                    if (items.isEmpty()) {
                        // No recipes found, show message and offer to create new meal
                        new AlertDialog.Builder(MealPlanHomeActivity.this)
                                .setTitle("No Recipes Found")
                                .setMessage("You don't have any recipes yet. Would you like to create a new meal?")
                                .setPositiveButton("Create New", (dialog, which) -> {
                                    // Navigate to MealFormActivity to create a new meal
                                    Intent intent = new Intent(MealPlanHomeActivity.this, MealFormActivity.class);
                                    intent.putExtra("meal_type", mealType);
                                    intent.putExtra("selected_date", selectedDate.getTime());
                                    startActivityForResult(intent, REQUEST_ADD_MEAL_PLAN);
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                        return;
                    }
                    
                    // Create list of recipe names for the dialog
                    List<Meal> meals = new ArrayList<>();
                    List<String> mealNames = new ArrayList<>();
                    
                    for (Object item : items) {
                        if (item instanceof Meal) {
                            Meal meal = (Meal) item;
                            meals.add(meal);
                            mealNames.add(meal.getName());
                        }
                    }
                    
                    // Show dialog with recipe list
                    new AlertDialog.Builder(MealPlanHomeActivity.this)
                            .setTitle("Select Recipe for " + mealType)
                            .setItems(mealNames.toArray(new String[0]), (dialog, which) -> {
                                // Get selected meal
                                Meal selectedMeal = meals.get(which);
                                
                                // Create a new MealPlanItem
                                MealPlanItem mealPlanItem = new MealPlanItem();
                                mealPlanItem.setMealId(selectedMeal.getId());
                                mealPlanItem.setMealName(selectedMeal.getName());
                                mealPlanItem.setMealType(mealType);
                                mealPlanItem.setDate(selectedDate);
                                mealPlanItem.setUserId(firebaseHelper.getCurrentUserId());
                                mealPlanItem.setImageUrl(selectedMeal.getImageUrl());
                                
                                // Save the meal plan item
                                saveMealPlanItem(mealPlanItem);
                            })
                            .setNeutralButton("Create New", (dialog, which) -> {
                                // Navigate to MealFormActivity to create a new meal
                                Intent intent = new Intent(MealPlanHomeActivity.this, MealFormActivity.class);
                                intent.putExtra("meal_type", mealType);
                                intent.putExtra("selected_date", selectedDate.getTime());
                                startActivityForResult(intent, REQUEST_ADD_MEAL_PLAN);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                progressIndicator.setVisibility(View.GONE);
                Toast.makeText(MealPlanHomeActivity.this, 
                        "Failed to load recipes: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                
                // Fallback to MealFormActivity
                Intent intent = new Intent(MealPlanHomeActivity.this, MealFormActivity.class);
                intent.putExtra("meal_type", mealType);
                intent.putExtra("selected_date", selectedDate.getTime());
                startActivityForResult(intent, REQUEST_ADD_MEAL_PLAN);
            }
        });
    }
    
    private void saveMealPlanItem(MealPlanItem mealPlanItem) {
        progressIndicator.setVisibility(View.VISIBLE);
        
        firebaseHelper.saveMealPlanItem(mealPlanItem, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                progressIndicator.setVisibility(View.GONE);
                Toast.makeText(MealPlanHomeActivity.this, 
                        "Meal added to plan", Toast.LENGTH_SHORT).show();
                
                // Reload meal plans to show the newly added item
                loadMealPlans();
            }
            
            @Override
            public void onFailure(Exception e) {
                progressIndicator.setVisibility(View.GONE);
                Toast.makeText(MealPlanHomeActivity.this, 
                        "Failed to add meal to plan: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
} 