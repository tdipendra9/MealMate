package edu.ismt.dipendra.mealmate.model;

import java.util.ArrayList;
import java.util.List;

public class IngredientMealRelation {
    private String ingredientId;
    private String ingredientName;
    private List<String> mealIds;
    private String status; // AVAILABLE, PENDING_PURCHASE, PURCHASED
    private String delegatedTo; // Phone number if delegated via SMS
    
    public IngredientMealRelation() {
        this.mealIds = new ArrayList<>();
        this.status = "AVAILABLE";
    }
    
    public String getIngredientId() {
        return ingredientId;
    }
    
    public void setIngredientId(String ingredientId) {
        this.ingredientId = ingredientId;
    }
    
    public String getIngredientName() {
        return ingredientName;
    }
    
    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }
    
    public List<String> getMealIds() {
        return mealIds;
    }
    
    public void setMealIds(List<String> mealIds) {
        this.mealIds = mealIds;
    }
    
    public void addMealId(String mealId) {
        if (!this.mealIds.contains(mealId)) {
            this.mealIds.add(mealId);
        }
    }
    
    public void removeMealId(String mealId) {
        this.mealIds.remove(mealId);
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getDelegatedTo() {
        return delegatedTo;
    }
    
    public void setDelegatedTo(String delegatedTo) {
        this.delegatedTo = delegatedTo;
    }
    
    public boolean isUsedInMeal(String mealId) {
        return mealIds.contains(mealId);
    }
    
    public boolean hasNoMeals() {
        return mealIds.isEmpty();
    }
} 