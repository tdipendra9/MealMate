package edu.ismt.dipendra.mealmate.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.model.Meal;

public class MealAdapter extends RecyclerView.Adapter<MealAdapter.MealViewHolder> {

    private static final String TAG = "MealAdapter";
    private List<Meal> mealList;
    private Context context;
    private OnMealClickListener listener;

    public interface OnMealClickListener {
        void onEditClick(Meal meal);
        void onDeleteClick(Meal meal);
        void onSetReminderClick(Meal meal);
        void onMealClick(Meal meal);
    }

    public MealAdapter(Context context, List<Meal> mealList, OnMealClickListener listener) {
        this.context = context;
        this.mealList = mealList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.meal_card, parent, false);
        return new MealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        Meal meal = mealList.get(position);
        
        // Debug log to check meal data
        Log.d(TAG, "Binding meal: " + meal.getId() + " - " + meal.getName() + 
                " - Category: " + meal.getCategory());
        
        // Additional debug for click handling
        final int pos = position;
        
        holder.textViewMealName.setText(meal.getName());
        holder.textViewMealDescription.setText(meal.getDescription());
        
        // Set category
        if (meal.getCategory() != null && !meal.getCategory().isEmpty()) {
            holder.textViewMealCategory.setText(meal.getCategory());
            holder.textViewMealCategory.setVisibility(View.VISIBLE);
        } else {
            holder.textViewMealCategory.setVisibility(View.GONE);
        }
        
        // Set preparation and cooking time
        String prepTime = String.format(Locale.getDefault(), 
                context.getString(R.string.prep_time), meal.getPreparationTime());
        holder.textViewPrepTime.setText(prepTime);
        
        String cookTime = String.format(Locale.getDefault(), 
                context.getString(R.string.cook_time), meal.getCookingTime());
        holder.textViewCookTime.setText(cookTime);
        
        // Set servings
        String servings = String.format(Locale.getDefault(), 
                context.getString(R.string.servings_count), meal.getServings());
        holder.textViewServings.setText(servings);
        
        // Load image if available
        if (meal.getImageUrl() != null && !meal.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(meal.getImageUrl())
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.placeholder_meal)
                            .error(R.drawable.placeholder_meal))
                    .into(holder.imageViewMeal);
            holder.imageViewMeal.setVisibility(View.VISIBLE);
        } else {
            holder.imageViewMeal.setVisibility(View.GONE);
        }
        
        // Set click listeners
        holder.buttonEditMeal.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(meal);
            }
        });
        
        holder.buttonDeleteMeal.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(meal);
            }
        });
        
        holder.buttonSetReminder.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSetReminderClick(meal);
            }
        });
        
        // Set click listener for the entire card
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                Log.d(TAG, "Item clicked: " + meal.getId() + " - " + meal.getName());
                listener.onMealClick(meal);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mealList.size();
    }

    public void updateMeals(List<Meal> newMeals) {
        if (newMeals == null) {
            Log.e(TAG, "Null meal list provided to adapter");
            return;
        }
        
        Log.d(TAG, "Updating adapter with " + newMeals.size() + " meals");
        
        // Create a new list to avoid reference issues
        this.mealList = new ArrayList<>();
        
        // Add each meal to the list, checking for nulls
        for (Meal meal : newMeals) {
            if (meal != null) {
                this.mealList.add(meal);
            } else {
                Log.w(TAG, "Skipping null meal in updateMeals");
            }
        }
        
        // Log all meals for debugging
        for (int i = 0; i < mealList.size(); i++) {
            Meal meal = mealList.get(i);
            Log.d(TAG, "Meal " + i + ": " + meal.getId() + " - " + meal.getName() + 
                    " - Category: " + meal.getCategory() + 
                    " - Ingredients: " + (meal.getIngredients() != null ? meal.getIngredients().size() : "null"));
        }
        
        notifyDataSetChanged();
        Log.d(TAG, "Adapter updated with " + mealList.size() + " meals");
    }

    static class MealViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMealName;
        TextView textViewMealDescription;
        TextView textViewMealCategory;
        TextView textViewPrepTime;
        TextView textViewCookTime;
        TextView textViewServings;
        ImageView imageViewMeal;
        ImageButton buttonEditMeal;
        ImageButton buttonDeleteMeal;
        ImageButton buttonSetReminder;

        public MealViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMealName = itemView.findViewById(R.id.textViewMealName);
            textViewMealDescription = itemView.findViewById(R.id.textViewMealDescription);
            textViewMealCategory = itemView.findViewById(R.id.textViewMealCategory);
            textViewPrepTime = itemView.findViewById(R.id.textViewPrepTime);
            textViewCookTime = itemView.findViewById(R.id.textViewCookTime);
            textViewServings = itemView.findViewById(R.id.textViewServings);
            imageViewMeal = itemView.findViewById(R.id.imageViewMeal);
            buttonEditMeal = itemView.findViewById(R.id.buttonEditMeal);
            buttonDeleteMeal = itemView.findViewById(R.id.buttonDeleteMeal);
            buttonSetReminder = itemView.findViewById(R.id.buttonSetReminder);
        }
    }
} 