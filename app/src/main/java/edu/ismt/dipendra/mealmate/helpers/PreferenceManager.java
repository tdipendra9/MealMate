package edu.ismt.dipendra.mealmate.helpers;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Helper class to manage user preferences
 */
public class PreferenceManager {
    private static final String PREF_NAME = "MealMatePrefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_NOTIFICATIONS = "notifications";
    private static final String KEY_DIETARY_PREFERENCE = "dietary_preference";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";

    private final SharedPreferences sharedPreferences;

    public PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Save user information to preferences
     */
    public void saveUserInfo(String userId, String userName, String userEmail) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, userName);
        editor.putString(KEY_USER_EMAIL, userEmail);
        editor.apply();
    }

    /**
     * Clear user data from preferences
     */
    public void clearUserData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USER_NAME);
        editor.remove(KEY_USER_EMAIL);
        editor.apply();
    }

    /**
     * Get user ID from preferences
     */
    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, "");
    }

    /**
     * Get user name from preferences
     */
    public String getUserName() {
        return sharedPreferences.getString(KEY_USER_NAME, "");
    }

    /**
     * Get user email from preferences
     */
    public String getUserEmail() {
        return sharedPreferences.getString(KEY_USER_EMAIL, "");
    }

    /**
     * Check if dark mode is enabled
     */
    public boolean isDarkModeEnabled() {
        return sharedPreferences.getBoolean(KEY_DARK_MODE, false);
    }

    /**
     * Set dark mode preference
     */
    public void setDarkModeEnabled(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_DARK_MODE, enabled);
        editor.apply();
    }

    /**
     * Check if notifications are enabled
     */
    public boolean areNotificationsEnabled() {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS, true);
    }

    /**
     * Set notifications preference
     */
    public void setNotificationsEnabled(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_NOTIFICATIONS, enabled);
        editor.apply();
    }

    /**
     * Get dietary preference
     */
    public String getDietaryPreference() {
        return sharedPreferences.getString(KEY_DIETARY_PREFERENCE, "None");
    }

    /**
     * Set dietary preference
     */
    public void setDietaryPreference(String preference) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_DIETARY_PREFERENCE, preference);
        editor.apply();
    }
} 