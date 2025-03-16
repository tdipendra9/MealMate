package edu.ismt.dipendra.mealmate.model;

import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroceryList {
    
    private String id;
    private String name;
    private String userId;
    private String delegatedBy;
    private Date createdAt;
    private Date updatedAt;
    private List<GroceryItem> items;
    private boolean isShared;
    private String sharedWithPhone;
    private List<String> mealIds; // IDs of meals included in this grocery list
    
    // Empty constructor needed for Firestore
    public GroceryList() {
        this.items = new ArrayList<>();
        this.mealIds = new ArrayList<>();
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }
    
    public GroceryList(String name, String userId) {
        this.name = name;
        this.userId = userId;
        this.items = new ArrayList<>();
        this.mealIds = new ArrayList<>();
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }
    
    public GroceryList(GroceryList other) {
        this.name = other.name;
        this.userId = other.userId;
        this.items = new ArrayList<>(other.items);
        this.mealIds = new ArrayList<>(other.mealIds);
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
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
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getDelegatedBy() {
        return delegatedBy;
    }
    
    public void setDelegatedBy(String delegatedBy) {
        this.delegatedBy = delegatedBy;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public Date getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public List<GroceryItem> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }
    
    public List<GroceryItem> getAllItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return new ArrayList<>(items);
    }
    
    public void setItems(List<GroceryItem> items) {
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
    }
    
    public void updateItems(List<GroceryItem> items) {
        setItems(items);
    }
    
    public void addItem(GroceryItem item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(item);
        this.updatedAt = new Date();
    }
    
    public void removeItem(GroceryItem item) {
        if (items != null) {
            items.remove(item);
            this.updatedAt = new Date();
        }
    }
    
    public void removePurchasedItems() {
        if (items != null) {
            items.removeIf(GroceryItem::isPurchased);
            this.updatedAt = new Date();
        }
    }
    
    public boolean isShared() {
        return isShared;
    }
    
    public void setShared(boolean shared) {
        isShared = shared;
    }
    
    public String getSharedWithPhone() {
        return sharedWithPhone;
    }
    
    public void setSharedWithPhone(String sharedWithPhone) {
        this.sharedWithPhone = sharedWithPhone;
    }
    
    @Exclude
    public Map<String, List<GroceryItem>> getItemsByCategory() {
        Map<String, List<GroceryItem>> categorizedItems = new HashMap<>();
        
        if (this.items != null) {
            for (GroceryItem item : this.items) {
                String category = item.getCategory();
                if (!categorizedItems.containsKey(category)) {
                    categorizedItems.put(category, new ArrayList<>());
                }
                categorizedItems.get(category).add(item);
            }
        }
        
        return categorizedItems;
    }
    
    @Exclude
    public int getTotalItemCount() {
        return items != null ? items.size() : 0;
    }
    
    @Exclude
    public int getPurchasedItemCount() {
        int count = 0;
        
        if (this.items != null) {
            for (GroceryItem item : this.items) {
                if (item.isPurchased()) {
                    count++;
                }
            }
        }
        
        return count;
    }

    public List<String> getMealIds() {
        return mealIds;
    }

    public void setMealIds(List<String> mealIds) {
        this.mealIds = mealIds;
    }

    public void addMealId(String mealId) {
        if (this.mealIds == null) {
            this.mealIds = new ArrayList<>();
        }
        if (!this.mealIds.contains(mealId)) {
            this.mealIds.add(mealId);
        }
    }

    public void removeMealId(String mealId) {
        if (this.mealIds != null) {
            this.mealIds.remove(mealId);
        }
    }
} 