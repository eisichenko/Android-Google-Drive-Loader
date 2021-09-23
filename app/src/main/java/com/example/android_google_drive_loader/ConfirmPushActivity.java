package com.example.android_google_drive_loader;

import android.content.Intent;
import android.os.Bundle;
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
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android_google_drive_loader.Enums.DriveType;
import com.example.android_google_drive_loader.Enums.OperationType;
import com.example.android_google_drive_loader.Enums.Theme;
import com.example.android_google_drive_loader.Files.AbstractFile;
import com.example.android_google_drive_loader.Files.DriveFile;
import com.example.android_google_drive_loader.Files.LocalFile;
import com.example.android_google_drive_loader.Helpers.FetchHelper;
import com.example.android_google_drive_loader.Helpers.SetOperationsHelper;
import com.google.api.services.drive.Drive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class ConfirmPushActivity extends AppCompatActivity {
    private ArrayList<RecyclerViewItem> uploadFromDriveRecyclerViewItemList;
    private ArrayList<RecyclerViewItem> deleteOnDriveRecyclerViewItemList;
    private RecyclerView uploadToDriveRecyclerView;
    private RecyclerView deleteOnDriveRecyclerView;

    public static HashMap<DriveFile, HashSet<LocalFile>> uploadToDriveFiles;
    public static HashMap<DriveFile, HashSet<DriveFile>> deleteOnDriveFiles;

    private TextView noneUploadDriveTextView;
    private TextView noneDeleteDriveTextView;
    private Button startPushButton;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);
        MenuItem themeItem = menu.findItem(R.id.theme_menu);

        String themeString = MainActivity.settings.getString(MainActivity.THEME_CACHE_NAME, null);

        if (themeString != null) {
            if (themeString.equals(Theme.DAY.toString())) {
                themeItem.setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_theme_night));
            }
            else {
                themeItem.setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_theme_day));
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.theme_menu:
                if (MainActivity.currentTheme == Theme.DAY) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    MainActivity.settings.edit().putString(MainActivity.THEME_CACHE_NAME, Theme.NIGHT.toString()).apply();
                    item.setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_theme_day));
                    MainActivity.currentTheme = Theme.NIGHT;
                }
                else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    MainActivity.settings.edit().putString(MainActivity.THEME_CACHE_NAME, Theme.DAY.toString()).apply();
                    item.setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_theme_night));
                    MainActivity.currentTheme = Theme.DAY;
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            setTheme(R.style.Theme_Night);
        }
        else {
            setTheme(R.style.Theme_Day);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_push);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        uploadToDriveRecyclerView = findViewById(R.id.uploadFromDriveRecyclerView);
        uploadToDriveRecyclerView.setFocusable(false);

        deleteOnDriveRecyclerView = findViewById(R.id.deleteFromDriveRecyclerView);
        deleteOnDriveRecyclerView.setFocusable(false);

        DividerItemDecoration decoration = new DividerItemDecoration(getApplicationContext(),
                DividerItemDecoration.VERTICAL);
        decoration.setDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.list_divider));

        uploadToDriveRecyclerView.addItemDecoration(decoration);
        deleteOnDriveRecyclerView.addItemDecoration(decoration);

        if (MainActivity.operationType == OperationType.PUSH) {
            HashMap<DriveFile, HashSet<DriveFile>> driveFolderFiles;
            HashMap<LocalFile, HashSet<LocalFile>> localFolderFiles;

            uploadToDriveFiles = new HashMap<>();
            deleteOnDriveFiles = new HashMap<>();

            if (MainActivity.fetchHelper == null) {
                driveFolderFiles = new HashMap<>();
                localFolderFiles = new HashMap<>();
            }
            else {
                driveFolderFiles = MainActivity.fetchHelper.getDriveFolderFiles();
                localFolderFiles = MainActivity.fetchHelper.getLocalFolderFiles();
            }

            for (DriveFile driveFolder : driveFolderFiles.keySet()) {
                try {
                    HashSet<LocalFile> localSet = localFolderFiles.get(driveFolder);
                    HashSet<DriveFile> driveSet = driveFolderFiles.get(driveFolder);

                    HashSet<LocalFile> uploadRes = SetOperationsHelper.
                            relativeComplement(localSet, driveSet);

                    HashSet<DriveFile> deleteRes = SetOperationsHelper.
                            relativeComplement(driveSet, localSet);

                    uploadToDriveFiles.put(driveFolder, uploadRes);

                    deleteOnDriveFiles.put(driveFolder, deleteRes);

                } catch (Exception e) {
                    MainActivity.msgHelper.showToast(e.getMessage());
                }
            }

            uploadFromDriveRecyclerViewItemList = new ArrayList<>();
            deleteOnDriveRecyclerViewItemList = new ArrayList<>();

            if (FetchHelper.getMapSize(deleteOnDriveFiles) == 0) {
                noneDeleteDriveTextView = findViewById(R.id.noneDeleteDriveTextView);
                noneDeleteDriveTextView.setVisibility(View.VISIBLE);
            }

            if (FetchHelper.getMapSize(uploadToDriveFiles) == 0) {
                noneUploadDriveTextView = findViewById(R.id.noneUploadDriveTextView);
                noneUploadDriveTextView.setVisibility(View.VISIBLE);
            }

            for (DriveFile folder : uploadToDriveFiles.keySet()) {
                if (uploadToDriveFiles.get(folder).size() == 0) {
                    continue;
                }
                uploadFromDriveRecyclerViewItemList.add(new RecyclerViewItem("FOLDER: " + folder.getName(), DriveType.FOLDER));
                for (LocalFile file : uploadToDriveFiles.get(folder)) {
                    uploadFromDriveRecyclerViewItemList.add(new RecyclerViewItem(file.getName()));
                }
            }

            for (DriveFile folder : deleteOnDriveFiles.keySet()) {
                if (deleteOnDriveFiles.get(folder).size() == 0) {
                    continue;
                }
                deleteOnDriveRecyclerViewItemList.add(new RecyclerViewItem("FOLDER: " + folder.getName(), DriveType.FOLDER));
                for (DriveFile file : deleteOnDriveFiles.get(folder)) {
                    deleteOnDriveRecyclerViewItemList.add(new RecyclerViewItem(file.getName()));
                }
            }

            setAdapter();
        }

        startPushButton = findViewById(R.id.startPushButton);
        startPushButton.setOnClickListener(view -> {
            MainActivity.operationType = OperationType.PUSH;
            Intent intent = new Intent(this, FinishJobActivity.class);
            startActivity(intent);
        });
    }

    private void setAdapter() {
        RecyclerViewAdapter uploadFromDriveAdapter = new RecyclerViewAdapter(uploadFromDriveRecyclerViewItemList);
        RecyclerView.LayoutManager uploadFromDriveLayoutManager = new LinearLayoutManager(getApplicationContext());

        RecyclerViewAdapter deleteOnDriveAdapter = new RecyclerViewAdapter(deleteOnDriveRecyclerViewItemList);
        RecyclerView.LayoutManager deleteOnDriveLayoutManager = new LinearLayoutManager(getApplicationContext());

        uploadToDriveRecyclerView.setLayoutManager(uploadFromDriveLayoutManager);
        uploadToDriveRecyclerView.setItemAnimator(new DefaultItemAnimator());
        uploadToDriveRecyclerView.setAdapter(uploadFromDriveAdapter);

        deleteOnDriveRecyclerView.setLayoutManager(deleteOnDriveLayoutManager);
        deleteOnDriveRecyclerView.setItemAnimator(new DefaultItemAnimator());
        deleteOnDriveRecyclerView.setAdapter(deleteOnDriveAdapter);
    }
}