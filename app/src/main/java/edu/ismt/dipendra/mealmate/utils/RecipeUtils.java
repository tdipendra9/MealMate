package edu.ismt.dipendra.mealmate.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class RecipeUtils {
    private static final Map<String, List<String>> mealTypeKeywords = new HashMap<>();
    private static final Map<String, List<String>> commonIngredientCombos = new HashMap<>();
    
    static {
        // Initialize meal type keywords
        mealTypeKeywords.put("BREAKFAST", Arrays.asList(
            "eggs", "pancake", "waffle", "cereal", "oatmeal", "toast",
            "breakfast", "morning", "brunch"
        ));
        
        mealTypeKeywords.put("LUNCH", Arrays.asList(
            "sandwich", "salad", "soup", "lunch", "noon", "wrap"
        ));
        
        mealTypeKeywords.put("DINNER", Arrays.asList(
            "dinner", "supper", "roast", "steak", "evening", "casserole"
        ));
        
        mealTypeKeywords.put("SNACK", Arrays.asList(
            "snack", "chips", "nuts", "fruit", "cookie", "crackers"
        ));
        
        // Initialize common ingredient combinations
        commonIngredientCombos.put("pasta", Arrays.asList(
            "tomato sauce", "garlic", "olive oil", "parmesan cheese", "basil"
        ));
        
        commonIngredientCombos.put("chicken", Arrays.asList(
            "salt", "pepper", "garlic powder", "olive oil", "herbs"
        ));
        
        commonIngredientCombos.put("salad", Arrays.asList(
            "lettuce", "tomatoes", "cucumber", "olive oil", "vinegar"
        ));
    }
    
    public static String suggestMealType(String mealName, List<String> ingredients) {
        String lowerMealName = mealName.toLowerCase();
        
        // Check meal name against keywords
        for (Map.Entry<String, List<String>> entry : mealTypeKeywords.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lowerMealName.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        
        // Check ingredients if meal name doesn't give clear indication
        int breakfastScore = 0;
        int lunchScore = 0;
        int dinnerScore = 0;
        int snackScore = 0;
        
        for (String ingredient : ingredients) {
            String lowerIngredient = ingredient.toLowerCase();
            for (String keyword : mealTypeKeywords.get("BREAKFAST")) {
                if (lowerIngredient.contains(keyword)) breakfastScore++;
            }
            for (String keyword : mealTypeKeywords.get("LUNCH")) {
                if (lowerIngredient.contains(keyword)) lunchScore++;
            }
            for (String keyword : mealTypeKeywords.get("DINNER")) {
                if (lowerIngredient.contains(keyword)) dinnerScore++;
            }
            for (String keyword : mealTypeKeywords.get("SNACK")) {
                if (lowerIngredient.contains(keyword)) snackScore++;
            }
        }
        
        // Return the meal type with highest score
        int maxScore = Math.max(Math.max(breakfastScore, lunchScore), 
                              Math.max(dinnerScore, snackScore));
        
        if (maxScore == 0) return "LUNCH"; // Default to lunch if no clear indication
        if (maxScore == breakfastScore) return "BREAKFAST";
        if (maxScore == lunchScore) return "LUNCH";
        if (maxScore == dinnerScore) return "DINNER";
        return "SNACK";
    }
    
    public static List<String> suggestIngredients(String mealName, List<String> currentIngredients) {
        List<String> suggestions = new ArrayList<>();
        String lowerMealName = mealName.toLowerCase();
        
        // Check if meal name matches any common combinations
        for (Map.Entry<String, List<String>> entry : commonIngredientCombos.entrySet()) {
            if (lowerMealName.contains(entry.getKey())) {
                for (String ingredient : entry.getValue()) {
                    if (!currentIngredients.contains(ingredient)) {
                        suggestions.add(ingredient);
                    }
                }
            }
        }
        
        return suggestions;
    }
    
    public static boolean isValidIngredient(String ingredient) {
        // Basic validation rules
        return ingredient != null 
            && !ingredient.trim().isEmpty()
            && ingredient.length() >= 2
            && !Pattern.matches(".*[0-9]+.*", ingredient); // No numbers in ingredient names
    }
    
    public static String standardizeIngredientName(String ingredient) {
        // Remove extra spaces and standardize format
        return ingredient.trim().toLowerCase()
                .replaceAll("\\s+", " ")
                .replaceAll("[^a-z\\s]", "");
    }
} 