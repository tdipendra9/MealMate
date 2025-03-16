package edu.ismt.dipendra.mealmate.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.model.Meal;

/**
 * Adapter for the ViewPager in MealDetailActivity to allow swiping between meal details
 */
public class MealDetailPagerAdapter extends RecyclerView.Adapter<MealDetailPagerAdapter.MealDetailViewHolder> {

    private static final String TAG = "MealDetailPagerAdapter";
    private List<Meal> meals;
    private Context context;
    private OnMealDetailActionListener listener;

    public interface OnMealDetailActionListener {
        void onEditClick(Meal meal);
        void onDeleteClick(Meal meal);
        void onSetReminderClick(Meal meal);
        void onShareClick(Meal meal);
    }

    public MealDetailPagerAdapter(Context context, OnMealDetailActionListener listener) {
        this.context = context;
        this.listener = listener;
        this.meals = new ArrayList<>();
    }

    @NonNull
    @Override
    public MealDetailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_meal_detail_page, parent, false);
        return new MealDetailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MealDetailViewHolder holder, int position) {
        Meal meal = meals.get(position);
        
        Log.d(TAG, "Binding meal detail: " + meal.getId() + " - " + meal.getName());
        
        // Set meal name
        holder.tvMealName.setText(meal.getName());
        
        // Set category
        if (meal.getCategory() != null && !meal.getCategory().isEmpty()) {
            holder.chipCategory.setText(meal.getCategory());
            holder.chipCategory.setVisibility(View.VISIBLE);
        } else {
            holder.chipCategory.setVisibility(View.GONE);
        }
        
        // Set calories if available
        if (meal.getCalories() > 0) {
            String calories = String.format(Locale.getDefault(), 
                    context.getString(R.string.calories_format), meal.getCalories());
            holder.tvCalories.setText(calories);
            holder.tvCalories.setVisibility(View.VISIBLE);
        } else {
            holder.tvCalories.setVisibility(View.GONE);
        }
        
        // Set preparation time, cooking time, and servings
        holder.tvPrepTime.setText(String.format(Locale.getDefault(), "%d min", meal.getPreparationTime()));
        holder.tvCookTime.setText(String.format(Locale.getDefault(), "%d min", meal.getCookingTime()));
        holder.tvServings.setText(String.format(Locale.getDefault(), "%d", meal.getServings()));
        
        // Set description
        if (meal.getDescription() != null && !meal.getDescription().isEmpty()) {
            holder.tvDescription.setText(meal.getDescription());
        } else {
            holder.tvDescription.setText("No description available");
        }
        
        // Set ingredients
        if (meal.getIngredients() != null && !meal.getIngredients().isEmpty()) {
            StringBuilder ingredientsBuilder = new StringBuilder();
            for (String ingredient : meal.getIngredients()) {
                ingredientsBuilder.append("• ").append(ingredient).append("\n");
            }
            holder.tvIngredients.setText(ingredientsBuilder.toString().trim());
        } else {
            holder.tvIngredients.setText("No ingredients listed");
        }
        
        // Set instructions
        if (meal.getInstructions() != null && !meal.getInstructions().isEmpty()) {
            // Format instructions with line breaks between steps
            String formattedInstructions = meal.getInstructions()
                    .replaceAll("(?m)^(\\d+\\.|\\*|-)\\s", "\n$0")
                    .trim();
            
            if (formattedInstructions.startsWith("\n")) {
                formattedInstructions = formattedInstructions.substring(1);
            }
            
            holder.tvInstructions.setText(formattedInstructions);
        } else {
            holder.tvInstructions.setText("No instructions provided");
        }
        
        // Set source information if this is an imported recipe
        if (meal.isImported() && meal.getSourceName() != null && !meal.getSourceName().isEmpty()) {
            holder.tvSourceLabel.setVisibility(View.VISIBLE);
            holder.tvSource.setVisibility(View.VISIBLE);
            
            String sourceText = context.getString(R.string.import_recipe_source, meal.getSourceName());
            if (meal.getSourceUrl() != null && !meal.getSourceUrl().isEmpty()) {
                sourceText += "\n" + meal.getSourceUrl();
            }
            
            holder.tvSource.setText(sourceText);
        } else {
            holder.tvSourceLabel.setVisibility(View.GONE);
            holder.tvSource.setVisibility(View.GONE);
        }
        
        // Load image if available
        if (meal.getImageUrl() != null && !meal.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(meal.getImageUrl())
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.placeholder_meal)
                            .error(R.drawable.placeholder_meal))
                    .into(holder.ivMealImage);
        } else {
            // Load placeholder
            holder.ivMealImage.setImageResource(R.drawable.placeholder_meal);
        }
        
        // Set click listeners
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(meal);
            }
        });
        
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(meal);
            }
        });
        
        holder.btnSetReminder.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSetReminderClick(meal);
            }
        });
        
        holder.fabShare.setOnClickListener(v -> {
            if (listener != null) {
                listener.onShareClick(meal);
            }
        });
    }

    @Override
    public int getItemCount() {
        return meals.size();
    }
    
    public void setMeals(List<Meal> meals) {
        if (meals == null) {
            this.meals = new ArrayList<>();
        } else {
            this.meals = new ArrayList<>(meals);
        }
        notifyDataSetChanged();
    }
    
    public void addMeal(Meal meal) {
        if (meal != null) {
            this.meals.add(meal);
            notifyItemInserted(this.meals.size() - 1);
        }
    }
    
    public Meal getMealAt(int position) {
        if (position >= 0 && position < meals.size()) {
            return meals.get(position);
        }
        return null;
    }
    
    public int getPositionForMealId(String mealId) {
        if (mealId == null) return -1;
        
        for (int i = 0; i < meals.size(); i++) {
            if (mealId.equals(meals.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }

    static class MealDetailViewHolder extends RecyclerView.ViewHolder {
        ImageView ivMealImage;
        TextView tvMealName;
        Chip chipCategory;
        TextView tvCalories;
        TextView tvPrepTime;
        TextView tvCookTime;
        TextView tvServings;
        TextView tvDescription;
        TextView tvIngredients;
        TextView tvInstructions;
        TextView tvSourceLabel;
        TextView tvSource;
        MaterialButton btnEdit;
        MaterialButton btnDelete;
        MaterialButton btnSetReminder;
        View fabShare;

        public MealDetailViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMealImage = itemView.findViewById(R.id.ivMealImage);
            tvMealName = itemView.findViewById(R.id.tvMealName);
            chipCategory = itemView.findViewById(R.id.chipCategory);
            tvCalories = itemView.findViewById(R.id.tvCalories);
            tvPrepTime = itemView.findViewById(R.id.tvPrepTime);
            tvCookTime = itemView.findViewById(R.id.tvCookTime);
            tvServings = itemView.findViewById(R.id.tvServings);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvIngredients = itemView.findViewById(R.id.tvIngredients);
            tvInstructions = itemView.findViewById(R.id.tvInstructions);
            tvSourceLabel = itemView.findViewById(R.id.tvSourceLabel);
            tvSource = itemView.findViewById(R.id.tvSource);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnSetReminder = itemView.findViewById(R.id.btnSetReminder);
            fabShare = itemView.findViewById(R.id.fabShare);
        }
    }
} 