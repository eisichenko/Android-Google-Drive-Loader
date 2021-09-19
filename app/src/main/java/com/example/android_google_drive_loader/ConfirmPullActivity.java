package com.example.android_google_drive_loader;

import android.content.Intent;
import android.graphics.Color;
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
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android_google_drive_loader.Enums.DriveType;
import com.example.android_google_drive_loader.Enums.OperationType;
import com.example.android_google_drive_loader.Enums.Theme;
import com.example.android_google_drive_loader.Helpers.GoogleDriveHelper;
import com.example.android_google_drive_loader.Helpers.LocalFileHelper;
import com.example.android_google_drive_loader.Helpers.MessageHelper;
import com.example.android_google_drive_loader.Helpers.SearchHelper;
import com.example.android_google_drive_loader.Helpers.SetOperationsHelper;
import com.google.api.services.drive.model.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class ConfirmPullActivity extends AppCompatActivity {
    private ArrayList<RecyclerViewItem> downloadFromDriveRecyclerViewItemList;
    private ArrayList<RecyclerViewItem> deleteInLocalRecyclerViewItemList;
    private RecyclerView downloadFromDriveRecyclerView;
    private RecyclerView deleteInLocalRecyclerView;

    public static HashMap<File, HashSet<File>> downloadFromDriveFiles;
    public static HashMap<DocumentFile, HashSet<DocumentFile>> deleteInLocalFiles;

    private TextView noneDownloadFromDriveTextView;
    private TextView noneDeleteInLocalTextView;
    private Button startPullButton;

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
        setContentView(R.layout.activity_confirm_pull);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        downloadFromDriveRecyclerView = findViewById(R.id.downloadFromDriveRecyclerView);
        downloadFromDriveRecyclerView.setFocusable(false);

        deleteInLocalRecyclerView = findViewById(R.id.deleteInLocalRecyclerView);
        deleteInLocalRecyclerView.setFocusable(false);

        DividerItemDecoration decoration = new DividerItemDecoration(getApplicationContext(),
                DividerItemDecoration.VERTICAL);
        decoration.setDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.list_divider));

        downloadFromDriveRecyclerView.addItemDecoration(decoration);
        deleteInLocalRecyclerView.addItemDecoration(decoration);

        if (MainActivity.operationType == OperationType.PULL) {
            HashMap<File, HashSet<File>> driveFolderFiles;
            HashMap<DocumentFile, HashSet<DocumentFile>> localFolderFiles;

            deleteInLocalFiles = new HashMap<>();
            downloadFromDriveFiles = new HashMap<>();

            if (MainActivity.fetchHelper == null) {
                driveFolderFiles = new HashMap<>();
                localFolderFiles = new HashMap<>();
            }
            else {
                driveFolderFiles = MainActivity.fetchHelper.getDriveFolderFiles();
                localFolderFiles = MainActivity.fetchHelper.getLocalFolderFiles();
            }

            for (File driveFolder : driveFolderFiles.keySet()) {
                try {
                    DocumentFile localFolder = SearchHelper.findLocalFileByName(localFolderFiles, driveFolder.getName());

                    HashSet<String> localSet = LocalFileHelper.getLocalNamesFromSet(localFolderFiles.get(localFolder));
                    HashSet<String> driveSet = GoogleDriveHelper.getDriveNamesFromSet(driveFolderFiles.get(driveFolder));

                    HashSet<String> deleteRes = SetOperationsHelper.
                            relativeComplement(localSet, driveSet);

                    HashSet<String> downloadRes = SetOperationsHelper.
                            relativeComplement(driveSet, localSet);

                    deleteInLocalFiles.put(localFolder, SearchHelper.strSetToDocumentFile(
                            localFolderFiles.get(localFolder),
                            deleteRes));

                    downloadFromDriveFiles.put(driveFolder, SearchHelper.strSetToFile(
                            driveFolderFiles.get(driveFolder),
                            downloadRes));

                } catch (Exception e) {
                    MainActivity.msgHelper.showToast(e.getMessage());
                }
            }

            downloadFromDriveRecyclerViewItemList = new ArrayList<>();
            deleteInLocalRecyclerViewItemList = new ArrayList<>();

            if (GoogleDriveHelper.getMapSize(downloadFromDriveFiles) == 0) {
                noneDownloadFromDriveTextView = findViewById(R.id.noneDownloadFromDriveTextView);
                noneDownloadFromDriveTextView.setVisibility(View.VISIBLE);
            }

            if (LocalFileHelper.getMapSize(deleteInLocalFiles) == 0) {
                noneDeleteInLocalTextView = findViewById(R.id.noneDeleteInLocalTextView);
                noneDeleteInLocalTextView.setVisibility(View.VISIBLE);
            }

            for (File folder : downloadFromDriveFiles.keySet()) {
                if (downloadFromDriveFiles.get(folder).size() == 0) {
                    continue;
                }
                downloadFromDriveRecyclerViewItemList.add(new RecyclerViewItem("FOLDER: " + folder.getName(), DriveType.FOLDER));
                for (File file : downloadFromDriveFiles.get(folder)) {
                    downloadFromDriveRecyclerViewItemList.add(new RecyclerViewItem(file.getName()));
                }
            }

            for (DocumentFile folder : deleteInLocalFiles.keySet()) {
                if (deleteInLocalFiles.get(folder).size() == 0) {
                    continue;
                }
                deleteInLocalRecyclerViewItemList.add(new RecyclerViewItem("FOLDER: " + folder.getName(), DriveType.FOLDER));
                for (DocumentFile file : deleteInLocalFiles.get(folder)) {
                    System.out.println("DELETE: " + file.getName());
                    deleteInLocalRecyclerViewItemList.add(new RecyclerViewItem(file.getName()));
                }
            }

            setAdapter();
        }

        startPullButton = findViewById(R.id.startPullButton);
        startPullButton.setOnClickListener(view -> {
            MainActivity.operationType = OperationType.PULL;
            Intent intent = new Intent(this, FinishJobActivity.class);
            startActivity(intent);
        });
    }

    private void setAdapter() {
        RecyclerViewAdapter downloadFromDriveAdapter = new RecyclerViewAdapter(downloadFromDriveRecyclerViewItemList);
        RecyclerView.LayoutManager downloadFromDriveLayoutManager = new LinearLayoutManager(getApplicationContext());

        RecyclerViewAdapter deleteInLocalAdapter = new RecyclerViewAdapter(deleteInLocalRecyclerViewItemList);
        RecyclerView.LayoutManager deleteInLocalLayoutManager = new LinearLayoutManager(getApplicationContext());

        downloadFromDriveRecyclerView.setLayoutManager(downloadFromDriveLayoutManager);
        downloadFromDriveRecyclerView.setItemAnimator(new DefaultItemAnimator());
        downloadFromDriveRecyclerView.setAdapter(downloadFromDriveAdapter);

        deleteInLocalRecyclerView.setLayoutManager(deleteInLocalLayoutManager);
        deleteInLocalRecyclerView.setItemAnimator(new DefaultItemAnimator());
        deleteInLocalRecyclerView.setAdapter(deleteInLocalAdapter);
    }
}