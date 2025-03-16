package edu.ismt.dipendra.mealmate.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import java.util.Date;
import java.util.List;

import edu.ismt.dipendra.mealmate.firebase.FirebaseHelper;
import edu.ismt.dipendra.mealmate.model.DelegationHistory;
import edu.ismt.dipendra.mealmate.model.GroceryItem;

public class SMSDelegationManager {
    private static final String TAG = "SMSDelegationManager";
    
    private final Context context;
    private final FirebaseHelper firebaseHelper;
    
    public SMSDelegationManager(Context context) {
        this.context = context;
        this.firebaseHelper = FirebaseHelper.getInstance();
    }
    
    public boolean delegateItems(String phoneNumber, List<GroceryItem> items, String groceryListId) {
        if (!isValidPhoneNumber(phoneNumber)) {
            Log.e(TAG, "Invalid phone number: " + phoneNumber);
            return false;
        }
        
        if (items == null || items.isEmpty()) {
            Log.e(TAG, "No items to delegate");
            return false;
        }
        
        // Format SMS message with emojis
        String message = EmojiUtils.formatSmsMessage(items);
        
        // Create delegation history
        DelegationHistory history = new DelegationHistory();
        history.setPhoneNumber(phoneNumber);
        history.setTimestamp(new Date());
        history.setItems(items);
        history.setGroceryListId(groceryListId);
        
        // Save delegation history to Firebase
        firebaseHelper.saveDelegationHistory(history, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "Delegation history saved successfully");
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to save delegation history: " + e.getMessage());
            }
        });
        
        // Update items in Firebase as delegated
        firebaseHelper.delegateGroceryItems(groceryListId, items, phoneNumber, new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "Items marked as delegated successfully");
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to mark items as delegated: " + e.getMessage());
            }
        });
        
        // Send SMS using intent
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + phoneNumber));
            intent.putExtra("sms_body", message);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to send SMS: " + e.getMessage());
            return false;
        }
    }
    
    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && 
               !phoneNumber.trim().isEmpty() && 
               PhoneNumberUtils.isGlobalPhoneNumber(phoneNumber.trim());
    }
    
    public void getDelegationHistory(String groceryListId, FirebaseHelper.FirebaseCallback callback) {
        firebaseHelper.getDelegationHistory(groceryListId, callback);
    }
} 