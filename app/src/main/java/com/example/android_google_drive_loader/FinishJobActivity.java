package com.example.android_google_drive_loader;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.example.android_google_drive_loader.Enums.OperationType;
import com.example.android_google_drive_loader.Enums.StopwatchState;
import com.example.android_google_drive_loader.Enums.Theme;

public class FinishJobActivity extends AppCompatActivity {

    public Button backButton;
    public TextView timeTextView;

    public Integer currentTimeInSeconds = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            setTheme(R.style.Theme_Night);
        }
        else {
            setTheme(R.style.Theme_Day);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finish_job);

        backButton = findViewById(R.id.backMainScreenButton);
        backButton.setOnClickListener(view -> {
            MainActivity.operationType = OperationType.NONE;
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });

        if (savedInstanceState == null) {
            if (MainActivity.operationType == OperationType.PUSH) {
                MainActivity.driveHelper.push(this, MainActivity.pickedDir, MainActivity.driveFolderName)
                        .addOnSuccessListener(isTestPassed -> {
                            if (isTestPassed) {
                                MainActivity.msgHelper.showToast("SUCCESS");
                            }
                            else {
                                MainActivity.msgHelper.showToast("FAIL (folders are not equal)");
                            }

                            MainActivity.stopwatchState = StopwatchState.STOPPED;
                            MainActivity.operationType = OperationType.NONE;
                        })
                        .addOnFailureListener(e -> {
                            MainActivity.msgHelper.showToast(e.getMessage());
                            System.out.println(e.getMessage());
                            e.printStackTrace();

                            MainActivity.stopwatchState = StopwatchState.STOPPED;
                            MainActivity.operationType = OperationType.NONE;
                            backButton.setVisibility(View.VISIBLE);
                        });
            }
            else if (MainActivity.operationType == OperationType.PULL) {
                MainActivity.driveHelper.pull(this, MainActivity.pickedDir, MainActivity.driveFolderName)
                        .addOnSuccessListener(isTestPassed -> {
                            if (isTestPassed) {
                                MainActivity.msgHelper.showToast("SUCCESS");
                            }
                            else {
                                MainActivity.msgHelper.showToast("FAIL (folders are not equal)");
                            }

                            MainActivity.stopwatchState = StopwatchState.STOPPED;
                            MainActivity.operationType = OperationType.NONE;
                        })
                        .addOnFailureListener(e -> {
                            MainActivity.msgHelper.showToast(e.getMessage());
                            System.out.println(e.getMessage());
                            e.printStackTrace();

                            MainActivity.operationType = OperationType.NONE;
                            MainActivity.stopwatchState = StopwatchState.STOPPED;
                            backButton.setVisibility(View.VISIBLE);
                        });
            }
            else {
                MainActivity.stopwatchState = StopwatchState.STOPPED;
                backButton.setVisibility(View.VISIBLE);
            }

            timeTextView = findViewById(R.id.timeTextView);
            final Handler handler = new Handler();

            MainActivity.stopwatchState = StopwatchState.RUNNING;

            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (MainActivity.stopwatchState == StopwatchState.RUNNING) {
                        currentTimeInSeconds++;

                        Integer minutes = currentTimeInSeconds / 60;
                        Integer seconds = currentTimeInSeconds % 60;

                        if (minutes > 0) {
                            timeTextView.setText(getResources().getString(R.string.time_text_view_value)
                                    + " " + minutes + " m " + seconds.toString() + " s");
                        }
                        else {
                            timeTextView.setText(getResources().getString(R.string.time_text_view_value)
                                    + " " + seconds.toString() + " s");
                        }

                        handler.postDelayed(this, 1000);
                    }
                }
            });
        }
    }
}
