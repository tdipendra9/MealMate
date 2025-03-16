package edu.ismt.dipendra.mealmate.model;

import com.google.firebase.firestore.Exclude;

import java.util.Date;

public class MealPlanItem {
    
    @Exclude
    private String id;
    private String mealId; // Reference to an existing meal, if using one
    private String mealName;
    private String mealType; // Breakfast, Lunch, Dinner, Snacks
    private String ingredients;
    private String instructions;
    private int calories;
    private int servings;
    private Date date; // The date this meal is planned for
    private Date createdAt;
    private String userId;
    private String imageUrl;
    
    // Empty constructor needed for Firestore
    public MealPlanItem() {
        this.createdAt = new Date();
        this.date = new Date(); // Default to today
    }
    
    // Constructor for creating a new meal plan with a new meal
    public MealPlanItem(String mealName, String mealType, String ingredients, 
                   String instructions, int calories, int servings, 
                   Date date, String userId) {
        this.mealName = mealName;
        this.mealType = mealType;
        this.ingredients = ingredients;
        this.instructions = instructions;
        this.calories = calories;
        this.servings = servings;
        this.date = date;
        this.createdAt = new Date();
        this.userId = userId;
    }
    
    // Constructor for creating a meal plan with an existing meal
    public MealPlanItem(String mealId, String mealName, String mealType, 
                   int calories, int servings, Date date, String userId, String imageUrl) {
        this.mealId = mealId;
        this.mealName = mealName;
        this.mealType = mealType;
        this.calories = calories;
        this.servings = servings;
        this.date = date;
        this.createdAt = new Date();
        this.userId = userId;
        this.imageUrl = imageUrl;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getMealId() {
        return mealId;
    }
    
    public void setMealId(String mealId) {
        this.mealId = mealId;
    }
    
    public String getMealName() {
        return mealName;
    }
    
    public void setMealName(String mealName) {
        this.mealName = mealName;
    }
    
    public String getMealType() {
        return mealType;
    }
    
    public void setMealType(String mealType) {
        this.mealType = mealType;
    }
    
    public String getIngredients() {
        return ingredients;
    }
    
    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }
    
    public String getInstructions() {
        return instructions;
    }
    
    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
    
    public int getCalories() {
        return calories;
    }
    
    public void setCalories(int calories) {
        this.calories = calories;
    }
    
    public int getServings() {
        return servings;
    }
    
    public void setServings(int servings) {
        this.servings = servings;
    }
    
    public Date getDate() {
        return date;
    }
    
    public void setDate(Date date) {
        this.date = date;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    // Helper method to check if this is based on an existing meal
    public boolean isExistingMeal() {
        return mealId != null && !mealId.isEmpty();
    }
} 