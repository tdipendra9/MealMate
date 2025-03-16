package edu.ismt.dipendra.mealmate.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.model.GroceryItem;

public class GroceryCategoryAdapter extends RecyclerView.Adapter<GroceryCategoryAdapter.CategoryViewHolder> {

    public interface OnGroceryItemChangeListener {
        void onItemPurchasedChanged(GroceryItem item, boolean purchased);
        void onItemDeleted(GroceryItem item);
        void onItemEdited(GroceryItem item);
    }

    // Interface for haptic feedback
    public interface OnGesturePerformedListener {
        void onGesturePerformed(int duration);
    }

    private final Context context;
    private final List<String> categories;
    private final Map<String, List<GroceryItem>> categoryItems;
    private OnGroceryItemChangeListener listener;
    private OnGesturePerformedListener gestureListener;
    private OnItemClickListener itemClickListener;
    private String currentFilter = "all";

    public GroceryCategoryAdapter(Context context, List<String> categories, Map<String, List<GroceryItem>> categoryItems) {
        this.context = context;
        this.categories = categories;
        this.categoryItems = categoryItems;
    }

    public GroceryCategoryAdapter(Context context, List<String> categories, Map<String, List<GroceryItem>> categoryItems, String filter) {
        this.context = context;
        this.categories = categories;
        this.categoryItems = categoryItems;
        this.currentFilter = filter;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.grocery_category_item, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        String category = categories.get(position);
        holder.tvCategoryName.setText(category);

        List<GroceryItem> items = categoryItems.get(category);
        if (items != null) {
            List<GroceryItem> filteredItems = new ArrayList<>();
            for (GroceryItem item : items) {
                if (shouldShowItem(item)) {
                    filteredItems.add(item);
                }
            }

            GroceryItemAdapter itemAdapter = new GroceryItemAdapter(context, filteredItems);
            itemAdapter.setOnGroceryItemChangeListener(listener);
            itemAdapter.setOnItemClickListener(itemClickListener);
            holder.rvCategoryItems.setAdapter(itemAdapter);
            
            // Setup swipe gestures for the items
            setupSwipeGestures(holder.rvCategoryItems, itemAdapter);
        }
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public void setOnGroceryItemChangeListener(OnGroceryItemChangeListener listener) {
        this.listener = listener;
    }

    public void setOnGesturePerformedListener(OnGesturePerformedListener listener) {
        this.gestureListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setFilter(String filter) {
        this.currentFilter = filter;
        notifyDataSetChanged();
    }

    private boolean shouldShowItem(GroceryItem item) {
        switch (currentFilter.toLowerCase()) {
            case "purchased":
                return item.isPurchased();
            case "non-purchased":
                return !item.isPurchased();
            default:
                return true;
        }
    }

    private void setupSwipeGestures(RecyclerView recyclerView, GroceryItemAdapter adapter) {
        // Create an ItemTouchHelper for swipe gestures
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, 
                                 @NonNull RecyclerView.ViewHolder viewHolder, 
                                 @NonNull RecyclerView.ViewHolder target) {
                return false; // We don't support drag and drop
            }
            
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                GroceryItem item = adapter.getItem(position);
                
                if (direction == ItemTouchHelper.LEFT) {
                    // Swipe left to delete
                    if (listener != null) {
                        listener.onItemDeleted(item);
                        
                        // Provide haptic feedback
                        if (gestureListener != null) {
                            gestureListener.onGesturePerformed(100);
                        }
                    }
                } else if (direction == ItemTouchHelper.RIGHT) {
                    // Swipe right to toggle purchased status
                    boolean newStatus = !item.isPurchased();
                    item.setPurchased(newStatus);
                    item.setStatus(newStatus ? "PURCHASED" : "AVAILABLE");
                    item.setCompleted(newStatus);
                    
                    if (listener != null) {
                        listener.onItemPurchasedChanged(item, newStatus);
                        
                        // Provide haptic feedback
                        if (gestureListener != null) {
                            gestureListener.onGesturePerformed(50);
                        }
                    }
                }
            }
            
            @Override
            public void onChildDraw(@NonNull android.graphics.Canvas c, 
                                   @NonNull RecyclerView recyclerView, 
                                   @NonNull RecyclerView.ViewHolder viewHolder, 
                                   float dX, float dY, int actionState, boolean isCurrentlyActive) {
                
                // Add visual feedback for swipe actions
                View itemView = viewHolder.itemView;
                android.graphics.drawable.Drawable background;
                android.graphics.drawable.Drawable icon;
                int iconMargin = (itemView.getHeight() - 24) / 2;
                
                if (dX > 0) {
                    // Swiping right - mark as purchased/unpurchased
                    GroceryItem item = adapter.getItem(viewHolder.getAdapterPosition());
                    if (!item.isPurchased()) {
                        // Will mark as purchased
                        background = new android.graphics.drawable.ColorDrawable(
                                ContextCompat.getColor(context, R.color.success));
                        icon = ContextCompat.getDrawable(context, android.R.drawable.checkbox_on_background);
                    } else {
                        // Will mark as unpurchased
                        background = new android.graphics.drawable.ColorDrawable(
                                ContextCompat.getColor(context, R.color.warning));
                        icon = ContextCompat.getDrawable(context, android.R.drawable.checkbox_off_background);
                    }
                    
                    // Draw background
                    background.setBounds(itemView.getLeft(), itemView.getTop(), 
                            itemView.getLeft() + (int) dX, itemView.getBottom());
                    background.draw(c);
                    
                    // Draw icon
                    if (icon != null) {
                        icon.setBounds(itemView.getLeft() + iconMargin, 
                                itemView.getTop() + iconMargin,
                                itemView.getLeft() + iconMargin + 24,
                                itemView.getBottom() - iconMargin);
                        icon.draw(c);
                    }
                } else if (dX < 0) {
                    // Swiping left - delete
                    background = new android.graphics.drawable.ColorDrawable(
                            ContextCompat.getColor(context, R.color.error));
                    icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_delete);
                    
                    // Draw background
                    background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(),
                            itemView.getRight(), itemView.getBottom());
                    background.draw(c);
                    
                    // Draw icon
                    if (icon != null) {
                        icon.setBounds(itemView.getRight() - iconMargin - 24,
                                itemView.getTop() + iconMargin,
                                itemView.getRight() - iconMargin,
                                itemView.getBottom() - iconMargin);
                        icon.draw(c);
                    }
                }
                
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };
        
        // Attach the ItemTouchHelper to the RecyclerView
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName;
        RecyclerView rvCategoryItems;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            rvCategoryItems = itemView.findViewById(R.id.rvCategoryItems);
            rvCategoryItems.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            rvCategoryItems.setNestedScrollingEnabled(false);
        }
    }

    private class GroceryItemAdapter extends RecyclerView.Adapter<GroceryItemAdapter.ItemViewHolder> {

        private final Context context;
        private final List<GroceryItem> items;
        private OnGroceryItemChangeListener listener;
        private OnItemClickListener itemClickListener;

        public GroceryItemAdapter(Context context, List<GroceryItem> items) {
            this.context = context;
            this.items = items;
        }

        public void setOnGroceryItemChangeListener(OnGroceryItemChangeListener listener) {
            this.listener = listener;
        }
        
        public void setOnItemClickListener(OnItemClickListener listener) {
            this.itemClickListener = listener;
        }

        @NonNull
        @Override
        public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.grocery_item, parent, false);
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
            GroceryItem item = items.get(position);

            holder.tvItemName.setText(item.getName());
            holder.tvItemQuantity.setText(String.format("%s %s", item.getQuantity(), item.getUnit()));
            
            // Remove the listener before setting checked state to avoid triggering it during binding
            holder.cbItemPurchased.setOnCheckedChangeListener(null);
            
            // Ensure checkbox state is consistent with purchased status
            holder.cbItemPurchased.setChecked(item.isPurchased());
            
            // Apply visual styling based on purchased status
            if (item.isPurchased()) {
                // Visual style for purchased items
                holder.tvItemName.setPaintFlags(holder.tvItemName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                holder.tvItemName.setAlpha(0.6f);
                holder.tvItemQuantity.setAlpha(0.6f);
                holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.lightGray));
            } else {
                // Visual style for non-purchased items
                holder.tvItemName.setPaintFlags(holder.tvItemName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                holder.tvItemName.setAlpha(1.0f);
                holder.tvItemQuantity.setAlpha(1.0f);
                holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
            }
            
            // Set price if available
            if (item.getPrice() > 0) {
                holder.tvItemPrice.setVisibility(View.VISIBLE);
                holder.tvItemPrice.setText(String.format("$%.2f", item.getPrice()));
                if (item.isPurchased()) {
                    holder.tvItemPrice.setAlpha(0.6f);
                } else {
                    holder.tvItemPrice.setAlpha(1.0f);
                }
            } else {
                holder.tvItemPrice.setVisibility(View.GONE);
            }

            // Show store info if available and item is not purchased
            if (!item.isPurchased() && item.hasStore()) {
                holder.tvItemStore.setVisibility(View.VISIBLE);
                holder.tvItemStore.setText(String.format("Buy from %s", item.getStoreName()));
            } else {
                holder.tvItemStore.setVisibility(View.GONE);
            }
            
            // Show delegated info if available and item is not purchased
            if (!item.isPurchased() && item.getDelegatedTo() != null && !item.getDelegatedTo().isEmpty()) {
                holder.tvItemDelegatedTo.setVisibility(View.VISIBLE);
                holder.tvItemDelegatedTo.setText(String.format("Delegated to: %s", item.getDelegatedTo()));
            } else {
                holder.tvItemDelegatedTo.setVisibility(View.GONE);
            }

            // Set click listeners AFTER setting the checked state
            holder.cbItemPurchased.setOnClickListener(v -> {
                boolean isChecked = ((CheckBox) v).isChecked();
                
                // Update the item's purchased status
                item.setPurchased(isChecked);
                item.setStatus(isChecked ? "PURCHASED" : "AVAILABLE");
                item.setCompleted(isChecked);
                
                // Notify the listener
                if (listener != null) {
                    listener.onItemPurchasedChanged(item, isChecked);
                }
            });
            
            // Make the checkbox clickable
            holder.cbItemPurchased.setClickable(true);
            holder.cbItemPurchased.setFocusable(true);
            
            // Make the entire item clickable
            holder.itemView.setOnClickListener(v -> {
                if (itemClickListener != null) {
                    itemClickListener.onItemClicked(item);
                }
            });

            holder.btnEditItem.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemEdited(item);
                }
            });

            holder.btnDeleteItem.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemDeleted(item);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public GroceryItem getItem(int position) {
            if (position >= 0 && position < items.size()) {
                return items.get(position);
            }
            return null;
        }

        class ItemViewHolder extends RecyclerView.ViewHolder {
            CheckBox cbItemPurchased;
            TextView tvItemName;
            TextView tvItemQuantity;
            TextView tvItemPrice;
            TextView tvItemStore;
            TextView tvItemDelegatedTo;
            ImageButton btnEditItem;
            ImageButton btnDeleteItem;

            public ItemViewHolder(@NonNull View itemView) {
                super(itemView);
                cbItemPurchased = itemView.findViewById(R.id.cbItemPurchased);
                tvItemName = itemView.findViewById(R.id.tvItemName);
                tvItemQuantity = itemView.findViewById(R.id.tvItemQuantity);
                tvItemPrice = itemView.findViewById(R.id.tvItemPrice);
                tvItemStore = itemView.findViewById(R.id.tvItemStore);
                tvItemDelegatedTo = itemView.findViewById(R.id.tvItemDelegatedTo);
                btnEditItem = itemView.findViewById(R.id.btnEditItem);
                btnDeleteItem = itemView.findViewById(R.id.btnDeleteItem);
            }
        }
    }
    
    // Interface for item click handling
    public interface OnItemClickListener {
        void onItemClicked(GroceryItem item);
    }
} 