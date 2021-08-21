package com.example.android_google_drive_loader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class FinishJobActivity extends AppCompatActivity {

    Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finish_job);

        backButton = findViewById(R.id.backMainScreenButton);
        backButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });

        MainActivity.driveHelper.push(this, MainActivity.pickedDir, MainActivity.driveFolderName)
                .addOnSuccessListener(isTestPassed -> {
                    if (isTestPassed) {
                        MainActivity.driveHelper.showToast("SUCCESS");
                    }
                    else {
                        MainActivity.driveHelper.showToast("FAIL (folders are not equal)");
                    }
                })
                .addOnFailureListener(e -> {
                    MainActivity.driveHelper.showToast(e.getMessage());
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                });
    }
}