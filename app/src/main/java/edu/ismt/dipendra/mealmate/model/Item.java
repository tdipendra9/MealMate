package edu.ismt.dipendra.mealmate.model;

import com.google.firebase.firestore.Exclude;

import java.util.Date;

public class Item {
    
    @Exclude
    private String id;
    private String name;
    private String description;
    private double price;
    private boolean purchased;
    private Date createdAt;
    private String userId;
    private String category;
    private int quantity;
    
    // Empty constructor needed for Firestore
    public Item() {
        this.purchased = false;
        this.createdAt = new Date();
        this.quantity = 1; // Default quantity
    }
    
    public Item(String name, String description, double price, String userId) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.purchased = false;
        this.createdAt = new Date();
        this.userId = userId;
        this.quantity = 1; // Default quantity
    }
    
    public Item(String name, String description, double price, String userId, String category, int quantity) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.purchased = false;
        this.createdAt = new Date();
        this.userId = userId;
        this.category = category;
        this.quantity = quantity;
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
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
} 