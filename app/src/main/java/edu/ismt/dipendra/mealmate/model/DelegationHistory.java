package edu.ismt.dipendra.mealmate.model;

import java.util.Date;
import java.util.List;

public class DelegationHistory {
    private String id;
    private String groceryListId;
    private String phoneNumber;
    private Date timestamp;
    private List<GroceryItem> items;
    private String status; // SENT, COMPLETED, FAILED
    
    public DelegationHistory() {
        // Required empty constructor for Firestore
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getGroceryListId() {
        return groceryListId;
    }
    
    public void setGroceryListId(String groceryListId) {
        this.groceryListId = groceryListId;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    
    public List<GroceryItem> getItems() {
        return items;
    }
    
    public void setItems(List<GroceryItem> items) {
        this.items = items;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
} 