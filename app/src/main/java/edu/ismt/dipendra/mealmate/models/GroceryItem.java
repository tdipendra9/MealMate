package edu.ismt.dipendra.mealmate.models;

import java.io.Serializable;

public class GroceryItem implements Serializable {
    private String id;
    private String name;
    private double amount;
    private String unit;
    private double price;
    private String date;

    // Empty constructor for Firebase
    public GroceryItem() {
    }

    public GroceryItem(String id, String name, double amount, String unit, double price, String date) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.unit = unit;
        this.price = price;
        this.date = date;
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

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getTotalPrice() {
        return amount * price;
    }
} 