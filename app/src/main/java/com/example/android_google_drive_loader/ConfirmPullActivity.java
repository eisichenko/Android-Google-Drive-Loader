package com.example.android_google_drive_loader;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android_google_drive_loader.Enums.DriveType;
import com.example.android_google_drive_loader.Enums.OperationType;
import com.example.android_google_drive_loader.Models.DriveFile;
import com.example.android_google_drive_loader.Models.LocalFile;
import com.example.android_google_drive_loader.Helpers.FetchHelper;
import com.example.android_google_drive_loader.Helpers.SetOperationsHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

public class ConfirmPullActivity extends AppCompatActivity {
    private ArrayList<RecyclerViewItem> downloadFromDriveRecyclerViewItemList;
    private ArrayList<RecyclerViewItem> deleteInLocalRecyclerViewItemList;
    private RecyclerView downloadFromDriveRecyclerView;
    private RecyclerView deleteInLocalRecyclerView;

    public static HashMap<LocalFile, HashSet<DriveFile>> downloadFromDriveFiles;
    public static HashMap<LocalFile, HashSet<LocalFile>> deleteInLocalFiles;
    public static HashMap<DriveFile, HashSet<DriveFile>> createFolderAndDownloadFromDriveFiles;
    public static HashMap<LocalFile, HashSet<LocalFile>> deleteLocalFolderAndFiles;

    private TextView noneDownloadFromDriveTextView;
    private TextView noneDeleteInLocalTextView;
    private Button startPullButton;

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

        downloadFromDriveRecyclerView = findViewById(R.id.downloadFromDriveRecyclerView);
        downloadFromDriveRecyclerView.setFocusable(false);

        deleteInLocalRecyclerView = findViewById(R.id.deleteInLocalRecyclerView);
        deleteInLocalRecyclerView.setFocusable(false);

        DividerItemDecoration decoration = new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL);
        decoration.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(this, R.drawable.list_divider)));

        downloadFromDriveRecyclerView.addItemDecoration(decoration);
        deleteInLocalRecyclerView.addItemDecoration(decoration);

        if (MainActivity.operationType == OperationType.PULL) {
            HashMap<DriveFile, HashSet<DriveFile>> driveFolderFiles;
            HashMap<LocalFile, HashSet<LocalFile>> localFolderFiles;

            deleteInLocalFiles = new HashMap<>();
            downloadFromDriveFiles = new HashMap<>();
            createFolderAndDownloadFromDriveFiles = new HashMap<>();
            deleteLocalFolderAndFiles = new HashMap<>();

            if (MainActivity.fetchHelper == null) {
                MainActivity.msgHelper.showToast("Fetch helper is null, please try again");
                return;
            }

            HashSet<DriveFile> driveFolders = MainActivity.fetchHelper.getDriveFolders();
            HashSet<LocalFile> localFolders = MainActivity.fetchHelper.getLocalFolders();

            HashSet<DriveFile> driveFoldersToCreateInLocal =
                    SetOperationsHelper.relativeComplement(driveFolders, localFolders);

            System.out.println("LOCAL FOLDERS TO CREATE");
            System.out.println(driveFoldersToCreateInLocal);

            HashSet<LocalFile> localFoldersToDelete =
                    SetOperationsHelper.relativeComplement(localFolders, driveFolders);

            System.out.println("LOCAL FOLDERS TO DELETE");
            System.out.println(localFoldersToDelete);

            driveFolderFiles = MainActivity.fetchHelper.getDriveFolderFiles();
            localFolderFiles = MainActivity.fetchHelper.getLocalFolderFiles();

            for (LocalFile localFolder : localFolderFiles.keySet()) {
                try {
                    if (!localFolderFiles.containsKey(localFolder) ||
                            !driveFolderFiles.containsKey(localFolder)) {
                        continue;
                    }

                    HashSet<DriveFile> driveSet = driveFolderFiles.get(localFolder);
                    HashSet<LocalFile> localSet = localFolderFiles.get(localFolder);

                    HashSet<DriveFile> downloadRes = SetOperationsHelper.
                            relativeComplement(driveSet, localSet);

                    HashSet<LocalFile> deleteRes = SetOperationsHelper.
                            relativeComplement(localSet, driveSet);

                    downloadFromDriveFiles.put(localFolder, downloadRes);
                    deleteInLocalFiles.put(localFolder, deleteRes);

                } catch (Exception e) {
                    MainActivity.msgHelper.showToast(e.getMessage());
                    e.printStackTrace();
                }
            }

            System.out.println("TO DOWNLOAD");
            System.out.println(downloadFromDriveFiles);

            downloadFromDriveRecyclerViewItemList = new ArrayList<>();
            deleteInLocalRecyclerViewItemList = new ArrayList<>();

            if (FetchHelper.getMapSize(downloadFromDriveFiles) == 0 && driveFoldersToCreateInLocal.size() == 0) {
                noneDownloadFromDriveTextView = findViewById(R.id.noneDownloadFromDriveTextView);
                noneDownloadFromDriveTextView.setVisibility(View.VISIBLE);
            }

            if (FetchHelper.getMapSize(deleteInLocalFiles) == 0 && localFoldersToDelete.size() == 0) {
                noneDeleteInLocalTextView = findViewById(R.id.noneDeleteInLocalTextView);
                noneDeleteInLocalTextView.setVisibility(View.VISIBLE);
            }

            for (LocalFile folder : downloadFromDriveFiles.keySet()) {
                if (downloadFromDriveFiles.get(folder).size() == 0) {
                    continue;
                }
                downloadFromDriveRecyclerViewItemList.add(new RecyclerViewItem(String.format("FOLDER: \"%s\"", folder.getAbsolutePath()), DriveType.FOLDER));
                for (DriveFile file : downloadFromDriveFiles.get(folder)) {
                    downloadFromDriveRecyclerViewItemList.add(new RecyclerViewItem(file.getName()));
                }
            }

            for (DriveFile folder : driveFoldersToCreateInLocal) {
                createFolderAndDownloadFromDriveFiles.put(folder, driveFolderFiles.get(folder));
                downloadFromDriveRecyclerViewItemList.add(new RecyclerViewItem(String.format("[CREATE] FOLDER: \"%s\"", folder.getAbsolutePath()), DriveType.FOLDER));
                System.out.println(String.format("[CREATE] %s", folder.getAbsolutePath()));
                for (DriveFile file : createFolderAndDownloadFromDriveFiles.get(folder)) {
                    System.out.println("DOWNLOAD WITH FOLDER " + file.getName());
                    downloadFromDriveRecyclerViewItemList.add(new RecyclerViewItem(file.getName()));
                }
            }

            System.out.println("\nTO DELETE");
            System.out.println(deleteInLocalFiles);

            for (LocalFile folder : deleteInLocalFiles.keySet()) {
                if (deleteInLocalFiles.get(folder).size() == 0) {
                    continue;
                }

                deleteInLocalRecyclerViewItemList.add(new RecyclerViewItem(String.format("FOLDER: \"%s\"", folder.getAbsolutePath()), DriveType.FOLDER));
                for (LocalFile file : deleteInLocalFiles.get(folder)) {
                    deleteInLocalRecyclerViewItemList.add(new RecyclerViewItem(file.getName()));
                }
            }

            for (LocalFile folder : localFoldersToDelete) {
                deleteLocalFolderAndFiles.put(folder, localFolderFiles.get(folder));

                deleteInLocalRecyclerViewItemList.add(new RecyclerViewItem(String.format("[DELETE] FOLDER: \"%s\"", folder.getAbsolutePath()), DriveType.FOLDER));
                System.out.println(String.format("[DELETE] FOLDER: \"%s\"", folder.getAbsolutePath()));

                for (LocalFile file : localFolderFiles.get(folder)) {
                    System.out.println("DELETE WITH FOLDER: " + file.getName());
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