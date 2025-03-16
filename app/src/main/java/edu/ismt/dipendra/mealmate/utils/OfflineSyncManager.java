package edu.ismt.dipendra.mealmate.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.ismt.dipendra.mealmate.firebase.FirebaseHelper;
import edu.ismt.dipendra.mealmate.model.Meal;
import edu.ismt.dipendra.mealmate.model.GroceryItem;
import edu.ismt.dipendra.mealmate.model.MealPlanItem;

public class OfflineSyncManager {
    private static final String TAG = "OfflineSyncManager";
    private static final String PREF_NAME = "offline_data";
    private static final String KEY_PENDING_MEALS = "pending_meals";
    private static final String KEY_PENDING_GROCERY_ITEMS = "pending_grocery_items";
    private static final String KEY_PENDING_MEAL_PLANS = "pending_meal_plans";
    
    private final Context context;
    private final SharedPreferences preferences;
    private final ExecutorService executorService;
    private final FirebaseHelper firebaseHelper;
    
    public OfflineSyncManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.executorService = Executors.newSingleThreadExecutor();
        this.firebaseHelper = FirebaseHelper.getInstance();
    }
    
    public void saveMealOffline(Meal meal) {
        List<Meal> pendingMeals = getPendingMeals();
        pendingMeals.add(meal);
        savePendingMeals(pendingMeals);
        
        // Try to sync if network is available
        if (isNetworkAvailable()) {
            syncPendingData();
        }
    }
    
    public void saveGroceryItemOffline(GroceryItem item) {
        List<GroceryItem> pendingItems = getPendingGroceryItems();
        pendingItems.add(item);
        savePendingGroceryItems(pendingItems);
        
        if (isNetworkAvailable()) {
            syncPendingData();
        }
    }
    
    public void saveMealPlanOffline(MealPlanItem mealPlan) {
        List<MealPlanItem> pendingPlans = getPendingMealPlans();
        pendingPlans.add(mealPlan);
        savePendingMealPlans(pendingPlans);
        
        if (isNetworkAvailable()) {
            syncPendingData();
        }
    }
    
    public void syncPendingData() {
        executorService.execute(() -> {
            if (!isNetworkAvailable()) {
                Log.d(TAG, "No network available, skipping sync");
                return;
            }
            
            // Sync meals
            List<Meal> pendingMeals = getPendingMeals();
            for (Meal meal : pendingMeals) {
                firebaseHelper.addMeal(meal, new FirebaseHelper.FirebaseCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        removePendingMeal(meal);
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Failed to sync meal: " + e.getMessage());
                    }
                });
            }
            
            // Sync grocery items
            List<GroceryItem> pendingItems = getPendingGroceryItems();
            for (GroceryItem item : pendingItems) {
                // Add to appropriate grocery list
                // Implementation depends on your data structure
            }
            
            // Sync meal plans
            List<MealPlanItem> pendingPlans = getPendingMealPlans();
            for (MealPlanItem plan : pendingPlans) {
                firebaseHelper.saveMealPlanItem(plan, new FirebaseHelper.FirebaseCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        removePendingMealPlan(plan);
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Failed to sync meal plan: " + e.getMessage());
                    }
                });
            }
        });
    }
    
    private List<Meal> getPendingMeals() {
        List<Meal> meals = new ArrayList<>();
        try {
            String json = preferences.getString(KEY_PENDING_MEALS, "[]");
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Meal meal = new Meal();
                meal.setId(obj.getString("id"));
                meal.setName(obj.getString("name"));
                // Add other meal properties as needed
                meals.add(meal);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing pending meals", e);
        }
        return meals;
    }
    
    private void savePendingMeals(List<Meal> meals) {
        try {
            JSONArray array = new JSONArray();
            for (Meal meal : meals) {
                JSONObject obj = new JSONObject();
                obj.put("id", meal.getId());
                obj.put("name", meal.getName());
                // Add other meal properties as needed
                array.put(obj);
            }
            preferences.edit().putString(KEY_PENDING_MEALS, array.toString()).apply();
        } catch (JSONException e) {
            Log.e(TAG, "Error saving pending meals", e);
        }
    }
    
    private void removePendingMeal(Meal meal) {
        List<Meal> meals = getPendingMeals();
        meals.remove(meal);
        savePendingMeals(meals);
    }
    
    private List<GroceryItem> getPendingGroceryItems() {
        List<GroceryItem> items = new ArrayList<>();
        try {
            String json = preferences.getString(KEY_PENDING_GROCERY_ITEMS, "[]");
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                GroceryItem item = new GroceryItem();
                item.setId(obj.getString("id"));
                item.setName(obj.getString("name"));
                // Add other item properties as needed
                items.add(item);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing pending grocery items", e);
        }
        return items;
    }
    
    private void savePendingGroceryItems(List<GroceryItem> items) {
        try {
            JSONArray array = new JSONArray();
            for (GroceryItem item : items) {
                JSONObject obj = new JSONObject();
                obj.put("id", item.getId());
                obj.put("name", item.getName());
                // Add other item properties as needed
                array.put(obj);
            }
            preferences.edit().putString(KEY_PENDING_GROCERY_ITEMS, array.toString()).apply();
        } catch (JSONException e) {
            Log.e(TAG, "Error saving pending grocery items", e);
        }
    }
    
    private List<MealPlanItem> getPendingMealPlans() {
        List<MealPlanItem> plans = new ArrayList<>();
        try {
            String json = preferences.getString(KEY_PENDING_MEAL_PLANS, "[]");
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                MealPlanItem plan = new MealPlanItem();
                plan.setId(obj.getString("id"));
                plan.setMealName(obj.getString("mealName"));
                plan.setMealType(obj.getString("mealType"));
                plan.setDate(new Date(obj.getLong("date")));
                // Add other plan properties as needed
                plans.add(plan);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing pending meal plans", e);
        }
        return plans;
    }
    
    private void savePendingMealPlans(List<MealPlanItem> plans) {
        try {
            JSONArray array = new JSONArray();
            for (MealPlanItem plan : plans) {
                JSONObject obj = new JSONObject();
                obj.put("id", plan.getId());
                obj.put("mealName", plan.getMealName());
                obj.put("mealType", plan.getMealType());
                obj.put("date", plan.getDate().getTime());
                // Add other plan properties as needed
                array.put(obj);
            }
            preferences.edit().putString(KEY_PENDING_MEAL_PLANS, array.toString()).apply();
        } catch (JSONException e) {
            Log.e(TAG, "Error saving pending meal plans", e);
        }
    }
    
    private void removePendingMealPlan(MealPlanItem plan) {
        List<MealPlanItem> plans = getPendingMealPlans();
        plans.remove(plan);
        savePendingMealPlans(plans);
    }
    
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) 
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;
        
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }
    
    public void shutdown() {
        executorService.shutdown();
    }
} 