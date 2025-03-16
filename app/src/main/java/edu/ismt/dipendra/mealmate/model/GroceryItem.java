package edu.ismt.dipendra.mealmate.model;

import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;

public class GroceryItem {
    private String id;
    private String name;
    private String category;
    private double quantity;
    private String unit;
    private double price;
    private String note;
    private boolean completed;
    private boolean selected;
    private String storeId;
    private String storeName;
    private List<String> mealIds; // IDs of meals using this ingredient
    private String status; // AVAILABLE, PENDING_PURCHASE, PURCHASED
    private String delegatedTo; // Phone number if delegated via SMS
    private String emoji; // Emoji representation for SMS messages
    private String mealId; // Optional: reference to the meal this item is from
    private boolean purchased;
    
    // Empty constructor needed for Firestore
    public GroceryItem() {
        this.id = generateId();
        this.completed = false;
        this.selected = false;
        this.mealIds = new ArrayList<>();
        this.status = "AVAILABLE";
        this.price = 0.0;
    }
    
    // Constructor for creating items from recipes/managed items
    public GroceryItem(String name, boolean selected) {
        this.id = generateId();
        this.name = name;
        this.selected = selected;
        this.completed = false;
        this.category = "General"; // Default category
        this.mealIds = new ArrayList<>();
        this.status = "AVAILABLE";
        this.price = 0.0;
    }
    
    public GroceryItem(String name, String category, double quantity, String unit) {
        this.id = generateId();
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.unit = unit;
        this.completed = false;
        this.selected = false;
        this.mealIds = new ArrayList<>();
        this.status = "AVAILABLE";
        this.price = 0.0;
    }
    
    public GroceryItem(String name, String category, double quantity, String unit, double price) {
        this(name, category, quantity, unit);
        this.price = price;
    }
    
    public GroceryItem(String name, String category, String quantity, String storeId, String storeName) {
        this.id = generateId();
        this.name = name;
        this.category = category;
        try {
            this.quantity = Double.parseDouble(quantity);
        } catch (NumberFormatException e) {
            this.quantity = 1.0; // Default to 1.0 if parsing fails
        }
        this.storeId = storeId;
        this.storeName = storeName;
        this.completed = false;
        this.selected = false;
        this.mealIds = new ArrayList<>();
        this.status = "AVAILABLE";
    }
    
    // Generate a unique ID for the item
    private String generateId() {
        return "item_" + System.currentTimeMillis() + "_" + Math.round(Math.random() * 1000);
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
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public double getQuantity() {
        return quantity;
    }
    
    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }
    
    public String getUnit() {
        return unit;
    }
    
    public void setUnit(String unit) {
        this.unit = unit;
    }
    
    public String getNote() {
        return note;
    }
    
    public void setNote(String note) {
        this.note = note;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    @Exclude
    public boolean isSelected() {
        return selected;
    }
    
    public void setSelected(boolean selected) {
        this.selected = selected;
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
        this.completed = "PURCHASED".equals(status);
    }
    
    public String getDelegatedTo() {
        return delegatedTo;
    }
    
    public void setDelegatedTo(String delegatedTo) {
        this.delegatedTo = delegatedTo;
        if (delegatedTo != null && !delegatedTo.isEmpty()) {
            this.status = "PENDING_PURCHASE";
        }
    }
    
    public String getEmoji() {
        return emoji;
    }
    
    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }
    
    @Exclude
    public boolean isUsedInMeal(String mealId) {
        return mealIds.contains(mealId);
    }
    
    @Exclude
    public boolean hasNoMeals() {
        return mealIds.isEmpty();
    }
    
    @Exclude
    public String getDisplayStatus() {
        switch (status) {
            case "PENDING_PURCHASE":
                return "Pending";
            case "PURCHASED":
                return "Purchased";
            default:
                return "Available";
        }
    }
    
    @Exclude
    public boolean hasStore() {
        return storeId != null && !storeId.isEmpty();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        GroceryItem that = (GroceryItem) obj;
        
        if (!name.equals(that.name)) return false;
        return category.equals(that.category);
    }
    
    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + category.hashCode();
        return result;
    }
    
    @Exclude
    public String getFormattedItem() {
        return name + " (" + quantity + " " + unit + ")";
    }
    
    @Exclude
    public String getFormattedItemWithStore() {
        if (hasStore()) {
            return name + " (" + quantity + " " + unit + ") - Buy from " + storeName;
        } else {
            return getFormattedItem();
        }
    }
    
    public String getMealId() {
        return mealId;
    }
    
    public void setMealId(String mealId) {
        this.mealId = mealId;
    }
    
    public double getPrice() {
        return price;
    }
    
    public void setPrice(double price) {
        this.price = price;
    }
    
    public boolean isPurchased() {
        return purchased;
    }
    
    public void setPurchased(boolean purchased) {
        this.purchased = purchased;
        if (purchased) {
            this.status = "PURCHASED";
            this.completed = true;
        } else {
            this.status = "AVAILABLE";
            this.completed = false;
        }
    }
    
    public String getStoreName() {
        return storeName;
    }
    
    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }
} 