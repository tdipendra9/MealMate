package edu.ismt.dipendra.mealmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Locale;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.model.MealPlanItem;

public class MealPlanAdapter extends RecyclerView.Adapter<MealPlanAdapter.MealPlanViewHolder> {

    private Context context;
    private List<MealPlanItem> mealPlanList;
    private OnMealPlanClickListener listener;

    public interface OnMealPlanClickListener {
        void onMealPlanClick(MealPlanItem mealPlan);
        void onMealPlanLongClick(MealPlanItem mealPlan);

        void onDeleteClick(MealPlanItem mealPlan);
    }

    public MealPlanAdapter(Context context, List<MealPlanItem> mealPlanList, OnMealPlanClickListener listener) {
        this.context = context;
        this.mealPlanList = mealPlanList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MealPlanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_meal_plan, parent, false);
        return new MealPlanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MealPlanViewHolder holder, int position) {
        MealPlanItem mealPlan = mealPlanList.get(position);
        
        // Set meal name
        holder.tvMealName.setText(mealPlan.getMealName());
        
        // Set calories if available
        if (mealPlan.getCalories() > 0) {
            holder.tvCalories.setText(context.getString(R.string.calories_format, mealPlan.getCalories()));
            holder.tvCalories.setVisibility(View.VISIBLE);
        } else {
            holder.tvCalories.setVisibility(View.GONE);
        }
        
        // Set servings if available
        if (mealPlan.getServings() > 0) {
            holder.tvServings.setText(context.getString(R.string.servings_format, mealPlan.getServings()));
            holder.tvServings.setVisibility(View.VISIBLE);
        } else {
            holder.tvServings.setVisibility(View.GONE);
        }
        
        // Load image if available
        if (mealPlan.getImageUrl() != null && !mealPlan.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(mealPlan.getImageUrl())
                    .placeholder(R.drawable.placeholder_meal)
                    .error(R.drawable.placeholder_meal)
                    .into(holder.ivMealImage);
            holder.ivMealImage.setVisibility(View.VISIBLE);
        } else {
            holder.ivMealImage.setVisibility(View.GONE);
        }
        
        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMealPlanClick(mealPlan);
            }
        });
        
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onMealPlanLongClick(mealPlan);
                return true;
            }
            return false;
        });
        
        // Set delete button click listener
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(mealPlan);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mealPlanList != null ? mealPlanList.size() : 0;
    }

    static class MealPlanViewHolder extends RecyclerView.ViewHolder {
        ImageView ivMealImage;
        TextView tvMealName;
        TextView tvCalories;
        TextView tvServings;
        ImageButton btnDelete;

        MealPlanViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMealImage = itemView.findViewById(R.id.ivMealImage);
            tvMealName = itemView.findViewById(R.id.tvMealName);
            tvCalories = itemView.findViewById(R.id.tvCalories);
            tvServings = itemView.findViewById(R.id.tvServings);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
} 