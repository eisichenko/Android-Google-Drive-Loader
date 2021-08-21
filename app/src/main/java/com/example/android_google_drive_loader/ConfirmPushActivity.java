package com.example.android_google_drive_loader;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android_google_drive_loader.Helpers.SetOperationsHelper;

import java.util.ArrayList;
import java.util.HashSet;

public class ConfirmPushActivity extends AppCompatActivity {
    private ArrayList<RecyclerViewItem> uploadFromDriveRecyclerViewItemList;
    private ArrayList<RecyclerViewItem> deleteOnDriveRecyclerViewItemList;
    private RecyclerView uploadFromDriveRecyclerView;
    private RecyclerView deleteOnDriveRecyclerView;

    public static HashSet<String> uploadToDriveFiles;
    public static HashSet<String> deleteOnDriveFiles;

    private TextView noneUploadDriveTextView;
    private TextView noneDeleteDriveTextView;
    private Button startPushButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_push);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        uploadFromDriveRecyclerView = findViewById(R.id.uploadFromDriveRecyclerView);
        deleteOnDriveRecyclerView = findViewById(R.id.deleteFromDriveRecyclerView);

        DividerItemDecoration decoration = new DividerItemDecoration(getApplicationContext(),
                DividerItemDecoration.VERTICAL);
        decoration.setDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.list_divider));

        uploadFromDriveRecyclerView.addItemDecoration(decoration);
        deleteOnDriveRecyclerView.addItemDecoration(decoration);

        HashSet<String> driveFolderFileNamesSet = MainActivity.fetchHelper.getDriveFolderFileNamesSet();
        HashSet<String> localFolderFileNamesSet = MainActivity.fetchHelper.getLocalFolderFileNamesSet();

        uploadToDriveFiles = SetOperationsHelper.
                relativeComplement(localFolderFileNamesSet, driveFolderFileNamesSet);

        deleteOnDriveFiles = SetOperationsHelper.
                relativeComplement(driveFolderFileNamesSet, localFolderFileNamesSet);

        uploadFromDriveRecyclerViewItemList = new ArrayList<>();
        deleteOnDriveRecyclerViewItemList = new ArrayList<>();

        if (uploadToDriveFiles.size() == 0) {
            noneUploadDriveTextView = findViewById(R.id.noneUploadDriveTextView);
            noneUploadDriveTextView.setVisibility(View.VISIBLE);
        }

        if (deleteOnDriveFiles.size() == 0) {
            noneDeleteDriveTextView = findViewById(R.id.noneDeleteDriveTextView);
            noneDeleteDriveTextView.setVisibility(View.VISIBLE);
        }

        for (String name : uploadToDriveFiles) {
            uploadFromDriveRecyclerViewItemList.add(new RecyclerViewItem(name));
        }

        for (String name : deleteOnDriveFiles) {
            deleteOnDriveRecyclerViewItemList.add(new RecyclerViewItem(name));
        }

        setAdapter();

        startPushButton = findViewById(R.id.startPushButton);
        startPushButton.setOnClickListener(view -> {
            MainActivity.driveHelper.push(MainActivity.pickedDir, MainActivity.driveFolderName)
            .addOnSuccessListener(isTestPassed -> {
                if (isTestPassed) {
                    MainActivity.driveHelper.showToast("SUCCESS");
                }
                else {
                    MainActivity.driveHelper.showToast("FAIL :(");
                }
            })
            .addOnFailureListener(e -> {
                MainActivity.driveHelper.showToast(e.getMessage());
                System.out.println(e.getMessage());
                e.printStackTrace();
            });
        });
    }

    private void setAdapter() {
        RecyclerViewAdapter uploadFromDriveAdapter = new RecyclerViewAdapter(uploadFromDriveRecyclerViewItemList);
        RecyclerView.LayoutManager uploadFromDriveLayoutManager = new LinearLayoutManager(getApplicationContext());

        RecyclerViewAdapter deleteOnDriveAdapter = new RecyclerViewAdapter(deleteOnDriveRecyclerViewItemList);
        RecyclerView.LayoutManager deleteOnDriveLayoutManager = new LinearLayoutManager(getApplicationContext());

        uploadFromDriveRecyclerView.setLayoutManager(uploadFromDriveLayoutManager);
        uploadFromDriveRecyclerView.setItemAnimator(new DefaultItemAnimator());
        uploadFromDriveRecyclerView.setAdapter(uploadFromDriveAdapter);

        deleteOnDriveRecyclerView.setLayoutManager(deleteOnDriveLayoutManager);
        deleteOnDriveRecyclerView.setItemAnimator(new DefaultItemAnimator());
        deleteOnDriveRecyclerView.setAdapter(deleteOnDriveAdapter);
    }
}