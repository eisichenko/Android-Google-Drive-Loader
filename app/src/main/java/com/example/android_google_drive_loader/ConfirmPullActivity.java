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

import com.example.android_google_drive_loader.Enums.OperationType;
import com.example.android_google_drive_loader.Enums.Theme;
import com.example.android_google_drive_loader.Helpers.SetOperationsHelper;

import java.util.ArrayList;
import java.util.HashSet;

public class ConfirmPullActivity extends AppCompatActivity {
    private ArrayList<RecyclerViewItem> downloadFromDriveRecyclerViewItemList;
    private ArrayList<RecyclerViewItem> deleteInLocalRecyclerViewItemList;
    private RecyclerView downloadFromDriveRecyclerView;
    private RecyclerView deleteInLocalRecyclerView;

    public static HashSet<String> downloadFromDriveFiles;
    public static HashSet<String> deleteInLocalFiles;

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

        HashSet<String> driveFolderFileNamesSet;
        HashSet<String> localFolderFileNamesSet;

        if (MainActivity.fetchHelper == null) {
            driveFolderFileNamesSet = new HashSet<>();
            localFolderFileNamesSet = new HashSet<>();
        }
        else {
            driveFolderFileNamesSet = MainActivity.fetchHelper.getDriveFolderFileNamesSet();
            localFolderFileNamesSet = MainActivity.fetchHelper.getLocalFolderFileNamesSet();
        }

        deleteInLocalFiles = SetOperationsHelper.
                relativeComplement(localFolderFileNamesSet, driveFolderFileNamesSet);

        downloadFromDriveFiles = SetOperationsHelper.
                relativeComplement(driveFolderFileNamesSet, localFolderFileNamesSet);

        downloadFromDriveRecyclerViewItemList = new ArrayList<>();
        deleteInLocalRecyclerViewItemList = new ArrayList<>();

        if (downloadFromDriveFiles.size() == 0) {
            noneDownloadFromDriveTextView = findViewById(R.id.noneDownloadFromDriveTextView);
            noneDownloadFromDriveTextView.setVisibility(View.VISIBLE);
        }

        if (deleteInLocalFiles.size() == 0) {
            noneDeleteInLocalTextView = findViewById(R.id.noneDeleteInLocalTextView);
            noneDeleteInLocalTextView.setVisibility(View.VISIBLE);
        }

        for (String name : downloadFromDriveFiles) {
            downloadFromDriveRecyclerViewItemList.add(new RecyclerViewItem(name));
        }

        for (String name : deleteInLocalFiles) {
            deleteInLocalRecyclerViewItemList.add(new RecyclerViewItem(name));
        }

        setAdapter();

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