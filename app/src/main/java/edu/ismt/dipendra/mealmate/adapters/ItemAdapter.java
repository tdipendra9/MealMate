package edu.ismt.dipendra.mealmate.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.model.Item;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

    private List<Item> itemList;
    private Context context;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onEditClick(Item item);
        void onDeleteClick(Item item);
        void onPurchasedStatusChanged(Item item, boolean isChecked);
        void onItemClick(Item item);
    }

    public ItemAdapter(Context context, List<Item> itemList, OnItemClickListener listener) {
        this.context = context;
        this.itemList = itemList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_card, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = itemList.get(position);
        
        // Debug log to check item data
        Log.d("ItemAdapter", "Binding item: " + item.getId() + " - " + item.getName() + 
                " - Category: " + item.getCategory() + " - Quantity: " + item.getQuantity());
        
        holder.textViewItemName.setText(item.getName());
        holder.textViewItemDescription.setText(item.getDescription());
        
        // Format price with quantity
        String priceText = String.format(Locale.getDefault(), 
                context.getString(R.string.price_format), item.getPrice());
        holder.textViewItemPrice.setText(priceText);
        
        // Set quantity
        String quantityText = String.format(Locale.getDefault(), 
                context.getString(R.string.quantity_format), item.getQuantity());
        holder.textViewItemQuantity.setText(quantityText);
        
        // Set category
        if (item.getCategory() != null && !item.getCategory().isEmpty()) {
            holder.textViewItemCategory.setText(item.getCategory());
            holder.textViewItemCategory.setVisibility(View.VISIBLE);
        } else {
            holder.textViewItemCategory.setVisibility(View.GONE);
        }
        
        // Set purchased status
        holder.checkBoxPurchased.setChecked(item.isPurchased());
        
        // Apply strikethrough if item is purchased
        if (item.isPurchased()) {
            holder.textViewItemName.setPaintFlags(holder.textViewItemName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.textViewItemDescription.setPaintFlags(holder.textViewItemDescription.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.textViewItemQuantity.setPaintFlags(holder.textViewItemQuantity.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.textViewItemCategory.setPaintFlags(holder.textViewItemCategory.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.textViewItemName.setPaintFlags(holder.textViewItemName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.textViewItemDescription.setPaintFlags(holder.textViewItemDescription.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.textViewItemQuantity.setPaintFlags(holder.textViewItemQuantity.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.textViewItemCategory.setPaintFlags(holder.textViewItemCategory.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
        
        // Set click listeners
        holder.buttonEditItem.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(item);
            }
        });
        
        holder.buttonDeleteItem.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(item);
            }
        });
        
        holder.checkBoxPurchased.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null && buttonView.isPressed()) { // Only trigger if user changed it
                listener.onPurchasedStatusChanged(item, isChecked);
            }
        });
        
        // Add click listener for the entire card
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public void updateItems(List<Item> newItems) {
        if (newItems == null) {
            return;
        }
        this.itemList = newItems;
        notifyDataSetChanged();
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView textViewItemName;
        TextView textViewItemDescription;
        TextView textViewItemPrice;
        TextView textViewItemQuantity;
        TextView textViewItemCategory;
        CheckBox checkBoxPurchased;
        ImageButton buttonEditItem;
        ImageButton buttonDeleteItem;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewItemName = itemView.findViewById(R.id.textViewItemName);
            textViewItemDescription = itemView.findViewById(R.id.textViewItemDescription);
            textViewItemPrice = itemView.findViewById(R.id.textViewItemPrice);
            textViewItemQuantity = itemView.findViewById(R.id.textViewItemQuantity);
            textViewItemCategory = itemView.findViewById(R.id.textViewItemCategory);
            checkBoxPurchased = itemView.findViewById(R.id.checkBoxPurchased);
            buttonEditItem = itemView.findViewById(R.id.buttonEditItem);
            buttonDeleteItem = itemView.findViewById(R.id.buttonDeleteItem);
        }
    }
} 