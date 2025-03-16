package edu.ismt.dipendra.mealmate.adapters;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.model.GeotaggedStore;

public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.StoreViewHolder> {
    
    private final Context context;
    private final List<GeotaggedStore> stores;
    private final OnStoreClickListener listener;
    private Location currentLocation;
    
    public interface OnStoreClickListener {
        void onStoreClick(GeotaggedStore store);
        void onEditStore(GeotaggedStore store);
        void onDeleteStore(GeotaggedStore store);
    }
    
    public StoreAdapter(Context context, List<GeotaggedStore> stores, OnStoreClickListener listener) {
        this.context = context;
        this.stores = stores;
        this.listener = listener;
    }
    
    public void setCurrentLocation(Location location) {
        this.currentLocation = location;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public StoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_store, parent, false);
        return new StoreViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull StoreViewHolder holder, int position) {
        GeotaggedStore store = stores.get(position);
        
        holder.tvStoreName.setText(store.getName());
        holder.tvStoreAddress.setText(store.getAddress());
        
        // Calculate distance if current location is available
        if (currentLocation != null) {
            float[] results = new float[1];
            Location.distanceBetween(
                    currentLocation.getLatitude(), currentLocation.getLongitude(),
                    store.getLatitude(), store.getLongitude(),
                    results);
            
            // Convert meters to kilometers
            float distanceInKm = results[0] / 1000;
            holder.tvStoreDistance.setText(context.getString(R.string.store_distance, distanceInKm));
            holder.tvStoreDistance.setVisibility(View.VISIBLE);
        } else {
            holder.tvStoreDistance.setVisibility(View.GONE);
        }
        
        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onStoreClick(store);
            }
        });
        
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onEditStore(store);
                return true;
            }
            return false;
        });
        
        holder.btnNavigate.setOnClickListener(v -> {
            // Open Google Maps navigation
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + store.getLatitude() + "," + store.getLongitude());
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            
            if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(mapIntent);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return stores.size();
    }
    
    public void updateStores(List<GeotaggedStore> newStores) {
        stores.clear();
        stores.addAll(newStores);
        notifyDataSetChanged();
    }
    
    static class StoreViewHolder extends RecyclerView.ViewHolder {
        TextView tvStoreName;
        TextView tvStoreAddress;
        TextView tvStoreDistance;
        ImageButton btnNavigate;
        
        public StoreViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStoreName = itemView.findViewById(R.id.tvStoreName);
            tvStoreAddress = itemView.findViewById(R.id.tvStoreAddress);
            tvStoreDistance = itemView.findViewById(R.id.tvStoreDistance);
            btnNavigate = itemView.findViewById(R.id.btnNavigate);
        }
    }
} 