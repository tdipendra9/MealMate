package edu.ismt.dipendra.mealmate.services;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import edu.ismt.dipendra.mealmate.R;
import edu.ismt.dipendra.mealmate.activities.MainActivity;
import edu.ismt.dipendra.mealmate.firebase.FirebaseHelper;
import edu.ismt.dipendra.mealmate.model.MealReminder;

public class ReminderNotificationService extends BroadcastReceiver {
    
    private static final String TAG = "ReminderNotificationService";
    
    // Action constants
    public static final String ACTION_SHOW_NOTIFICATION = "mealmate.ACTION_SHOW_NOTIFICATION";
    public static final String ACTION_DISMISS_NOTIFICATION = "mealmate.ACTION_DISMISS_NOTIFICATION";
    public static final String ACTION_SNOOZE_NOTIFICATION = "mealmate.ACTION_SNOOZE_NOTIFICATION";
    
    // Extra constants
    public static final String EXTRA_REMINDER_ID = "reminder_id";
    public static final String EXTRA_MEAL_NAME = "meal_name";
    public static final String EXTRA_COOKING_TIME = "cooking_time";
    
    // Notification constants
    private static final String CHANNEL_ID = "meal_reminder_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final int DISMISS_REQUEST_CODE = 101;
    private static final int SNOOZE_REQUEST_CODE = 102;
    private static final int OPEN_APP_REQUEST_CODE = 103;
    
    // Snooze time in milliseconds (15 minutes)
    private static final long SNOOZE_TIME_MS = 15 * 60 * 1000;
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (action == null) {
            return;
        }
        
        switch (action) {
            case ACTION_SHOW_NOTIFICATION:
                showNotification(context, intent);
                break;
            case ACTION_DISMISS_NOTIFICATION:
                dismissNotification(context, intent);
                break;
            case ACTION_SNOOZE_NOTIFICATION:
                snoozeNotification(context, intent);
                break;
        }
    }
    
    private void showNotification(Context context, Intent intent) {
        String reminderId = intent.getStringExtra(EXTRA_REMINDER_ID);
        String mealName = intent.getStringExtra(EXTRA_MEAL_NAME);
        long cookingTimeMillis = intent.getLongExtra(EXTRA_COOKING_TIME, 0);
        
        if (reminderId == null || mealName == null || cookingTimeMillis == 0) {
            Log.e(TAG, "Missing required extras for notification");
            return;
        }
        
        // Format cooking time
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        String formattedTime = timeFormat.format(new Date(cookingTimeMillis));
        
        // Create intents for notification actions
        Intent dismissIntent = new Intent(context, ReminderNotificationService.class);
        dismissIntent.setAction(ACTION_DISMISS_NOTIFICATION);
        dismissIntent.putExtra(EXTRA_REMINDER_ID, reminderId);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(
                context, 
                DISMISS_REQUEST_CODE, 
                dismissIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        Intent snoozeIntent = new Intent(context, ReminderNotificationService.class);
        snoozeIntent.setAction(ACTION_SNOOZE_NOTIFICATION);
        snoozeIntent.putExtra(EXTRA_REMINDER_ID, reminderId);
        snoozeIntent.putExtra(EXTRA_MEAL_NAME, mealName);
        snoozeIntent.putExtra(EXTRA_COOKING_TIME, cookingTimeMillis);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                context, 
                SNOOZE_REQUEST_CODE, 
                snoozeIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // Create intent to open app
        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent openAppPendingIntent = PendingIntent.getActivity(
                context, 
                OPEN_APP_REQUEST_CODE, 
                openAppIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // Create notification channel for Android O and above
        createNotificationChannel(context);
        
        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(context.getString(R.string.reminder_notification_title))
                .setContentText(context.getString(R.string.reminder_notification_content, mealName, formattedTime))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(openAppPendingIntent)
                .setAutoCancel(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, 
                        context.getString(R.string.reminder_notification_action_dismiss), 
                        dismissPendingIntent)
                .addAction(android.R.drawable.ic_popup_reminder, 
                        context.getString(R.string.reminder_notification_action_snooze), 
                        snoozePendingIntent);
        
        // Show notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) 
                == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }
    
    private void dismissNotification(Context context, Intent intent) {
        String reminderId = intent.getStringExtra(EXTRA_REMINDER_ID);
        
        if (reminderId == null) {
            Log.e(TAG, "Missing reminder ID for dismiss action");
            return;
        }
        
        // Cancel notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(NOTIFICATION_ID);
        
        // Update reminder in Firebase (mark as inactive)
        FirebaseHelper firebaseHelper = FirebaseHelper.getInstance();
        firebaseHelper.getMealReminderById(reminderId, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof MealReminder) {
                    MealReminder reminder = (MealReminder) result;
                    reminder.setActive(false);
                    
                    firebaseHelper.updateMealReminder(reminder, new FirebaseHelper.FirebaseCallback() {
                        @Override
                        public void onSuccess(Object result) {
                            Log.d(TAG, "Reminder marked as inactive");
                        }
                        
                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "Failed to update reminder", e);
                        }
                    });
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to get reminder", e);
            }
        });
    }
    
    private void snoozeNotification(Context context, Intent intent) {
        String reminderId = intent.getStringExtra(EXTRA_REMINDER_ID);
        String mealName = intent.getStringExtra(EXTRA_MEAL_NAME);
        long cookingTimeMillis = intent.getLongExtra(EXTRA_COOKING_TIME, 0);
        
        if (reminderId == null || mealName == null || cookingTimeMillis == 0) {
            Log.e(TAG, "Missing required extras for snooze action");
            return;
        }
        
        // Cancel current notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(NOTIFICATION_ID);
        
        // Schedule new notification in 15 minutes
        Intent newIntent = new Intent(context, ReminderNotificationService.class);
        newIntent.setAction(ACTION_SHOW_NOTIFICATION);
        newIntent.putExtra(EXTRA_REMINDER_ID, reminderId);
        newIntent.putExtra(EXTRA_MEAL_NAME, mealName);
        newIntent.putExtra(EXTRA_COOKING_TIME, cookingTimeMillis);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 3, newIntent, PendingIntent.FLAG_IMMUTABLE);
        
        // Schedule alarm in 15 minutes
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            long snoozeTimeMillis = System.currentTimeMillis() + (15 * 60 * 1000); // 15 minutes
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        snoozeTimeMillis,
                        pendingIntent);
            } else {
                alarmManager.setExact(
                        android.app.AlarmManager.RTC_WAKEUP,
                        snoozeTimeMillis,
                        pendingIntent);
            }
            
            Log.d(TAG, "Notification snoozed for 15 minutes");
        }
    }
    
    private void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.reminder_channel_name);
            String description = context.getString(R.string.reminder_channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            
            // Register the channel with the system
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * Schedule a reminder notification
     * @param context Application context
     * @param reminder Reminder to schedule
     */
    public static void scheduleReminder(Context context, MealReminder reminder) {
        // Create intent for alarm
        Intent intent = new Intent(context, ReminderNotificationService.class);
        intent.setAction(ACTION_SHOW_NOTIFICATION);
        intent.putExtra(EXTRA_REMINDER_ID, reminder.getId());
        intent.putExtra(EXTRA_MEAL_NAME, reminder.getMealName());
        intent.putExtra(EXTRA_COOKING_TIME, reminder.getCookingTime());
        
        // Create pending intent
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                reminder.getId().hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // Get alarm manager
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        // Schedule alarm
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        reminder.getReminderTime(),
                        pendingIntent);
            } else {
                alarmManager.setExact(
                        android.app.AlarmManager.RTC_WAKEUP,
                        reminder.getReminderTime(),
                        pendingIntent);
            }
            
            Log.d(TAG, "Reminder scheduled for " + new Date(reminder.getReminderTime()));
        }
    }
    
    /**
     * Cancel a scheduled reminder notification
     *
     * @param context       Application context
     * @param reminderId    Reminder ID
     */
    public static void cancelReminder(Context context, String reminderId) {
        Intent intent = new Intent(context, ReminderNotificationService.class);
        intent.setAction(ACTION_SHOW_NOTIFICATION);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Reminder canceled: " + reminderId);
        }
    }
} 