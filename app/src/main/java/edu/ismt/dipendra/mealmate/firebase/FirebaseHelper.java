package edu.ismt.dipendra.mealmate.firebase;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import edu.ismt.dipendra.mealmate.model.GeotaggedStore;
import edu.ismt.dipendra.mealmate.model.GroceryList;
import edu.ismt.dipendra.mealmate.model.Item;
import edu.ismt.dipendra.mealmate.model.Meal;
import edu.ismt.dipendra.mealmate.model.MealPlan;
import edu.ismt.dipendra.mealmate.model.MealPlanItem;
import edu.ismt.dipendra.mealmate.model.MealReminder;
import edu.ismt.dipendra.mealmate.model.User;
import edu.ismt.dipendra.mealmate.model.GroceryItem;
import edu.ismt.dipendra.mealmate.model.IngredientMealRelation;
import edu.ismt.dipendra.mealmate.model.DelegationHistory;
import edu.ismt.dipendra.mealmate.utils.EmojiUtils;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    private static final String USERS_COLLECTION = "users";
    private static final String MEALS_COLLECTION = "meals";
    private static final String RECIPE_COLLECTION = "recipe";
    private static final String MEAL_PLAN_ITEMS_COLLECTION = "meal_plan_items";
    private static final String MEAL_REMINDERS_COLLECTION = "meal_reminders";
    private static final String GROCERY_LISTS_COLLECTION = "grocery_lists";
    private static final String GROCERY_ITEMS_COLLECTION = "grocery_items";
    private static final String ITEMS_COLLECTION = "items";
    private static final String STORES_COLLECTION = "stores";
    private static final String IMAGES_STORAGE = "images";
    private static final String INGREDIENT_RELATIONS_COLLECTION = "ingredient_relations";
    private static final String DELEGATION_HISTORY_COLLECTION = "delegation_history";

    private static FirebaseHelper instance;
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore mFirestore;
    private final FirebaseStorage mStorage;
    private final FirebaseDatabase firebaseDatabase;

    public interface FirebaseCallback {
        void onSuccess(Object result);
        void onFailure(Exception e);
    }

    public interface UserCallback {
        void onUserFound(FirebaseUser user);
    }

    private FirebaseHelper() {
        firebaseAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mStorage = FirebaseStorage.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
    }

    public static synchronized FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    public FirebaseAuth getAuth() {
        return firebaseAuth;
    }

    public DatabaseReference getDatabase() {
        return firebaseDatabase.getReference();
    }

    // Auth methods
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public String getCurrentUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public boolean isUserSignedIn() {
        return getCurrentUser() != null;
    }

    public void signOut() {
        firebaseAuth.signOut();
    }

    public void loginUser(String email, String password, final FirebaseCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> callback.onSuccess(authResult.getUser()))
                .addOnFailureListener(callback::onFailure);
    }

    public void registerUser(User user, final FirebaseCallback callback) {
        firebaseAuth.createUserWithEmailAndPassword(user.getEmail(), user.getPassword())
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        user.setUid(firebaseUser.getUid());
                        mFirestore.collection(USERS_COLLECTION)
                                .document(user.getUid())
                                .set(user)
                                .addOnSuccessListener(aVoid -> callback.onSuccess(user))
                                .addOnFailureListener(callback::onFailure);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getCurrentUserData(final FirebaseCallback callback) {
        FirebaseUser currentUser = getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        mFirestore.collection(USERS_COLLECTION)
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        callback.onSuccess(user);
                    } else {
                        callback.onFailure(new Exception("User data not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    // Meal methods
    public void addMeal(Meal meal, final FirebaseCallback callback) {
        // First save the meal
        mFirestore.collection(MEALS_COLLECTION)
                .document(meal.getId())
                .set(meal)
                .addOnSuccessListener(aVoid -> {
                    // Now update ingredient relations
                    updateIngredientRelations(meal, callback);
                })
                .addOnFailureListener(callback::onFailure);
    }

    private void updateIngredientRelations(Meal meal, final FirebaseCallback callback) {
        List<String> ingredients = meal.getIngredients();
        List<Task<Void>> tasks = new ArrayList<>();

        for (String ingredient : ingredients) {
            // Create or update ingredient relation
            DocumentReference relationRef = mFirestore.collection(INGREDIENT_RELATIONS_COLLECTION)
                    .document(ingredient.toLowerCase().trim());

            Task<Void> task = relationRef.get()
                    .continueWithTask(snapshot -> {
                        IngredientMealRelation relation;
                        if (snapshot.isSuccessful() && snapshot.getResult().exists()) {
                            relation = snapshot.getResult().toObject(IngredientMealRelation.class);
                        } else {
                            relation = new IngredientMealRelation();
                            relation.setIngredientName(ingredient);
                        }
                        relation.addMealId(meal.getId());
                        return relationRef.set(relation);
                    });

            tasks.add(task);

            // Also update grocery items that use this ingredient
            updateGroceryItemsForIngredient(ingredient, meal.getId());
        }

        Tasks.whenAllComplete(tasks)
                .addOnSuccessListener(v -> callback.onSuccess(meal))
                .addOnFailureListener(callback::onFailure);
    }

    private void updateGroceryItemsForIngredient(String ingredient, String mealId) {
        mFirestore.collection(GROCERY_LISTS_COLLECTION)
                .whereEqualTo("userId", getCurrentUserId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        GroceryList list = doc.toObject(GroceryList.class);
                        if (list != null) {
                            boolean updated = false;
                            for (GroceryItem item : list.getItems()) {
                                if (item.getName().equalsIgnoreCase(ingredient)) {
                                    item.addMealId(mealId);
                                    updated = true;
                                }
                            }
                            if (updated) {
                                doc.getReference().set(list);
                            }
                        }
                    }
                });
    }

    public void updateMeal(Meal meal, final FirebaseCallback callback) {
        if (meal.getId() == null) {
            callback.onFailure(new Exception("Meal ID cannot be null"));
            return;
        }

        mFirestore.collection(MEALS_COLLECTION)
                .document(meal.getId())
                .set(meal)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void deleteMeal(String mealId, final FirebaseCallback callback) {
        // First get the meal to know which ingredients to update
        mFirestore.collection(MEALS_COLLECTION)
                .document(mealId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Meal meal = documentSnapshot.toObject(Meal.class);
                    if (meal != null) {
                        // Remove meal from ingredient relations
                        removeIngredientRelations(meal, () -> {
                            // Then delete the meal
                            documentSnapshot.getReference().delete()
                                    .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                                    .addOnFailureListener(callback::onFailure);
                        });
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    private void removeIngredientRelations(Meal meal, Runnable onComplete) {
        List<Task<Void>> tasks = new ArrayList<>();

        for (String ingredient : meal.getIngredients()) {
            DocumentReference relationRef = mFirestore.collection(INGREDIENT_RELATIONS_COLLECTION)
                    .document(ingredient.toLowerCase().trim());

            Task<Void> task = relationRef.get()
                    .continueWithTask(snapshot -> {
                        if (snapshot.isSuccessful() && snapshot.getResult().exists()) {
                            IngredientMealRelation relation = snapshot.getResult().toObject(IngredientMealRelation.class);
                            if (relation != null) {
                                relation.removeMealId(meal.getId());
                                if (relation.hasNoMeals()) {
                                    return relationRef.delete();
                                } else {
                                    return relationRef.set(relation);
                                }
                            }
                        }
                        return Tasks.forResult(null);
                    });

            tasks.add(task);

            // Also update grocery items
            removeGroceryItemsForIngredient(ingredient, meal.getId());
        }

        Tasks.whenAllComplete(tasks)
                .addOnSuccessListener(v -> onComplete.run());
    }

    private void removeGroceryItemsForIngredient(String ingredient, String mealId) {
        mFirestore.collection(GROCERY_LISTS_COLLECTION)
                .whereEqualTo("userId", getCurrentUserId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        GroceryList list = doc.toObject(GroceryList.class);
                        if (list != null) {
                            boolean updated = false;
                            List<GroceryItem> itemsToRemove = new ArrayList<>();
                            
                            for (GroceryItem item : list.getItems()) {
                                if (item.getName().equalsIgnoreCase(ingredient)) {
                                    item.removeMealId(mealId);
                                    if (item.hasNoMeals()) {
                                        itemsToRemove.add(item);
                                    }
                                    updated = true;
                                }
                            }
                            
                            if (!itemsToRemove.isEmpty()) {
                                list.getItems().removeAll(itemsToRemove);
                            }
                            
                            if (updated) {
                                doc.getReference().set(list);
                            }
                        }
                    }
                });
    }

    public void getMealById(String mealId, final FirebaseCallback callback) {
        mFirestore.collection(MEALS_COLLECTION)
                .document(mealId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Meal meal = documentSnapshot.toObject(Meal.class);
                    if (meal != null) {
                        meal.setId(documentSnapshot.getId());
                        callback.onSuccess(meal);
                    } else {
                        callback.onFailure(new Exception("Meal not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getUserMeals(final FirebaseCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        mFirestore.collection(MEALS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Meal> meals = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot) {
                        Meal meal = document.toObject(Meal.class);
                        if (meal != null) {
                            meal.setId(document.getId());
                            meals.add(meal);
                        }
                    }
                    callback.onSuccess(meals);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // Image handling methods
    public boolean isValidImageUrl(String imageUrl) {
        return imageUrl != null && !imageUrl.isEmpty() && 
               (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"));
    }

    public void uploadImage(Uri imageUri, final FirebaseCallback callback) {
        if (imageUri == null) {
            callback.onFailure(new Exception("Image URI cannot be null"));
            return;
        }

        String imageName = UUID.randomUUID().toString();
        StorageReference imageRef = mStorage.getReference()
                .child(IMAGES_STORAGE)
                .child(imageName);

        imageRef.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return imageRef.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> callback.onSuccess(uri.toString()))
                .addOnFailureListener(callback::onFailure);
    }

    // Meal Plan methods
    public void getMealPlanByDate(Date date, final FirebaseCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startDate = calendar.getTime();

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.add(Calendar.MILLISECOND, -1);
        Date endDate = calendar.getTime();

        mFirestore.collection(MEAL_PLAN_ITEMS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    MealPlan mealPlan = new MealPlan(userId, date);
                    for (DocumentSnapshot document : querySnapshot) {
                        MealPlanItem item = document.toObject(MealPlanItem.class);
                        if (item != null) {
                            item.setId(document.getId());
                            mealPlan.addMeal(item.getMealType(), item.getMealId(), item.getMealName());
                        }
                    }
                    callback.onSuccess(mealPlan);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // Meal Plan Item methods
    public void getMealPlanItemById(String mealPlanItemId, final FirebaseCallback callback) {
        if (mealPlanItemId == null || mealPlanItemId.isEmpty()) {
            callback.onFailure(new Exception("Meal plan item ID cannot be null or empty"));
            return;
        }
        
        mFirestore.collection(MEAL_PLAN_ITEMS_COLLECTION)
                .document(mealPlanItemId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        MealPlanItem mealPlanItem = documentSnapshot.toObject(MealPlanItem.class);
                        if (mealPlanItem != null) {
                            mealPlanItem.setId(documentSnapshot.getId());
                            callback.onSuccess(mealPlanItem);
                        } else {
                            callback.onFailure(new Exception("Failed to convert document to MealPlanItem"));
                        }
                    } else {
                        callback.onFailure(new Exception("Meal plan item not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getMealPlanItemsByDate(Date date, final FirebaseCallback callback) {
        try {
            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
            if (currentUser == null) {
                Log.e(TAG, "getMealPlanItemsByDate: User not authenticated");
                callback.onFailure(new Exception("User not authenticated"));
                return;
            }
            
            if (date == null) {
                Log.e(TAG, "getMealPlanItemsByDate: Date is null");
                callback.onFailure(new Exception("Date cannot be null"));
                return;
            }
            
            // Create start and end dates for the query (start of day to end of day)
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Date startDate = calendar.getTime();
            
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            calendar.add(Calendar.MILLISECOND, -1);
            final Date endDate = calendar.getTime();
            
            Log.d(TAG, "Querying meal plans for user: " + currentUser.getUid() + 
                    ", date range: " + startDate + " to " + endDate);
            
            // Temporary workaround until index is built - just query by userId and filter dates in memory
            mFirestore.collection(MEAL_PLAN_ITEMS_COLLECTION)
                    .whereEqualTo("userId", currentUser.getUid())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        try {
                            List<MealPlanItem> mealPlanItems = new ArrayList<>();
                            Log.d(TAG, "Query returned " + queryDocumentSnapshots.size() + " documents");
                            
                            for (DocumentSnapshot document : queryDocumentSnapshots) {
                                try {
                                    MealPlanItem mealPlanItem = document.toObject(MealPlanItem.class);
                                    if (mealPlanItem != null) {
                                        // Filter by date in memory
                                        Date itemDate = mealPlanItem.getDate();
                                        if (itemDate != null && 
                                            !itemDate.before(startDate) && 
                                            !itemDate.after(endDate)) {
                                            mealPlanItem.setId(document.getId());
                                            mealPlanItems.add(mealPlanItem);
                                            Log.d(TAG, "Added meal plan: " + mealPlanItem.getMealName() + 
                                                    ", type: " + mealPlanItem.getMealType());
                                        }
                                    } else {
                                        Log.w(TAG, "Failed to convert document to MealPlanItem: " + document.getId());
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error processing document: " + document.getId(), e);
                                }
                            }
                            
                            Log.d(TAG, "Returning " + mealPlanItems.size() + " meal plan items");
                            callback.onSuccess(mealPlanItems);
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing query results", e);
                            callback.onFailure(e);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Query failed", e);
                        callback.onFailure(e);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in getMealPlanItemsByDate", e);
            callback.onFailure(e);
        }
    }

    public void getMealPlanItemsByDateAndType(Date date, String mealType, final FirebaseCallback callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }
        
        // Create start and end dates for the query (start of day to end of day)
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startDate = calendar.getTime();
        
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.add(Calendar.MILLISECOND, -1);
        Date endDate = calendar.getTime();
        
        mFirestore.collection(MEAL_PLAN_ITEMS_COLLECTION)
                .whereEqualTo("userId", currentUser.getUid())
                .whereEqualTo("mealType", mealType)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<MealPlanItem> mealPlanItems = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        MealPlanItem mealPlanItem = document.toObject(MealPlanItem.class);
                        if (mealPlanItem != null) {
                            mealPlanItem.setId(document.getId());
                            mealPlanItems.add(mealPlanItem);
                        }
                    }
                    callback.onSuccess(mealPlanItems);
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    public void deleteMealPlanItem(String mealPlanItemId, final FirebaseCallback callback) {
        if (mealPlanItemId == null || mealPlanItemId.isEmpty()) {
            callback.onFailure(new Exception("Meal plan item ID cannot be null or empty"));
            return;
        }
        
        mFirestore.collection(MEAL_PLAN_ITEMS_COLLECTION)
                .document(mealPlanItemId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    // Meal Reminder methods
    public void addMealReminder(MealReminder reminder, final FirebaseCallback callback) {
        mFirestore.collection(MEAL_REMINDERS_COLLECTION)
                .add(reminder)
                .addOnSuccessListener(documentReference -> callback.onSuccess(documentReference.getId()))
                .addOnFailureListener(callback::onFailure);
    }

    public void updateMealReminder(MealReminder reminder, final FirebaseCallback callback) {
        mFirestore.collection(MEAL_REMINDERS_COLLECTION)
                .document(reminder.getId())
                .set(reminder)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void deleteMealReminder(String reminderId, final FirebaseCallback callback) {
        mFirestore.collection(MEAL_REMINDERS_COLLECTION)
                .document(reminderId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void getMealReminderById(String reminderId, final FirebaseCallback callback) {
        mFirestore.collection(MEAL_REMINDERS_COLLECTION)
                .document(reminderId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    MealReminder reminder = documentSnapshot.toObject(MealReminder.class);
                    if (reminder != null) {
                        reminder.setId(documentSnapshot.getId());
                        callback.onSuccess(reminder);
                    } else {
                        callback.onFailure(new Exception("Reminder not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getUserMealReminders(final FirebaseCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        mFirestore.collection(MEAL_REMINDERS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<MealReminder> reminders = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot) {
                        MealReminder reminder = document.toObject(MealReminder.class);
                        if (reminder != null) {
                            reminder.setId(document.getId());
                            reminders.add(reminder);
                        }
                    }
                    callback.onSuccess(reminders);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getMealRemindersForMeal(String mealId, final FirebaseCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        mFirestore.collection(MEAL_REMINDERS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("mealId", mealId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<MealReminder> reminders = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot) {
                        MealReminder reminder = document.toObject(MealReminder.class);
                        if (reminder != null) {
                            reminder.setId(document.getId());
                            reminders.add(reminder);
                        }
                    }
                    callback.onSuccess(reminders);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // Grocery List methods
    public void addGroceryList(GroceryList groceryList, final FirebaseCallback callback) {
        mFirestore.collection(GROCERY_LISTS_COLLECTION)
                .add(groceryList)
                .addOnSuccessListener(documentReference -> callback.onSuccess(documentReference.getId()))
                .addOnFailureListener(callback::onFailure);
    }

    public void updateGroceryList(GroceryList groceryList, final FirebaseCallback callback) {
        mFirestore.collection(GROCERY_LISTS_COLLECTION)
                .document(groceryList.getId())
                .set(groceryList)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void deleteGroceryList(String groceryListId, final FirebaseCallback callback) {
        mFirestore.collection(GROCERY_LISTS_COLLECTION)
                .document(groceryListId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void getGroceryListById(String groceryListId, final FirebaseCallback callback) {
        mFirestore.collection(GROCERY_LISTS_COLLECTION)
                .document(groceryListId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    GroceryList groceryList = documentSnapshot.toObject(GroceryList.class);
                    if (groceryList != null) {
                        groceryList.setId(documentSnapshot.getId());
                        callback.onSuccess(groceryList);
                    } else {
                        callback.onFailure(new Exception("Grocery list not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getUserGroceryLists(final FirebaseCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        mFirestore.collection(GROCERY_LISTS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<GroceryList> groceryLists = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot) {
                        GroceryList groceryList = document.toObject(GroceryList.class);
                        if (groceryList != null) {
                            groceryList.setId(document.getId());
                            groceryLists.add(groceryList);
                        }
                    }
                    callback.onSuccess(groceryLists);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // Item methods
    public void addItem(Item item, final FirebaseCallback callback) {
        mFirestore.collection(ITEMS_COLLECTION)
                .add(item)
                .addOnSuccessListener(documentReference -> callback.onSuccess(documentReference.getId()))
                .addOnFailureListener(callback::onFailure);
    }

    public void updateItem(Item item, final FirebaseCallback callback) {
        mFirestore.collection(ITEMS_COLLECTION)
                .document(item.getId())
                .set(item)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void deleteItem(String itemId, final FirebaseCallback callback) {
        mFirestore.collection(ITEMS_COLLECTION)
                .document(itemId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void getUserItems(final FirebaseCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        mFirestore.collection(ITEMS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Item> items = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot) {
                        Item item = document.toObject(Item.class);
                        if (item != null) {
                            item.setId(document.getId());
                            items.add(item);
                        }
                    }
                    callback.onSuccess(items);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // Store methods
    public void addGeotaggedStore(GeotaggedStore store, final FirebaseCallback callback) {
        mFirestore.collection(STORES_COLLECTION)
                .add(store)
                .addOnSuccessListener(documentReference -> callback.onSuccess(documentReference.getId()))
                .addOnFailureListener(callback::onFailure);
    }

    public void updateGeotaggedStore(GeotaggedStore store, final FirebaseCallback callback) {
        mFirestore.collection(STORES_COLLECTION)
                .document(store.getId())
                .set(store)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void deleteGeotaggedStore(String storeId, final FirebaseCallback callback) {
        mFirestore.collection(STORES_COLLECTION)
                .document(storeId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void getGeotaggedStoreById(String storeId, final FirebaseCallback callback) {
        mFirestore.collection(STORES_COLLECTION)
                .document(storeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    GeotaggedStore store = documentSnapshot.toObject(GeotaggedStore.class);
                    if (store != null) {
                        store.setId(documentSnapshot.getId());
                        callback.onSuccess(store);
                    } else {
                        callback.onFailure(new Exception("Store not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getUserGeotaggedStores(final FirebaseCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        mFirestore.collection(STORES_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<GeotaggedStore> stores = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot) {
                        GeotaggedStore store = document.toObject(GeotaggedStore.class);
                        if (store != null) {
                            store.setId(document.getId());
                            stores.add(store);
                        }
                    }
                    callback.onSuccess(stores);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void saveMealPlanItem(MealPlanItem mealPlanItem, final FirebaseCallback callback) {
        if (mealPlanItem.getId() == null || mealPlanItem.getId().isEmpty()) {
            // New item
            mFirestore.collection(MEAL_PLAN_ITEMS_COLLECTION)
                    .add(mealPlanItem)
                    .addOnSuccessListener(documentReference -> {
                        mealPlanItem.setId(documentReference.getId());
                        callback.onSuccess(mealPlanItem);
                    })
                    .addOnFailureListener(callback::onFailure);
        } else {
            // Update existing item
            mFirestore.collection(MEAL_PLAN_ITEMS_COLLECTION)
                    .document(mealPlanItem.getId())
                    .set(mealPlanItem)
                    .addOnSuccessListener(aVoid -> callback.onSuccess(mealPlanItem))
                    .addOnFailureListener(callback::onFailure);
        }
    }

    public void generateGroceryList(String name, List<String> mealIds, final FirebaseCallback callback) {
        if (mealIds == null || mealIds.isEmpty()) {
            callback.onFailure(new Exception("No meals selected"));
            return;
        }

        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        // Create a new grocery list
        GroceryList groceryList = new GroceryList();
        groceryList.setName(name);
        groceryList.setUserId(userId);
        groceryList.setCreatedAt(new Date());
        groceryList.setMealIds(mealIds);
        groceryList.setItems(new ArrayList<>());

        // Get all meals and combine their ingredients
        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String mealId : mealIds) {
            Task<DocumentSnapshot> task = mFirestore.collection(MEALS_COLLECTION)
                    .document(mealId)
                    .get();
            tasks.add(task);
        }

        Tasks.whenAllSuccess(tasks)
                .addOnSuccessListener(objects -> {
                    Set<String> uniqueIngredients = new HashSet<>();
                    for (Object object : objects) {
                        DocumentSnapshot document = (DocumentSnapshot) object;
                        Meal meal = document.toObject(Meal.class);
                        if (meal != null && meal.getIngredients() != null) {
                            uniqueIngredients.addAll(meal.getIngredients());
                        }
                    }

                    // Convert ingredients to grocery list items
                    for (String ingredient : uniqueIngredients) {
                        GroceryItem item = new GroceryItem();
                        item.setName(ingredient);
                        item.setCategory("General"); // Default category
                        item.setQuantity(1.0); // Changed from "1" to 1.0 to match double type
                        item.setUnit("unit"); // Default unit
                        item.setPurchased(false);
                        groceryList.getItems().add(item);
                    }

                    // Save the grocery list
                    mFirestore.collection(GROCERY_LISTS_COLLECTION)
                            .add(groceryList)
                            .addOnSuccessListener(documentReference -> {
                                groceryList.setId(documentReference.getId());
                                callback.onSuccess(groceryList);
                            })
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getItemById(String itemId, final FirebaseCallback callback) {
        if (itemId == null || itemId.isEmpty()) {
            callback.onFailure(new Exception("Item ID cannot be null or empty"));
            return;
        }

        mFirestore.collection(ITEMS_COLLECTION)
                .document(itemId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Item item = documentSnapshot.toObject(Item.class);
                    if (item != null) {
                        item.setId(documentSnapshot.getId());
                        callback.onSuccess(item);
                    } else {
                        callback.onFailure(new Exception("Item not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void delegateGroceryItems(String groceryListId, List<GroceryItem> items, String phoneNumber, final FirebaseCallback callback) {
        mFirestore.collection(GROCERY_LISTS_COLLECTION)
                .document(groceryListId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    GroceryList list = documentSnapshot.toObject(GroceryList.class);
                    if (list != null) {
                        boolean updated = false;
                        for (GroceryItem listItem : list.getItems()) {
                            for (GroceryItem delegatedItem : items) {
                                if (listItem.getName().equals(delegatedItem.getName())) {
                                    // Set emoji before delegation
                                    String emoji = EmojiUtils.getEmojiForIngredient(listItem.getName(), listItem.getCategory());
                                    listItem.setEmoji(emoji);
                                    listItem.setDelegatedTo(phoneNumber);
                                    listItem.setStatus("PENDING_PURCHASE");
                                    updated = true;
                                }
                            }
                        }
                        if (updated) {
                            documentSnapshot.getReference().set(list)
                                    .addOnSuccessListener(aVoid -> callback.onSuccess(EmojiUtils.formatSmsMessage(items)))
                                    .addOnFailureListener(callback::onFailure);
                        } else {
                            callback.onSuccess(null);
                        }
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void saveDelegationHistory(DelegationHistory history, final FirebaseCallback callback) {
        if (history.getId() == null || history.getId().isEmpty()) {
            mFirestore.collection(DELEGATION_HISTORY_COLLECTION)
                    .add(history)
                    .addOnSuccessListener(documentReference -> {
                        history.setId(documentReference.getId());
                        callback.onSuccess(history);
                    })
                    .addOnFailureListener(callback::onFailure);
        } else {
            mFirestore.collection(DELEGATION_HISTORY_COLLECTION)
                    .document(history.getId())
                    .set(history)
                    .addOnSuccessListener(aVoid -> callback.onSuccess(history))
                    .addOnFailureListener(callback::onFailure);
        }
    }

    public void getDelegationHistory(String groceryListId, final FirebaseCallback callback) {
        mFirestore.collection(DELEGATION_HISTORY_COLLECTION)
                .whereEqualTo("groceryListId", groceryListId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DelegationHistory> history = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot) {
                        DelegationHistory entry = document.toObject(DelegationHistory.class);
                        if (entry != null) {
                            entry.setId(document.getId());
                            history.add(entry);
                        }
                    }
                    callback.onSuccess(history);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void updateDelegationStatus(String delegationId, String status, final FirebaseCallback callback) {
        mFirestore.collection(DELEGATION_HISTORY_COLLECTION)
                .document(delegationId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void saveGroceryList(GroceryList groceryList, final FirebaseCallback callback) {
        if (groceryList == null) {
            System.out.println("Error: Cannot save null grocery list");
            if (callback != null) {
                callback.onFailure(new Exception("Cannot save null grocery list"));
            }
            return;
        }
        
        if (groceryList.getId() == null) {
            groceryList.setId(mFirestore.collection(GROCERY_LISTS_COLLECTION).document().getId());
            System.out.println("Generated new ID for grocery list: " + groceryList.getId());
        }
        
        System.out.println("Saving grocery list: " + groceryList.getId() + " with " + 
                (groceryList.getItems() != null ? groceryList.getItems().size() : 0) + " items");
        
        mFirestore.collection(GROCERY_LISTS_COLLECTION)
            .document(groceryList.getId())
            .set(groceryList)
            .addOnSuccessListener(aVoid -> {
                System.out.println("Successfully saved grocery list: " + groceryList.getId());
                if (callback != null) {
                    callback.onSuccess(groceryList);
                }
            })
            .addOnFailureListener(e -> {
                System.out.println("Failed to save grocery list: " + e.getMessage());
                e.printStackTrace();
                if (callback != null) {
                    callback.onFailure(e);
                }
            });
    }

    public void getGroceryLists(String userId, final FirebaseCallback callback) {
        if (userId == null || userId.isEmpty()) {
            System.out.println("Error: Cannot get grocery lists for null or empty user ID");
            if (callback != null) {
                callback.onFailure(new Exception("User ID cannot be null or empty"));
            }
            return;
        }
        
        System.out.println("Getting grocery lists for user: " + userId);
        
        mFirestore.collection(GROCERY_LISTS_COLLECTION)
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<GroceryList> groceryLists = new ArrayList<>();
                System.out.println("Received " + querySnapshot.size() + " grocery list documents");
                
                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                    try {
                        GroceryList groceryList = document.toObject(GroceryList.class);
                        if (groceryList != null) {
                            groceryList.setId(document.getId());
                            groceryLists.add(groceryList);
                            System.out.println("Added grocery list: " + groceryList.getName() + 
                                    " with " + groceryList.getItems().size() + " items");
                        } else {
                            System.out.println("Failed to convert document to GroceryList: " + document.getId());
                        }
                    } catch (Exception e) {
                        System.out.println("Error processing grocery list document: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                
                if (callback != null) {
                    callback.onSuccess(groceryLists);
                }
            })
            .addOnFailureListener(e -> {
                System.out.println("Failed to get grocery lists: " + e.getMessage());
                e.printStackTrace();
                if (callback != null) {
                    callback.onFailure(e);
                }
            });
    }

    public void shareGroceryList(GroceryList groceryList, String phoneNumber, final FirebaseCallback callback) {
        groceryList.setShared(true);
        groceryList.setSharedWithPhone(phoneNumber);
        saveGroceryList(groceryList, callback);
    }

    public void saveGroceryItem(String groceryListId, GroceryItem item, final FirebaseCallback callback) {
        if (item.getId() == null) {
            item.setId(mFirestore.collection(GROCERY_ITEMS_COLLECTION).document().getId());
        }
        
        mFirestore.collection(GROCERY_LISTS_COLLECTION)
            .document(groceryListId)
            .collection(GROCERY_ITEMS_COLLECTION)
            .document(item.getId())
            .set(item)
            .addOnSuccessListener(aVoid -> {
                if (callback != null) {
                    callback.onSuccess(item);
                }
            })
            .addOnFailureListener(e -> {
                if (callback != null) {
                    callback.onFailure(e);
                }
            });
    }

    public void deleteGroceryItem(String groceryListId, String itemId, final FirebaseCallback callback) {
        mFirestore.collection(GROCERY_LISTS_COLLECTION)
            .document(groceryListId)
            .collection(GROCERY_ITEMS_COLLECTION)
            .document(itemId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                if (callback != null) {
                    callback.onSuccess(null);
                }
            })
            .addOnFailureListener(e -> {
                if (callback != null) {
                    callback.onFailure(e);
                }
            });
    }

    public void getGroceryItems(String groceryListId, final FirebaseCallback callback) {
        mFirestore.collection(GROCERY_LISTS_COLLECTION)
            .document(groceryListId)
            .collection(GROCERY_ITEMS_COLLECTION)
            .orderBy("name")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<GroceryItem> items = new ArrayList<>();
                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                    GroceryItem item = document.toObject(GroceryItem.class);
                    if (item != null) {
                        item.setId(document.getId());
                        items.add(item);
                    }
                }
                if (callback != null) {
                    callback.onSuccess(items);
                }
            })
            .addOnFailureListener(e -> {
                if (callback != null) {
                    callback.onFailure(e);
                }
            });
    }

    public void updateGroceryItemStatus(String groceryListId, String itemId, 
            boolean isPurchased, final FirebaseCallback callback) {
        mFirestore.collection(GROCERY_LISTS_COLLECTION)
            .document(groceryListId)
            .collection(GROCERY_ITEMS_COLLECTION)
            .document(itemId)
            .update("purchased", isPurchased)
            .addOnSuccessListener(aVoid -> {
                if (callback != null) {
                    callback.onSuccess(null);
                }
            })
            .addOnFailureListener(e -> {
                if (callback != null) {
                    callback.onFailure(e);
                }
            });
    }

    public void getUserByEmail(String email, UserCallback callback) {
        firebaseAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().getSignInMethods() != null && 
                            !task.getResult().getSignInMethods().isEmpty()) {
                            // User exists, get their data from Firestore
                            mFirestore.collection(USERS_COLLECTION)
                                    .whereEqualTo("email", email)
                                    .get()
                                    .addOnSuccessListener(querySnapshot -> {
                                        if (!querySnapshot.isEmpty()) {
                                            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                                            FirebaseUser currentUser = getCurrentUser();
                                            if (currentUser != null) {
                                                callback.onUserFound(currentUser);
                                            } else {
                                                callback.onUserFound(null);
                                            }
                                        } else {
                                            callback.onUserFound(null);
                                        }
                                    })
                                    .addOnFailureListener(e -> callback.onUserFound(null));
                        } else {
                            callback.onUserFound(null);
                        }
                    } else {
                        callback.onUserFound(null);
                    }
                });
    }

    /**
     * Adds a meal to the recipe collection in Firestore
     * @param meal The meal to add
     * @param callback Callback for success or failure
     */
    public void addMealToRecipe(Meal meal, final FirebaseCallback callback) {
        // Generate a new ID if not present
        if (meal.getId() == null || meal.getId().isEmpty()) {
            meal.setId(UUID.randomUUID().toString());
        }
        
        // Save to the recipe collection
        mFirestore.collection(RECIPE_COLLECTION)
                .document(meal.getId())
                .set(meal)
                .addOnSuccessListener(aVoid -> {
                    // Now update ingredient relations
                    updateIngredientRelations(meal, callback);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Updates a meal in the recipe collection in Firestore
     * @param meal The meal to update
     * @param callback Callback for success or failure
     */
    public void updateMealInRecipe(Meal meal, final FirebaseCallback callback) {
        if (meal.getId() == null) {
            callback.onFailure(new Exception("Meal ID cannot be null"));
            return;
        }

        mFirestore.collection(RECIPE_COLLECTION)
                .document(meal.getId())
                .set(meal)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Gets all meals from the recipe collection for the current user
     * @param callback Callback for success or failure
     */
    public void getRecipes(final FirebaseCallback callback) {
        Log.d(TAG, "getRecipes: Fetching recipes for user: " + getCurrentUserId());
        
        mFirestore.collection(RECIPE_COLLECTION)
                .whereEqualTo("userId", getCurrentUserId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Meal> meals = new ArrayList<>();
                    Log.d(TAG, "getRecipes: Found " + queryDocumentSnapshots.size() + " documents");
                    
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Log.d(TAG, "getRecipes: Processing document with ID: " + doc.getId());
                        Meal meal = doc.toObject(Meal.class);
                        if (meal != null) {
                            meal.setId(doc.getId());
                            Log.d(TAG, "getRecipes: Added meal with ID: " + meal.getId() + ", Name: " + meal.getName());
                            meals.add(meal);
                        } else {
                            Log.e(TAG, "getRecipes: Failed to convert document to Meal: " + doc.getId());
                        }
                    }
                    
                    Log.d(TAG, "getRecipes: Returning " + meals.size() + " meals");
                    callback.onSuccess(meals);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getRecipes: Failed to fetch recipes", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Get recent recipes for the current user, limited to a specific count
     * @param limit The maximum number of recipes to return
     * @param callback The callback to handle the result
     */
    public void getRecentRecipes(int limit, final FirebaseCallback callback) {
        Log.d(TAG, "getRecentRecipes: Fetching recent recipes for user: " + getCurrentUserId() + ", limit: " + limit);
        
        // Use a simpler query that doesn't require a composite index
        mFirestore.collection(RECIPE_COLLECTION)
                .whereEqualTo("userId", getCurrentUserId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Meal> meals = new ArrayList<>();
                    Log.d(TAG, "getRecentRecipes: Found " + queryDocumentSnapshots.size() + " documents");
                    
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Log.d(TAG, "getRecentRecipes: Processing document with ID: " + doc.getId());
                        Meal meal = doc.toObject(Meal.class);
                        if (meal != null) {
                            meal.setId(doc.getId());
                            Log.d(TAG, "getRecentRecipes: Added meal with ID: " + meal.getId() + ", Name: " + meal.getName());
                            meals.add(meal);
                        } else {
                            Log.e(TAG, "getRecentRecipes: Failed to convert document to Meal: " + doc.getId());
                        }
                    }
                    
                    // Sort the meals by createdAt in memory (most recent first)
                    meals.sort((meal1, meal2) -> {
                        Date date1 = meal1.getCreatedAt();
                        Date date2 = meal2.getCreatedAt();
                        
                        // Handle null dates (put them at the end)
                        if (date1 == null && date2 == null) return 0;
                        if (date1 == null) return 1;
                        if (date2 == null) return -1;
                        
                        // Sort in descending order (most recent first)
                        return date2.compareTo(date1);
                    });
                    
                    // Limit the results
                    if (meals.size() > limit) {
                        meals = meals.subList(0, limit);
                    }
                    
                    Log.d(TAG, "getRecentRecipes: Returning " + meals.size() + " meals");
                    callback.onSuccess(meals);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getRecentRecipes: Failed to fetch recent recipes", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Deletes a meal from the recipe collection in Firestore
     * @param mealId The ID of the meal to delete
     * @param callback Callback for success or failure
     */
    public void deleteMealFromRecipe(String mealId, final FirebaseCallback callback) {
        if (mealId == null || mealId.isEmpty()) {
            callback.onFailure(new Exception("Meal ID cannot be null or empty"));
            return;
        }

        // First get the meal to know which ingredients to update
        mFirestore.collection(RECIPE_COLLECTION)
                .document(mealId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Meal meal = documentSnapshot.toObject(Meal.class);
                    if (meal != null) {
                        // Remove meal from ingredient relations
                        removeIngredientRelations(meal, () -> {
                            // Then delete the meal
                            mFirestore.collection(RECIPE_COLLECTION)
                                    .document(mealId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                                    .addOnFailureListener(callback::onFailure);
                        });
                    } else {
                        // Meal not found, just return success
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Checks if a meal exists in the recipe collection
     * @param mealId The ID of the meal to check
     * @param callback Callback for success or failure
     */
    public void checkMealExistsInRecipe(String mealId, final FirebaseCallback callback) {
        if (mealId == null || mealId.isEmpty()) {
            callback.onFailure(new Exception("Meal ID cannot be null or empty"));
            return;
        }

        mFirestore.collection(RECIPE_COLLECTION)
                .document(mealId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    boolean exists = documentSnapshot.exists();
                    callback.onSuccess(exists);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Gets a single meal from the recipe collection
     * @param mealId The ID of the meal to get
     * @param callback Callback for success or failure
     */
    public void getMealFromRecipe(String mealId, final FirebaseCallback callback) {
        if (mealId == null || mealId.isEmpty()) {
            Log.e(TAG, "getMealFromRecipe: Meal ID is null or empty");
            callback.onFailure(new Exception("Meal ID cannot be null or empty"));
            return;
        }

        Log.d(TAG, "getMealFromRecipe: Fetching meal with ID: " + mealId);
        mFirestore.collection(RECIPE_COLLECTION)
                .document(mealId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "getMealFromRecipe: Document exists with ID: " + documentSnapshot.getId());
                        Meal meal = documentSnapshot.toObject(Meal.class);
                        if (meal != null) {
                            meal.setId(documentSnapshot.getId());
                            Log.d(TAG, "getMealFromRecipe: Successfully converted to Meal object with ID: " + meal.getId() + ", Name: " + meal.getName());
                            callback.onSuccess(meal);
                        } else {
                            Log.e(TAG, "getMealFromRecipe: Failed to convert document to Meal object");
                            callback.onFailure(new Exception("Failed to convert document to Meal object"));
                        }
                    } else {
                        Log.e(TAG, "getMealFromRecipe: Document does not exist for ID: " + mealId);
                        callback.onFailure(new Exception("Meal not found"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getMealFromRecipe: Error fetching document", e);
                    callback.onFailure(e);
                });
    }
} 