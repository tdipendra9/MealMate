package edu.ismt.dipendra.mealmate.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.ismt.dipendra.mealmate.model.GroceryItem;

public class EmojiUtils {
    public static final String GROCERY_CART = "🛒";
    public static final String SHOPPING_BAGS = "🛍️";
    public static final String CHECK_MARK = "✅";
    public static final String CROSS_MARK = "❌";
    public static final String SHARE = "📤";
    public static final String EDIT = "✏️";
    public static final String DELETE = "🗑️";
    
    private static final Map<String, String> categoryEmojis = new HashMap<>();
    private static final Map<String, String> commonIngredientEmojis = new HashMap<>();
    
    static {
        // Category emojis
        categoryEmojis.put("Fruits", "🍎");
        categoryEmojis.put("Vegetables", "🥕");
        categoryEmojis.put("Dairy", "🥛");
        categoryEmojis.put("Meat", "🍗");
        categoryEmojis.put("Grains", "🌾");
        categoryEmojis.put("Spices", "🌶️");
        categoryEmojis.put("Beverages", "🥤");
        categoryEmojis.put("Snacks", "🍪");
        categoryEmojis.put("Baking", "🥖");
        categoryEmojis.put("General", "🛒");
        
        // Common ingredient emojis
        commonIngredientEmojis.put("apple", "🍎");
        commonIngredientEmojis.put("banana", "🍌");
        commonIngredientEmojis.put("orange", "🍊");
        commonIngredientEmojis.put("lemon", "🍋");
        commonIngredientEmojis.put("carrot", "🥕");
        commonIngredientEmojis.put("tomato", "🍅");
        commonIngredientEmojis.put("potato", "🥔");
        commonIngredientEmojis.put("milk", "🥛");
        commonIngredientEmojis.put("cheese", "🧀");
        commonIngredientEmojis.put("egg", "🥚");
        commonIngredientEmojis.put("chicken", "🍗");
        commonIngredientEmojis.put("meat", "🥩");
        commonIngredientEmojis.put("fish", "🐟");
        commonIngredientEmojis.put("bread", "🍞");
        commonIngredientEmojis.put("rice", "🍚");
        commonIngredientEmojis.put("pasta", "🍝");
        commonIngredientEmojis.put("salt", "🧂");
        commonIngredientEmojis.put("water", "💧");
        commonIngredientEmojis.put("coffee", "☕");
        commonIngredientEmojis.put("tea", "🫖");
    }
    
    private EmojiUtils() {
        // Private constructor to prevent instantiation
    }
    
    public static String getRandomGroceryEmoji() {
        String[] emojis = {GROCERY_CART, SHOPPING_BAGS};
        return emojis[(int) (Math.random() * emojis.length)];
    }
    
    public static String getPurchaseStatusEmoji(boolean isPurchased) {
        return isPurchased ? CHECK_MARK : CROSS_MARK;
    }
    
    public static String getEmojiForIngredient(String ingredient, String category) {
        // Convert to lowercase for case-insensitive matching
        String lowerIngredient = ingredient.toLowerCase();
        
        // First try to match common ingredients
        for (Map.Entry<String, String> entry : commonIngredientEmojis.entrySet()) {
            if (lowerIngredient.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        // If no ingredient match, use category emoji
        String categoryEmoji = categoryEmojis.get(category);
        return categoryEmoji != null ? categoryEmoji : GROCERY_CART; // Default to grocery cart if category not found
    }
    
    public static String formatSmsMessage(List<GroceryItem> items) {
        StringBuilder message = new StringBuilder("MealMate Shopping List:\n");
        
        for (GroceryItem item : items) {
            String emoji = item.getEmoji() != null ? item.getEmoji() : getEmojiForIngredient(item.getName(), item.getCategory());
            message.append(emoji).append(" ").append(item.getName());
            message.append(String.format(" (%.1f)", item.getQuantity()));
            
            if (item.hasStore()) {
                message.append(" - Buy from ").append(item.getStoreName());
            }
            message.append("\n");
        }
        
        return message.toString();
    }
} 