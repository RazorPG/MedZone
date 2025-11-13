package com.example.medzone;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Paksa aplikasi selalu menggunakan mode terang (non-night)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}

