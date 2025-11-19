package com.example.medzone.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Helper class untuk menyimpan data user secara lokal menggunakan SharedPreferences
 */
public class UserPreferences {
    private static final String PREF_NAME = "MedZoneUserPrefs";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_PHONE = "userPhone";
    private static final String KEY_USER_PHOTO_URL = "userPhotoUrl";
    private static final String KEY_JOIN_DATE = "joinDate";
    private static final String KEY_LAST_SYNC = "lastSync";

    private final SharedPreferences prefs;

    public UserPreferences(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Save user data
    public void saveUserData(String userId, String name, String email, String phone, String photoUrl, long joinDate) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_PHONE, phone);
        editor.putString(KEY_USER_PHOTO_URL, photoUrl);
        editor.putLong(KEY_JOIN_DATE, joinDate);
        editor.putLong(KEY_LAST_SYNC, System.currentTimeMillis());
        editor.apply();
    }

    // Get individual fields
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, null);
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }

    public String getUserPhone() {
        return prefs.getString(KEY_USER_PHONE, null);
    }

    public String getUserPhotoUrl() {
        return prefs.getString(KEY_USER_PHOTO_URL, null);
    }

    public long getJoinDate() {
        return prefs.getLong(KEY_JOIN_DATE, 0);
    }

    public long getLastSync() {
        return prefs.getLong(KEY_LAST_SYNC, 0);
    }

    // Check if data needs refresh (older than 1 hour)
    public boolean needsRefresh() {
        long lastSync = getLastSync();
        if (lastSync == 0) return true;

        long oneHourInMillis = 60 * 60 * 1000;
        return (System.currentTimeMillis() - lastSync) > oneHourInMillis;
    }

    // Clear all user data (for logout)
    public void clearUserData() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }

    // Check if user data exists locally
    public boolean hasUserData() {
        return prefs.getString(KEY_USER_ID, null) != null;
    }
}

