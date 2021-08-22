package com.example.android_google_drive_loader;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.android_google_drive_loader.Helpers.OperationType;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_pull);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        downloadFromDriveRecyclerView = findViewById(R.id.downloadFromDriveRecyclerView);
        deleteInLocalRecyclerView = findViewById(R.id.deleteInLocalRecyclerView);

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