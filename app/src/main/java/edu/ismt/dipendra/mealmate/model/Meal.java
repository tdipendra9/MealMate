package edu.ismt.dipendra.mealmate.model;

import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Meal implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String name;
    private String description;
    private List<String> ingredients;
    private String instructions;
    private int preparationTime; // in minutes
    private int cookingTime; // in minutes
    private int servings;
    private String category; // e.g., breakfast, lunch, dinner, dessert
    private String imageUrl; // optional, for future use
    private Date createdAt;
    private String userId;
    private String sourceUrl; // URL of the imported recipe
    private String sourceName; // Name of the source website
    private int calories; // calories per serving
    
    // Empty constructor needed for Firestore
    public Meal() {
        this.ingredients = new ArrayList<>();
        this.createdAt = new Date();
    }
    
    public Meal(String name, String description, List<String> ingredients, 
                String instructions, int preparationTime, int cookingTime, 
                int servings, String category, String userId) {
        this.name = name;
        this.description = description;
        this.ingredients = ingredients;
        this.instructions = instructions;
        this.preparationTime = preparationTime;
        this.cookingTime = cookingTime;
        this.servings = servings;
        this.category = category;
        this.createdAt = new Date();
        this.userId = userId;
        this.calories = 0; // Default value
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<String> getIngredients() {
        return ingredients;
    }
    
    public void setIngredients(List<String> ingredients) {
        this.ingredients = ingredients;
    }
    
    public void addIngredient(String ingredient) {
        if (this.ingredients == null) {
            this.ingredients = new ArrayList<>();
        }
        this.ingredients.add(ingredient);
    }
    
    public void removeIngredient(String ingredient) {
        if (this.ingredients != null) {
            this.ingredients.remove(ingredient);
        }
    }
    
    public String getInstructions() {
        return instructions;
    }
    
    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
    
    public int getPreparationTime() {
        return preparationTime;
    }
    
    public void setPreparationTime(int preparationTime) {
        this.preparationTime = preparationTime;
    }
    
    public int getCookingTime() {
        return cookingTime;
    }
    
    public void setCookingTime(int cookingTime) {
        this.cookingTime = cookingTime;
    }
    
    public int getServings() {
        return servings;
    }
    
    public void setServings(int servings) {
        this.servings = servings;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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
    
    public String getSourceUrl() {
        return sourceUrl;
    }
    
    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
    
    public String getSourceName() {
        return sourceName;
    }
    
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }
    
    // Helper method to check if this is an imported recipe
    public boolean isImported() {
        return sourceUrl != null && !sourceUrl.isEmpty();
    }
    
    // Helper method to get total time (prep + cooking)
    public int getTotalTime() {
        return preparationTime + cookingTime;
    }
    
    // Add getter and setter for calories
    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }
} 