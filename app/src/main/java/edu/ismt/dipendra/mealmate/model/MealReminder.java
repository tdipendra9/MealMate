package edu.ismt.dipendra.mealmate.model;

import com.google.firebase.firestore.DocumentId;

/**
 * Model class for meal preparation reminders
 */
public class MealReminder {
    
    @DocumentId
    private String id;
    private String userId;
    private String mealId;
    private String mealName;
    private long reminderTime;
    private long cookingTime;
    private boolean isActive;
    
    // Required empty constructor for Firestore
    public MealReminder() {
    }
    
    public MealReminder(String userId, String mealId, String mealName, long reminderTime, long cookingTime) {
        this.userId = userId;
        this.mealId = mealId;
        this.mealName = mealName;
        this.reminderTime = reminderTime;
        this.cookingTime = cookingTime;
        this.isActive = true;
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
    
    public long getReminderTime() {
        return reminderTime;
    }
    
    public void setReminderTime(long reminderTime) {
        this.reminderTime = reminderTime;
    }
    
    public long getCookingTime() {
        return cookingTime;
    }
    
    public void setCookingTime(long cookingTime) {
        this.cookingTime = cookingTime;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    @Override
    public String toString() {
        return "MealReminder{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", mealId='" + mealId + '\'' +
                ", mealName='" + mealName + '\'' +
                ", reminderTime=" + reminderTime +
                ", cookingTime=" + cookingTime +
                ", isActive=" + isActive +
                '}';
    }
} 