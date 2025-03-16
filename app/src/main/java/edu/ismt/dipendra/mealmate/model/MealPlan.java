package edu.ismt.dipendra.mealmate.model;

import com.google.firebase.firestore.Exclude;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MealPlan {
    
    @Exclude
    private String id;
    private String userId;
    private Date date;
    private Map<String, String> meals; // Category -> MealId mapping
    private Map<String, String> mealNames; // Category -> MealName for quick access
    
    // Required empty constructor for Firestore
    public MealPlan() {
        meals = new HashMap<>();
        mealNames = new HashMap<>();
    }
    
    public MealPlan(String userId, Date date) {
        this.userId = userId;
        this.date = date;
        this.meals = new HashMap<>();
        this.mealNames = new HashMap<>();
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public Date getDate() {
        return date;
    }
    
    public void setDate(Date date) {
        this.date = date;
    }
    
    public Map<String, String> getMeals() {
        return meals;
    }
    
    public void setMeals(Map<String, String> meals) {
        this.meals = meals;
    }
    
    public Map<String, String> getMealNames() {
        return mealNames;
    }
    
    public void setMealNames(Map<String, String> mealNames) {
        this.mealNames = mealNames;
    }
    
    public void addMeal(String category, String mealId, String mealName) {
        meals.put(category, mealId);
        mealNames.put(category, mealName);
    }
    
    public void removeMeal(String category) {
        meals.remove(category);
        mealNames.remove(category);
    }
    
    public String getMealId(String category) {
        return meals.get(category);
    }

    public String getMealName(String category) {
        return mealNames.get(category);
    }

    public boolean hasMeal(String category) {
        return meals.containsKey(category) && meals.get(category) != null;
    }

    @Override
    public String toString() {
        return "MealPlan{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", date=" + date +
                ", meals=" + meals +
                ", mealNames=" + mealNames +
                '}';
    }
} 