package edu.ismt.dipendra.mealmate.utils;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.firebase.FirebaseHelper;
import edu.ismt.dipendra.mealmate.model.Meal;
import edu.ismt.dipendra.mealmate.model.MealReminder;
import edu.ismt.dipendra.mealmate.services.ReminderNotificationService;

/**
 * Utility class for managing meal reminders
 */
public class ReminderManager {
    
    private static final String TAG = "ReminderManager";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.getDefault());
    private static final SimpleDateFormat FULL_FORMAT = new SimpleDateFormat("EEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault());
    
    /**
     * Show dialog to set a reminder for a meal
     *
     * @param context   Application context
     * @param meal      Meal to set reminder for
     */
    public static void showSetReminderDialog(Context context, Meal meal) {
        // Inflate the dialog layout
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_set_reminder, null);
        
        // Get references to views
        TextView tvMealName = dialogView.findViewById(R.id.tvMealName);
        TextView tvCookingTime = dialogView.findViewById(R.id.tvCookingTime);
        Button btnSelectDate = dialogView.findViewById(R.id.btnSelectDate);
        Button btnSelectTime = dialogView.findViewById(R.id.btnSelectTime);
        TextView tvReminderTime = dialogView.findViewById(R.id.tvReminderTime);
        
        // Set meal name
        tvMealName.setText(meal.getName());
        
        // Initialize calendar with current date/time
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, 1); // Default to 1 hour from now
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        
        // Update button texts
        updateDateButtonText(btnSelectDate, calendar);
        updateTimeButtonText(btnSelectTime, calendar);
        updateReminderTimeText(tvReminderTime, calendar);
        
        // Set cooking time
        int cookingTimeMinutes = meal.getCookingTime();
        String cookingTimeText = context.getString(R.string.cooking_time) + " " + formatCookingTime(cookingTimeMinutes);
        tvCookingTime.setText(cookingTimeText);
        
        // Create dialog
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.set_reminder)
                .setView(dialogView)
                .setPositiveButton(R.string.set_reminder, null) // Set later to prevent auto-dismiss
                .setNegativeButton(R.string.cancel, null)
                .create();
        
        // Show dialog
        dialog.show();
        
        // Set date button click listener
        btnSelectDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    context,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        
                        updateDateButtonText(btnSelectDate, calendar);
                        updateReminderTimeText(tvReminderTime, calendar);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            
            // Set min date to today
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            datePickerDialog.show();
        });
        
        // Set time button click listener
        btnSelectTime.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    context,
                    (view, hourOfDay, minute) -> {
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);
                        
                        updateTimeButtonText(btnSelectTime, calendar);
                        updateReminderTimeText(tvReminderTime, calendar);
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
            );
            
            timePickerDialog.show();
        });
        
        // Set positive button click listener
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            // Check if selected time is in the future
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                Toast.makeText(context, "Please select a future time", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Calculate cooking time (reminder time + 30 minutes)
            long reminderTimeMillis = calendar.getTimeInMillis();
            long cookingTimeMillis = reminderTimeMillis + (30 * 60 * 1000); // 30 minutes later
            
            // Create reminder
            createReminder(context, meal, reminderTimeMillis, cookingTimeMillis);
            
            // Dismiss dialog
            dialog.dismiss();
        });
    }
    
    /**
     * Create a new reminder for a meal
     *
     * @param context       Application context
     * @param meal          Meal to set reminder for
     * @param reminderTimeMillis  Reminder time in milliseconds
     * @param cookingTimeMillis  Cooking time in milliseconds
     */
    private static void createReminder(Context context, Meal meal, long reminderTimeMillis, long cookingTimeMillis) {
        // Create reminder object
        MealReminder reminder = new MealReminder(
                FirebaseHelper.getInstance().getCurrentUserId(),
                meal.getId(),
                meal.getName(),
                reminderTimeMillis,
                cookingTimeMillis
        );
        
        // Save to Firebase
        FirebaseHelper.getInstance().addMealReminder(reminder, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof MealReminder) {
                    MealReminder savedReminder = (MealReminder) result;
                    
                    // Schedule notification
                    ReminderNotificationService.scheduleReminder(context, savedReminder);
                    
                    // Show success message
                    Toast.makeText(context, 
                            "Reminder set for " + FULL_FORMAT.format(new Date(reminderTimeMillis)), 
                            Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(context, "Failed to set reminder: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Update date button text
     *
     * @param button    Button to update
     * @param calendar  Calendar with selected date
     */
    private static void updateDateButtonText(Button button, Calendar calendar) {
        button.setText(DATE_FORMAT.format(calendar.getTime()));
    }
    
    /**
     * Update time button text
     *
     * @param button    Button to update
     * @param calendar  Calendar with selected time
     */
    private static void updateTimeButtonText(Button button, Calendar calendar) {
        button.setText(TIME_FORMAT.format(calendar.getTime()));
    }
    
    /**
     * Update reminder time text
     *
     * @param textView  TextView to update
     * @param calendar  Calendar with selected cooking time
     */
    private static void updateReminderTimeText(TextView textView, Calendar calendar) {
        textView.setText(FULL_FORMAT.format(calendar.getTime()));
    }
    
    private static String formatCookingTime(int minutes) {
        if (minutes < 60) {
            return minutes + " minutes";
        } else {
            int hours = minutes / 60;
            int remainingMinutes = minutes % 60;
            
            if (remainingMinutes == 0) {
                return hours + " hour" + (hours > 1 ? "s" : "");
            } else {
                return hours + " hour" + (hours > 1 ? "s" : "") + " " + remainingMinutes + " minute" + (remainingMinutes > 1 ? "s" : "");
            }
        }
    }
    
    /**
     * Delete all reminders for a meal
     *
     * @param context   Application context
     * @param mealId    Meal ID
     */
    public static void deleteRemindersForMeal(Context context, String mealId) {
        FirebaseHelper.getInstance().getMealRemindersForMeal(mealId, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof List) {
                    List<MealReminder> reminders = (List<MealReminder>) result;
                    
                    for (MealReminder reminder : reminders) {
                        // Cancel scheduled notification
                        ReminderNotificationService.cancelReminder(context, reminder.getId());
                        
                        // Delete from Firebase
                        FirebaseHelper.getInstance().deleteMealReminder(reminder.getId(), new FirebaseHelper.FirebaseCallback() {
                            @Override
                            public void onSuccess(Object result) {
                                // Reminder deleted successfully
                            }
                            
                            @Override
                            public void onFailure(Exception e) {
                                // Failed to delete reminder
                            }
                        });
                    }
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                // Failed to get reminders
            }
        });
    }
} 