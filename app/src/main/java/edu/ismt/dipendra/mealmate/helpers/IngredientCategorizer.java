package edu.ismt.dipendra.mealmate.helpers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class to categorize ingredients into food groups
 */
public class IngredientCategorizer {

    // Food categories
    public static final String CATEGORY_VEGETABLES = "Vegetables";
    public static final String CATEGORY_FRUITS = "Fruits";
    public static final String CATEGORY_GRAINS = "Grains & Pasta";
    public static final String CATEGORY_PROTEINS = "Proteins";
    public static final String CATEGORY_DAIRY = "Dairy";
    public static final String CATEGORY_SPICES = "Spices & Herbs";
    public static final String CATEGORY_OILS = "Oils & Condiments";
    public static final String CATEGORY_BAKING = "Baking";
    public static final String CATEGORY_CANNED = "Canned & Jarred";
    public static final String CATEGORY_OTHER = "Other";

    // Maps for ingredient categorization
    private static final Map<String, Set<String>> categoryMap = new HashMap<>();

    // Initialize the category map
    static {
        // Vegetables
        categoryMap.put(CATEGORY_VEGETABLES, new HashSet<>(Arrays.asList(
                "onion", "garlic", "tomato", "potato", "carrot", "bell pepper", "pepper", "lettuce",
                "spinach", "kale", "broccoli", "cauliflower", "cucumber", "zucchini", "eggplant",
                "celery", "asparagus", "corn", "green bean", "pea", "mushroom", "cabbage",
                "brussels sprout", "artichoke", "leek", "shallot", "scallion", "radish", "turnip",
                "beetroot", "beet", "squash", "pumpkin", "sweet potato", "yam", "okra", "ginger"
        )));

        // Fruits
        categoryMap.put(CATEGORY_FRUITS, new HashSet<>(Arrays.asList(
                "apple", "banana", "orange", "lemon", "lime", "grapefruit", "pear", "peach",
                "plum", "cherry", "grape", "strawberry", "blueberry", "raspberry", "blackberry",
                "cranberry", "pineapple", "mango", "kiwi", "watermelon", "melon", "cantaloupe",
                "avocado", "coconut", "fig", "date", "apricot", "pomegranate", "papaya", "guava"
        )));

        // Grains & Pasta
        categoryMap.put(CATEGORY_GRAINS, new HashSet<>(Arrays.asList(
                "rice", "pasta", "noodle", "spaghetti", "macaroni", "penne", "fettuccine", "linguine",
                "bread", "flour", "oat", "barley", "quinoa", "couscous", "bulgur", "cornmeal",
                "cereal", "tortilla", "pita", "bagel", "roll", "cracker", "breadcrumb", "panko",
                "wheat", "rye", "buckwheat", "millet", "amaranth", "teff", "farro", "orzo"
        )));

        // Proteins & Meat
        categoryMap.put(CATEGORY_PROTEINS, new HashSet<>(Arrays.asList(
                "chicken", "beef", "pork", "lamb", "turkey", "duck", "fish", "salmon", "tuna",
                "shrimp", "prawn", "crab", "lobster", "clam", "mussel", "oyster", "scallop",
                "tofu", "tempeh", "seitan", "bean", "lentil", "chickpea", "pea", "egg", "sausage",
                "bacon", "ham", "steak", "ground beef", "ground turkey", "ground chicken", "mince",
                "nut", "seed", "almond", "walnut", "pecan", "cashew", "peanut", "pistachio",
                "sunflower seed", "pumpkin seed", "chia seed", "flax seed", "hemp seed"
        )));

        // Dairy
        categoryMap.put(CATEGORY_DAIRY, new HashSet<>(Arrays.asList(
                "milk", "cream", "cheese", "cheddar", "mozzarella", "parmesan", "feta", "gouda",
                "brie", "ricotta", "cottage cheese", "yogurt", "butter", "ghee", "sour cream",
                "buttermilk", "whey", "ice cream", "custard", "pudding", "kefir", "mascarpone"
        )));

        // Spices & Herbs
        categoryMap.put(CATEGORY_SPICES, new HashSet<>(Arrays.asList(
                "salt", "pepper", "cumin", "coriander", "turmeric", "paprika", "chili powder",
                "cayenne", "cinnamon", "nutmeg", "clove", "allspice", "cardamom", "star anise",
                "fennel", "mustard", "oregano", "basil", "thyme", "rosemary", "sage", "parsley",
                "cilantro", "dill", "mint", "bay leaf", "tarragon", "marjoram", "saffron", "vanilla",
                "ginger", "garlic powder", "onion powder", "curry powder", "garam masala", "za'atar"
        )));

        // Oils & Condiments
        categoryMap.put(CATEGORY_OILS, new HashSet<>(Arrays.asList(
                "oil", "olive oil", "vegetable oil", "canola oil", "coconut oil", "sesame oil",
                "vinegar", "balsamic vinegar", "wine vinegar", "apple cider vinegar", "rice vinegar",
                "soy sauce", "fish sauce", "oyster sauce", "hoisin sauce", "teriyaki sauce",
                "hot sauce", "sriracha", "tabasco", "ketchup", "mustard", "mayonnaise", "aioli",
                "salsa", "pesto", "hummus", "tahini", "miso", "honey", "maple syrup", "molasses",
                "worcestershire sauce", "bbq sauce"
        )));

        // Baking
        categoryMap.put(CATEGORY_BAKING, new HashSet<>(Arrays.asList(
                "flour", "sugar", "brown sugar", "powdered sugar", "baking powder", "baking soda",
                "yeast", "cocoa", "chocolate", "chocolate chip", "vanilla extract", "almond extract",
                "food coloring", "sprinkle", "frosting", "icing", "corn starch", "gelatin", "pectin",
                "cream of tartar", "shortening", "lard", "marzipan", "fondant", "cake mix", "brownie mix"
        )));

        // Canned & Jarred
        categoryMap.put(CATEGORY_CANNED, new HashSet<>(Arrays.asList(
                "canned", "jarred", "tomato sauce", "tomato paste", "marinara", "salsa", "broth",
                "stock", "soup", "beans", "chickpeas", "lentils", "corn", "peas", "carrots",
                "olives", "pickles", "relish", "jam", "jelly", "preserves", "tuna", "salmon",
                "sardines", "anchovies", "coconut milk", "condensed milk", "evaporated milk"
        )));
    }

    /**
     * Categorize an ingredient based on its name
     *
     * @param ingredient The ingredient name
     * @return The category of the ingredient
     */
    public static String categorizeIngredient(String ingredient) {
        if (ingredient == null || ingredient.trim().isEmpty()) {
            return CATEGORY_OTHER;
        }

        // Extract the base ingredient name (remove quantities, units, etc.)
        String baseIngredient = extractBaseIngredient(ingredient.toLowerCase());

        // Check each category
        for (Map.Entry<String, Set<String>> entry : categoryMap.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (baseIngredient.contains(keyword) || keyword.contains(baseIngredient)) {
                    return entry.getKey();
                }
            }
        }

        return CATEGORY_OTHER;
    }

    /**
     * Extract the base ingredient name from a full ingredient string
     * (e.g., "2 cups of flour" -> "flour")
     *
     * @param ingredient The full ingredient string
     * @return The base ingredient name
     */
    public static String extractBaseIngredient(String ingredient) {
        // Remove quantities (numbers and fractions)
        String cleaned = ingredient.replaceAll("\\d+(\\.\\d+)?\\s*", "");
        cleaned = cleaned.replaceAll("\\d+/\\d+\\s*", "");
        
        // Remove common units
        cleaned = cleaned.replaceAll("\\b(cup|cups|tablespoon|tablespoons|tbsp|teaspoon|teaspoons|tsp|ounce|ounces|oz|pound|pounds|lb|lbs|gram|grams|g|kilogram|kilograms|kg|ml|milliliter|milliliters|liter|liters|l|gallon|gallons|quart|quarts|pint|pints|slice|slices|piece|pieces|bunch|bunches|clove|cloves|head|heads|stalk|stalks|sprig|sprigs|pinch|pinches|dash|dashes)s?\\b", "");
        
        // Remove common prepositions and articles
        cleaned = cleaned.replaceAll("\\b(of|with|and|or|the|a|an)\\b", "");
        
        // Remove extra spaces and trim
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        
        return cleaned;
    }

    /**
     * Extract quantity and unit from an ingredient string
     *
     * @param ingredient The full ingredient string
     * @return The quantity and unit as a string (e.g., "2 cups")
     */
    public static String extractQuantityAndUnit(String ingredient) {
        if (ingredient == null || ingredient.trim().isEmpty()) {
            return "";
        }
        
        // Pattern to match quantity (number or fraction) followed by optional unit
        Pattern pattern = Pattern.compile("(\\d+(?:\\.\\d+)?|\\d+/\\d+)\\s*([a-zA-Z]+)?");
        Matcher matcher = pattern.matcher(ingredient);
        
        if (matcher.find()) {
            String quantity = matcher.group(1);
            String unit = matcher.groupCount() > 1 && matcher.group(2) != null ? matcher.group(2) : "";
            return quantity + (unit.isEmpty() ? "" : " " + unit);
        }
        
        return "";
    }
} 