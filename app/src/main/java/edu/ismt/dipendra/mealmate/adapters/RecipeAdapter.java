package edu.ismt.dipendra.mealmate.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.activities.MealDetailActivity;
import edu.ismt.dipendra.mealmate.models.Recipe;

/**
 * Adapter for displaying recipes in a RecyclerView
 */
public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private final Context context;
    private final List<Recipe> recipes;

    public RecipeAdapter(Context context, List<Recipe> recipes) {
        this.context = context;
        this.recipes = recipes;
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        
        holder.tvRecipeName.setText(recipe.getName());
        holder.tvRecipeCategory.setText(recipe.getCategory());
        holder.tvRecipeCookTime.setText(recipe.getCookTime());
        holder.ivRecipeImage.setImageResource(recipe.getImageResourceId());
        
        holder.cardRecipe.setOnClickListener(v -> {
            Intent intent = new Intent(context, MealDetailActivity.class);
            intent.putExtra("recipe_id", recipe.getId());
            intent.putExtra("recipe_name", recipe.getName());
            context.startActivity(intent);
            
            // Add entry animation
            if (context instanceof Activity) {
                ((Activity) context).overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        CardView cardRecipe;
        ImageView ivRecipeImage;
        TextView tvRecipeName, tvRecipeCategory, tvRecipeCookTime;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRecipe = itemView.findViewById(R.id.cardRecipe);
            ivRecipeImage = itemView.findViewById(R.id.ivRecipeImage);
            tvRecipeName = itemView.findViewById(R.id.tvRecipeName);
            tvRecipeCategory = itemView.findViewById(R.id.tvRecipeCategory);
            tvRecipeCookTime = itemView.findViewById(R.id.tvRecipeCookTime);
        }
    }
} 