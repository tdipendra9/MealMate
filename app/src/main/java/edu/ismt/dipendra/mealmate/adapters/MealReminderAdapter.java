package edu.ismt.dipendra.mealmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.model.MealReminder;

public class MealReminderAdapter extends RecyclerView.Adapter<MealReminderAdapter.ReminderViewHolder> {

    private final Context context;
    private final List<MealReminder> reminders;
    private final OnReminderActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault());

    public interface OnReminderActionListener {
        void onReminderEdit(MealReminder reminder);
        void onReminderDelete(MealReminder reminder);
    }

    public MealReminderAdapter(Context context, List<MealReminder> reminders, OnReminderActionListener listener) {
        this.context = context;
        this.reminders = reminders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_meal_reminder, parent, false);
        return new ReminderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        MealReminder reminder = reminders.get(position);
        
        holder.tvMealName.setText(reminder.getMealName());
        
        // Format reminder time
        String reminderTime = dateFormat.format(new Date(reminder.getReminderTime()));
        holder.tvReminderTime.setText(reminderTime);
        
        // Format cooking time
        String cookingTime = dateFormat.format(new Date(reminder.getCookingTime()));
        holder.tvCookingTime.setText(context.getString(R.string.cooking_time_format, cookingTime));
        
        // Set click listeners
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReminderEdit(reminder);
            }
        });
        
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReminderDelete(reminder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return reminders.size();
    }

    static class ReminderViewHolder extends RecyclerView.ViewHolder {
        TextView tvMealName, tvReminderTime, tvCookingTime;
        ImageButton btnEdit, btnDelete;

        ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMealName = itemView.findViewById(R.id.tvMealName);
            tvReminderTime = itemView.findViewById(R.id.tvReminderTime);
            tvCookingTime = itemView.findViewById(R.id.tvCookingTime);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
} 