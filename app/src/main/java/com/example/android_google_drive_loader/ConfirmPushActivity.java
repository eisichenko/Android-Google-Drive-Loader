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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

public class ConfirmPushActivity extends AppCompatActivity {
    private ArrayList<RecyclerViewItem> uploadToDriveRecyclerViewItemList;
    private ArrayList<RecyclerViewItem> deleteOnDriveRecyclerViewItemList;
    private RecyclerView uploadToDriveRecyclerView;
    private RecyclerView deleteOnDriveRecyclerView;

    public static HashMap<DriveFile, HashSet<LocalFile>> uploadToDriveFiles;
    public static HashMap<DriveFile, HashSet<DriveFile>> deleteOnDriveFiles;
    public static HashMap<LocalFile, HashSet<LocalFile>> createFolderAndUploadToDriveFiles;
    public static HashMap<DriveFile, HashSet<DriveFile>> deleteDriveFolderAndFiles;

    private TextView noneUploadDriveTextView;
    private TextView noneDeleteDriveTextView;
    private Button startPushButton;

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

        uploadToDriveRecyclerView = findViewById(R.id.uploadFromDriveRecyclerView);
        uploadToDriveRecyclerView.setFocusable(false);

        deleteOnDriveRecyclerView = findViewById(R.id.deleteFromDriveRecyclerView);
        deleteOnDriveRecyclerView.setFocusable(false);

        DividerItemDecoration decoration = new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL);
        decoration.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(this, R.drawable.list_divider)));

        uploadToDriveRecyclerView.addItemDecoration(decoration);
        deleteOnDriveRecyclerView.addItemDecoration(decoration);

        if (MainActivity.operationType == OperationType.PUSH) {
            HashMap<DriveFile, HashSet<DriveFile>> driveFolderFiles;
            HashMap<LocalFile, HashSet<LocalFile>> localFolderFiles;

            uploadToDriveFiles = new HashMap<>();
            deleteOnDriveFiles = new HashMap<>();
            createFolderAndUploadToDriveFiles = new HashMap<>();
            deleteDriveFolderAndFiles = new HashMap<>();

            if (MainActivity.fetchHelper == null) {
                MainActivity.msgHelper.showToast("Fetch helper is null, please try again");
                return;
            }

            HashSet<DriveFile> driveFolders = MainActivity.fetchHelper.getDriveFolders();
            HashSet<LocalFile> localFolders = MainActivity.fetchHelper.getLocalFolders();

            HashSet<LocalFile> driveFoldersToCreate = SetOperationsHelper
                    .relativeComplement(localFolders, driveFolders);

            System.out.println("DRIVE FOLDERS TO CREATE");
            System.out.println(driveFoldersToCreate);

            HashSet<DriveFile> driveFoldersToDelete = SetOperationsHelper
                    .relativeComplement(driveFolders, localFolders);

            System.out.println("DRIVE FOLDERS TO DELETE");
            System.out.println(driveFoldersToDelete);

            driveFolderFiles = MainActivity.fetchHelper.getDriveFolderFiles();
            localFolderFiles = MainActivity.fetchHelper.getLocalFolderFiles();

            for (DriveFile driveFolder : driveFolderFiles.keySet()) {
                try {
                    if (!localFolderFiles.containsKey(driveFolder) ||
                        !driveFolderFiles.containsKey(driveFolder)) {
                        continue;
                    }

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
                    e.printStackTrace();
                }
            }

            uploadToDriveRecyclerViewItemList = new ArrayList<>();
            deleteOnDriveRecyclerViewItemList = new ArrayList<>();

            if (FetchHelper.getMapSize(deleteOnDriveFiles) == 0 && driveFoldersToDelete.size() == 0) {
                noneDeleteDriveTextView = findViewById(R.id.noneDeleteDriveTextView);
                noneDeleteDriveTextView.setVisibility(View.VISIBLE);
            }

            if (FetchHelper.getMapSize(uploadToDriveFiles) == 0 && driveFoldersToCreate.size() == 0) {
                noneUploadDriveTextView = findViewById(R.id.noneUploadDriveTextView);
                noneUploadDriveTextView.setVisibility(View.VISIBLE);
            }

            for (DriveFile folder : uploadToDriveFiles.keySet()) {
                if (uploadToDriveFiles.get(folder).size() == 0) {
                    continue;
                }
                uploadToDriveRecyclerViewItemList.add(new RecyclerViewItem(String.format("FOLDER: %s", folder.getAbsolutePath()), DriveType.FOLDER));
                for (LocalFile file : uploadToDriveFiles.get(folder)) {
                    uploadToDriveRecyclerViewItemList.add(new RecyclerViewItem(file.getName()));
                }
            }

            for (LocalFile localFolder : driveFoldersToCreate) {
                createFolderAndUploadToDriveFiles.put(localFolder, localFolderFiles.get(localFolder));
                uploadToDriveRecyclerViewItemList.add(new RecyclerViewItem(String.format("[CREATE] FOLDER: \"%s\"", localFolder.getAbsolutePath()), DriveType.FOLDER));
                System.out.println(String.format("[CREATE] %s", localFolder.getAbsolutePath()));
                for (LocalFile localFile : createFolderAndUploadToDriveFiles.get(localFolder)) {
                    System.out.println("UPLOAD WITH FOLDER " + localFile.getName());
                    uploadToDriveRecyclerViewItemList.add(new RecyclerViewItem(localFile.getName()));
                }
            }

            for (DriveFile folder : deleteOnDriveFiles.keySet()) {
                if (deleteOnDriveFiles.get(folder).size() == 0) {
                    continue;
                }
                deleteOnDriveRecyclerViewItemList.add(new RecyclerViewItem(String.format("FOLDER: %s", folder.getAbsolutePath()), DriveType.FOLDER));
                for (DriveFile file : deleteOnDriveFiles.get(folder)) {
                    deleteOnDriveRecyclerViewItemList.add(new RecyclerViewItem(file.getName()));
                }
            }

            for (DriveFile driveFolder : driveFoldersToDelete) {
                deleteDriveFolderAndFiles.put(driveFolder, driveFolderFiles.get(driveFolder));
                deleteOnDriveRecyclerViewItemList.add(new RecyclerViewItem(String.format("FOLDER: %s", driveFolder.getAbsolutePath()), DriveType.FOLDER));
                for (DriveFile file : deleteDriveFolderAndFiles.get(driveFolder)) {
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
        RecyclerViewAdapter uploadFromDriveAdapter = new RecyclerViewAdapter(uploadToDriveRecyclerViewItemList);
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