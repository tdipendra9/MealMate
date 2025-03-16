package edu.ismt.dipendra.mealmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.model.Meal;

public class MealSelectionAdapter extends RecyclerView.Adapter<MealSelectionAdapter.MealViewHolder> {

    private final Context context;
    private final List<Meal> mealList;
    private int selectedPosition = -1;
    private OnMealSelectedListener listener;

    public interface OnMealSelectedListener {
        void onMealSelected(Meal meal);
    }

    public MealSelectionAdapter(Context context, List<Meal> mealList, OnMealSelectedListener listener) {
        this.context = context;
        this.mealList = mealList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_meal_selection, parent, false);
        return new MealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        Meal meal = mealList.get(position);
        
        holder.tvMealName.setText(meal.getName());
        
        // Set calories if available
        if (meal.getCalories() > 0) {
            String calories = String.format(Locale.getDefault(), 
                    context.getString(R.string.calories_format), meal.getCalories());
            holder.tvCalories.setText(calories);
            holder.tvCalories.setVisibility(View.VISIBLE);
        } else {
            holder.tvCalories.setVisibility(View.GONE);
        }
        
        // Set preparation and cooking time
        String prepTime = String.format(Locale.getDefault(), 
                context.getString(R.string.prep_time), meal.getPreparationTime());
        holder.tvPrepTime.setText(prepTime);
        
        // Set radio button state
        holder.radioButton.setChecked(position == selectedPosition);
        
        // Set click listener for the entire item
        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            
            // Notify previous selected item to update its state
            if (previousSelected != -1) {
                notifyItemChanged(previousSelected);
            }
            
            // Notify current selected item to update its state
            notifyItemChanged(selectedPosition);
            
            // Notify listener
            if (listener != null) {
                listener.onMealSelected(meal);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mealList.size();
    }

    public Meal getSelectedMeal() {
        if (selectedPosition != -1 && selectedPosition < mealList.size()) {
            return mealList.get(selectedPosition);
        }
        return null;
    }

    public void clearSelection() {
        selectedPosition = -1;
        notifyDataSetChanged();
    }

    static class MealViewHolder extends RecyclerView.ViewHolder {
        RadioButton radioButton;
        TextView tvMealName;
        TextView tvCalories;
        TextView tvPrepTime;

        MealViewHolder(@NonNull View itemView) {
            super(itemView);
            radioButton = itemView.findViewById(R.id.radioButton);
            tvMealName = itemView.findViewById(R.id.tvMealName);
            tvCalories = itemView.findViewById(R.id.tvCalories);
            tvPrepTime = itemView.findViewById(R.id.tvPrepTime);
            
            // Set click listener for radio button
            radioButton.setOnClickListener(v -> itemView.performClick());
        }
    }
} 