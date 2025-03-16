package edu.ismt.dipendra.mealmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.model.Meal;

public class GroceryMealSelectionAdapter extends RecyclerView.Adapter<GroceryMealSelectionAdapter.MealSelectionViewHolder> {

    private final Context context;
    private List<Meal> meals;
    private final Set<String> selectedMealIds;

    public GroceryMealSelectionAdapter(Context context) {
        this.context = context;
        this.meals = new ArrayList<>();
        this.selectedMealIds = new HashSet<>();
    }

    @NonNull
    @Override
    public MealSelectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.meal_selection_item, parent, false);
        return new MealSelectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MealSelectionViewHolder holder, int position) {
        Meal meal = meals.get(position);
        
        holder.tvMealName.setText(meal.getName());
        
        // Set ingredient count
        int ingredientCount = meal.getIngredients() != null ? meal.getIngredients().size() : 0;
        holder.tvIngredientCount.setText(ingredientCount + " ingredients");
        
        // Set category
        holder.tvCategory.setText(meal.getCategory());
        
        // Set image if available
        if (meal.getImageUrl() != null && !meal.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(meal.getImageUrl())
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .centerCrop()
                    .into(holder.ivMealImage);
        } else {
            holder.ivMealImage.setImageResource(R.drawable.ic_launcher_foreground);
        }
        
        // Set checkbox state
        holder.cbSelectMeal.setChecked(selectedMealIds.contains(meal.getId()));
        
        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            toggleMealSelection(meal.getId());
            holder.cbSelectMeal.setChecked(selectedMealIds.contains(meal.getId()));
        });
        
        holder.cbSelectMeal.setOnClickListener(v -> {
            toggleMealSelection(meal.getId());
        });
    }

    @Override
    public int getItemCount() {
        return meals.size();
    }

    public void updateMeals(List<Meal> newMeals) {
        this.meals = newMeals;
        notifyDataSetChanged();
    }

    public List<String> getSelectedMealIds() {
        return new ArrayList<>(selectedMealIds);
    }

    private void toggleMealSelection(String mealId) {
        if (selectedMealIds.contains(mealId)) {
            selectedMealIds.remove(mealId);
        } else {
            selectedMealIds.add(mealId);
        }
    }

    public void clearSelections() {
        selectedMealIds.clear();
        notifyDataSetChanged();
    }

    static class MealSelectionViewHolder extends RecyclerView.ViewHolder {
        ImageView ivMealImage;
        TextView tvMealName;
        TextView tvIngredientCount;
        TextView tvCategory;
        CheckBox cbSelectMeal;

        public MealSelectionViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMealImage = itemView.findViewById(R.id.ivMealImage);
            tvMealName = itemView.findViewById(R.id.tvMealName);
            tvIngredientCount = itemView.findViewById(R.id.tvIngredientCount);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            cbSelectMeal = itemView.findViewById(R.id.cbSelectMeal);
        }
    }
} 