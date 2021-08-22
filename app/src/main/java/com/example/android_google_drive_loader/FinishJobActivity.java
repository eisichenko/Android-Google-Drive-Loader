package com.example.android_google_drive_loader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;

import com.example.android_google_drive_loader.Helpers.OperationType;
import com.example.android_google_drive_loader.Helpers.StopwatchState;

public class FinishJobActivity extends AppCompatActivity {

    Button backButton;
    TextView timeTextView;

    Integer currentTimeInSeconds = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finish_job);

        backButton = findViewById(R.id.backMainScreenButton);
        backButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });

        if (MainActivity.operationType == OperationType.PUSH) {
            MainActivity.driveHelper.push(this, MainActivity.pickedDir, MainActivity.driveFolderName)
                    .addOnSuccessListener(isTestPassed -> {
                        if (isTestPassed) {
                            MainActivity.driveHelper.showToast("SUCCESS");
                        }
                        else {
                            MainActivity.driveHelper.showToast("FAIL (folders are not equal)");
                        }

                        MainActivity.stopwatchState = StopwatchState.STOPPED;
                    })
                    .addOnFailureListener(e -> {
                        MainActivity.driveHelper.showToast(e.getMessage());
                        System.out.println(e.getMessage());
                        e.printStackTrace();

                        MainActivity.stopwatchState = StopwatchState.STOPPED;
                    });
        }
        else if (MainActivity.operationType == OperationType.PULL) {
            MainActivity.driveHelper.pull(this, MainActivity.pickedDir, MainActivity.driveFolderName)
                    .addOnSuccessListener(isTestPassed -> {
                        if (isTestPassed) {
                            MainActivity.driveHelper.showToast("SUCCESS");
                        }
                        else {
                            MainActivity.driveHelper.showToast("FAIL (folders are not equal)");
                        }

                        MainActivity.stopwatchState = StopwatchState.STOPPED;
                    })
                    .addOnFailureListener(e -> {
                        MainActivity.driveHelper.showToast(e.getMessage());
                        System.out.println(e.getMessage());
                        e.printStackTrace();

                        MainActivity.stopwatchState = StopwatchState.STOPPED;
                    });
        }
        else {
            MainActivity.driveHelper.showToast("ERROR: Unknown operation");
            MainActivity.stopwatchState = StopwatchState.STOPPED;
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
