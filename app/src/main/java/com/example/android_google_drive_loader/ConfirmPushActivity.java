package com.example.android_google_drive_loader;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class ConfirmPushActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_push);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
}