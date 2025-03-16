package edu.ismt.dipendra.mealmate.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class for recipes
 */
public class Recipe {
    private String id;
    private String name;
    private String category;
    private String cookTime;
    private int imageResourceId;
    private String description;
    private List<String> ingredients;
    private List<String> instructions;
    private int servings;
    private String difficulty;
    private boolean isFavorite;
    private String userId;
    private long createdAt;

    public Recipe() {
        // Required empty constructor for Firebase
        this.ingredients = new ArrayList<>();
        this.instructions = new ArrayList<>();
    }

    public Recipe(String name, String category, String cookTime, int imageResourceId) {
        this.name = name;
        this.category = category;
        this.cookTime = cookTime;
        this.imageResourceId = imageResourceId;
        this.ingredients = new ArrayList<>();
        this.instructions = new ArrayList<>();
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

    public String getCookTime() {
        return cookTime;
    }

    public void setCookTime(String cookTime) {
        this.cookTime = cookTime;
    }

    public int getImageResourceId() {
        return imageResourceId;
    }

    public void setImageResourceId(int imageResourceId) {
        this.imageResourceId = imageResourceId;
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

    public List<String> getInstructions() {
        return instructions;
    }

    public void setInstructions(List<String> instructions) {
        this.instructions = instructions;
    }

    public int getServings() {
        return servings;
    }

    public void setServings(int servings) {
        this.servings = servings;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
} 