package edu.ismt.dipendra.mealmate.adapters;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import edu.ismt.dipendra.mealmate.R;

public class IngredientAdapter extends RecyclerView.Adapter<IngredientAdapter.IngredientViewHolder> {
    
    private final Context context;
    private final List<String> ingredients;
    private OnIngredientClickListener listener;
    
    public interface OnIngredientClickListener {
        void onDeleteClick(int position);
    }
    
    public IngredientAdapter(Context context, List<String> ingredients) {
        this.context = context;
        this.ingredients = ingredients;
    }
    
    public IngredientAdapter(Context context, List<String> ingredients, OnIngredientClickListener listener) {
        this.context = context;
        this.ingredients = ingredients;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public IngredientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_ingredient, parent, false);
        return new IngredientViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull IngredientViewHolder holder, int position) {
        String ingredient = ingredients.get(position);
        holder.etIngredient.setText(ingredient);
        
        // Add text change listener to update the list
        holder.etIngredient.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not used
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    ingredients.set(adapterPosition, s.toString());
                }
            }
        });
        
        // Set remove button click listener
        holder.btnRemoveIngredient.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                if (listener != null) {
                    listener.onDeleteClick(adapterPosition);
                } else {
                    ingredients.remove(adapterPosition);
                    notifyItemRemoved(adapterPosition);
                    notifyItemRangeChanged(adapterPosition, ingredients.size() - adapterPosition);
                }
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return ingredients.size();
    }
    
    static class IngredientViewHolder extends RecyclerView.ViewHolder {
        TextInputEditText etIngredient;
        ImageButton btnRemoveIngredient;
        
        public IngredientViewHolder(@NonNull View itemView) {
            super(itemView);
            etIngredient = itemView.findViewById(R.id.etIngredient);
            btnRemoveIngredient = itemView.findViewById(R.id.btnRemoveIngredient);
        }
    }
} 