package com.example.android_google_drive_loader.Helpers;

import static com.example.android_google_drive_loader.MainActivity.msgHelper;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.documentfile.provider.DocumentFile;

import com.example.android_google_drive_loader.ConfirmPullActivity;
import com.example.android_google_drive_loader.ConfirmPushActivity;
import com.example.android_google_drive_loader.Enums.DriveType;
import com.example.android_google_drive_loader.Files.DriveFile;
import com.example.android_google_drive_loader.Files.LocalFile;
import com.example.android_google_drive_loader.MainActivity;
import com.example.android_google_drive_loader.R;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class GoogleDriveHelper {

    private final Executor executor;
    private Drive drive;
    private Context appContext;

    public static String SCOPE = DriveScopes.DRIVE;
    private final int FILE_PAGE_SIZE = 100;

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    public List<File> filterByName(List<File> list, String name) {
        List<File> res = new ArrayList<>();
        for(File file : list) {
            if (name.equals(file.getName())) {
                res.add(file);
            }
        }
        return res;
    }

    public GoogleDriveHelper(Context appContext, GoogleSignInAccount account, String appName) {
        GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(
                        appContext, Collections.singleton(SCOPE));

        credential.setSelectedAccount(account.getAccount());

        this.drive = new Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                new GsonFactory(),
                credential)
                .setApplicationName(appName)
                .build();

        this.appContext = appContext;
        this.executor = Executors.newSingleThreadExecutor();
    }

    public File getFileByName(String name, DriveType fileType) throws Exception {
        String query;

        if (fileType == DriveType.ANY) {
            query = "name = '" + name.replace("'", "\\'") + "' and trashed = false";
        }
        else {
            query = "name = '" + name.replace("'", "\\'") + "' and trashed = false and mimeType = 'application/vnd.google-apps.folder'";
        }

        FileList result = drive.files().list()
                .setFields("files(id, name)")
                .setQ(query).execute();

        List<File> files = filterByName(result.getFiles(), name);

        if (files.size() > 1) {
            throw msgHelper.getExceptionWithError("Duplicate folders " + name);
        }

        if (files.size() == 0) {
            throw msgHelper.getExceptionWithError("Folder " + name + " on the drive was not found");
        }

        return files.get(0);
    }

    public HashSet<DriveFile> getFolderFiles(DriveFile folderFile) throws Exception {
        String query = "'" + folderFile.getId() + "' in parents and mimeType != 'application/vnd.google-apps.folder' and trashed = false";

        Drive.Files.List request = drive.files().list()
                .setPageSize(FILE_PAGE_SIZE)
                .setFields("nextPageToken, files(id, name)")
                .setQ(query);

        HashSet<DriveFile> res = new HashSet<>();

        do {
            FileList files = request.execute();

            for (File curFile : files.getFiles()) {
                DriveFile driveFile = new DriveFile(curFile);
                if (res.contains(driveFile)) {
                    throw MainActivity.msgHelper.getExceptionWithError("Duplicate files in folder " + folderFile.getName());
                }
                res.add(driveFile);
            }

            request.setPageToken(files.getNextPageToken());
        }
        while (request.getPageToken() != null && request.getPageToken().length() > 0);

        return res;
    }

    public HashSet<DriveFile> getNestedFolders(File folderFile) throws Exception {
        String query = "'" + folderFile.getId() + "' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false";

        Drive.Files.List request = drive.files().list()
                .setPageSize(FILE_PAGE_SIZE)
                .setFields("nextPageToken, files(id, name)")
                .setQ(query);

        HashSet<File> resultSet = new HashSet<>();

        do {
            FileList nestedFolders = request.execute();

            for (File curFolder : nestedFolders.getFiles()) {
                if (resultSet.contains(curFolder)) {
                    throw MainActivity.msgHelper.getExceptionWithError("Duplicate folders in folder " + folderFile.getName());
                }
                resultSet.add(curFolder);
            }

            request.setPageToken(nestedFolders.getNextPageToken());
        }
        while (request.getPageToken() != null && request.getPageToken().length() > 0);

        HashSet<DriveFile> folderSet = new HashSet<>();

        for (File folder : resultSet) {
            DriveFile driveFile = new DriveFile(folder);
            if (folderSet.contains(driveFile)) {
                throw MainActivity.msgHelper.getExceptionWithError("Duplicate folders in folder " + folderFile.getName());
            }
            folderSet.add(driveFile);
            folderSet.addAll(getNestedFolders(folder));
        }

        return folderSet;
    }

    public Boolean uploadToDriveFile(LocalFile file, DriveFile driveFolder) throws Exception {
        DocumentFile localDocumentFile = file.getFile();

        File metadata = new File()
                .setParents(Arrays.asList(driveFolder.getId()))
                .setName(file.getName());

        java.io.File localFile = LocalFileHelper.getFileFromDocumentFile(localDocumentFile);

        InputStreamContent mediaContent =
                new InputStreamContent(null,
                        new BufferedInputStream(new FileInputStream(localFile)));
        mediaContent.setLength(localFile.length());

        Drive.Files.Create request = drive.files().create(metadata, mediaContent).setFields("id, name");

        File resFile = request.execute();

        return resFile != null;
    }

    public Boolean deleteDriveFile(DriveFile file) throws Exception {
        drive.files().delete(file.getId()).execute();

        return true;
    }

    public Boolean downloadDriveFile(LocalFile localFolder, DriveFile driveFile) throws Exception {
        DocumentFile folderDocumentFile = localFolder.getFile();

        try {

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            drive.files().get(driveFile.getId()).executeMediaAndDownloadTo(outputStream);

            DocumentFile downloadedFile = folderDocumentFile.createFile(null, driveFile.getName());

            OutputStream fileOutputStream = appContext.getContentResolver().openOutputStream(downloadedFile.getUri());

            outputStream.writeTo(fileOutputStream);

            outputStream.close();
        }
        catch (Exception e) {
            DocumentFile createdFile = folderDocumentFile.findFile(driveFile.getName());

            if (createdFile != null) {
                createdFile.delete();
            }

            throw e;
        }
        return true;
    }

    public Boolean deleteLocalFile(LocalFile localFolder,  LocalFile file) throws Exception {
        DocumentFile folderDocumentFile = localFolder.getFile();

        DocumentFile fileToDelete = folderDocumentFile.findFile(file.getName());

        if (fileToDelete == null) {
            throw msgHelper.getExceptionWithError("File to delete " + file.getName() + " was not found");
        }

        fileToDelete.delete();

        return true;
    }

    public Boolean testLocalAndDriveFolders(DocumentFile localFolder, String driveFolderName) throws Exception {
        FetchHelper helper = getFetcher(localFolder, driveFolderName);

        return helper.mapsAreEqual();
    }

    public FetchHelper getFetcher(DocumentFile localFolder, String driveFolderName) throws Exception {
        File rootFolder = getFileByName(driveFolderName, DriveType.FOLDER);

        HashSet<DriveFile> driveFolderSet = getNestedFolders(rootFolder);
        driveFolderSet.add(new DriveFile(rootFolder));

        HashMap<DriveFile, HashSet<DriveFile>> driveFoldersWithFiles = new HashMap<>();

        for (DriveFile folder : driveFolderSet) {
            driveFoldersWithFiles.put(folder, getFolderFiles(folder));
        }

        HashSet<LocalFile> localFolderSet = LocalFileHelper.getNestedFolders(localFolder);
        localFolderSet.add(new LocalFile(localFolder));

        if (!localFolderSet.equals(driveFolderSet)) {
            throw msgHelper.getExceptionWithError("Drive and local folders are not equal");
        }

        HashMap<LocalFile, HashSet<LocalFile>> localFolderWithFiles = new HashMap<>();

        for (LocalFile folder : localFolderSet) {
            localFolderWithFiles.put(folder, MainActivity.localHelper.getFolderFiles(folder));
        }

        return new FetchHelper(driveFoldersWithFiles, localFolderWithFiles);
    }

    public FetchHelper getFetcher(Activity activity, DocumentFile localFolder, String driveFolderName) throws Exception {
        TextView currentFetchOperationTextView = activity.findViewById(R.id.currentFetchOperationTextView);

        activity.runOnUiThread(() -> currentFetchOperationTextView.setText("Getting drive files..."));

        File rootFolder = getFileByName(driveFolderName, DriveType.FOLDER);

        HashSet<DriveFile> driveFolderSet = getNestedFolders(rootFolder);
        driveFolderSet.add(new DriveFile(rootFolder));

        System.out.println("NESTED FOLDERS");
        System.out.println(driveFolderSet);

        HashMap<DriveFile, HashSet<DriveFile>> driveFoldersWithFiles = new HashMap<>();

        for (DriveFile folder : driveFolderSet) {
            driveFoldersWithFiles.put(folder, getFolderFiles(folder));
        }

        System.out.println("ALL DRIVE FILES WITH FOLDERS");
        System.out.println(driveFoldersWithFiles);

        activity.runOnUiThread(() -> currentFetchOperationTextView.setText("Getting local files..."));

        System.out.println("ALL LOCAL FILES WITH FOLDERS");

        HashSet<LocalFile> localFolderSet = LocalFileHelper.getNestedFolders(localFolder);
        localFolderSet.add(new LocalFile(localFolder));

        if (!localFolderSet.equals(driveFolderSet)) {
            throw msgHelper.getExceptionWithError("Drive and local folders are not equal");
        }

        HashMap<LocalFile, HashSet<LocalFile>> localFolderWithFiles = new HashMap<>();

        for (LocalFile folder : localFolderSet) {
            localFolderWithFiles.put(folder, MainActivity.localHelper.getFolderFiles(folder));
        }

        System.out.println(localFolderWithFiles);

        return new FetchHelper(driveFoldersWithFiles, localFolderWithFiles);
    }

    public Task<FetchHelper> fetchData(Activity activity, DocumentFile localFolder, String driveFolderName) {
        return Tasks.call(executor, () -> {

            if (!isNetworkAvailable()) {
                throw msgHelper.getExceptionWithError("Can't connect to network");
            }

            return getFetcher(activity, localFolder, driveFolderName);
        });
    }

    public Task<Boolean> push(Activity activity, DocumentFile rootFolder, String driveFolderName) {
        return Tasks.call(executor, () -> {

            TextView currentOperationNameTextView = activity.findViewById(R.id.currentOperationNameTextView);
            TextView currentOperationFileNameTextView = activity.findViewById(R.id.currentOperationFileNameTextView);
            ProgressBar loadingProgressBar = activity.findViewById(R.id.loadingProgressBar);
            TextView loadingStatusTextView = activity.findViewById(R.id.loadingStatusTextView);
            Button backButton = activity.findViewById(R.id.backMainScreenButton);
            TextView warningTextView = activity.findViewById(R.id.warningTextView);

            if (!isNetworkAvailable()) {
                throw msgHelper.getExceptionWithError("Can't connect to network");
            }

            HashMap<DriveFile, HashSet<LocalFile>> uploadToDriveFiles = ConfirmPushActivity.uploadToDriveFiles;
            HashMap<DriveFile, HashSet<DriveFile>> deleteOnDriveFiles = ConfirmPushActivity.deleteOnDriveFiles;

            final Integer totalSize = FetchHelper.getMapSize(deleteOnDriveFiles) + FetchHelper.getMapSize(uploadToDriveFiles);
            Integer currentCompleted = 0;

            System.out.println("UPLOAD:");
            System.out.println(uploadToDriveFiles);

            for (DriveFile folder : uploadToDriveFiles.keySet()) {
                for (LocalFile file : uploadToDriveFiles.get(folder)) {
                    activity.runOnUiThread(() -> {
                        currentOperationNameTextView.setText("Uploading to drive");
                        currentOperationFileNameTextView.setText(file.getName());
                    });

                    if (uploadToDriveFile(file, folder)) {
                        System.out.println(file.getName() + " : " + "OK");

                        currentCompleted++;
                        int percent = Math.round((float)Math.ceil((float)currentCompleted / totalSize * 100));

                        activity.runOnUiThread(() -> {
                            loadingProgressBar.setProgress(percent);
                            loadingStatusTextView.setText("Loading " + percent + "%");
                        });
                    }
                    else {
                        System.out.println(file.getName() + " : FAIL");
                    }
                }
            }

            System.out.println("DELETE:");
            System.out.println(deleteOnDriveFiles);

            for (DriveFile folder : deleteOnDriveFiles.keySet()) {
                for (DriveFile file : deleteOnDriveFiles.get(folder)) {
                    activity.runOnUiThread(() -> {
                        currentOperationNameTextView.setText("Deleting from drive");
                        currentOperationFileNameTextView.setText(file.getName());
                    });

                    if (deleteDriveFile(file)) {
                        System.out.println("DELETE: " + file.getName() + " OK");

                        currentCompleted++;
                        int percent = Math.round((float)Math.ceil((float)currentCompleted / totalSize * 100));

                        activity.runOnUiThread(() -> {
                            loadingProgressBar.setProgress(percent);
                            loadingStatusTextView.setText("Loading " + percent + "%");
                        });
                    }
                    else {
                        System.out.println("DELETE: " + file.getName() + " FAIL");
                    }
                }
            }

            activity.runOnUiThread(() -> {
                currentOperationNameTextView.setText("Comparing folders...");
                currentOperationFileNameTextView.setText("");
            });

            Boolean result = testLocalAndDriveFolders(rootFolder, driveFolderName);

            activity.runOnUiThread(() -> {
                if (result) {
                    loadingStatusTextView.setText("SUCCESS");
                    loadingStatusTextView.setTextColor(Color.parseColor("#00ff00"));
                }
                else {
                    loadingStatusTextView.setText("FAIL (folders are not equal)");
                    loadingStatusTextView.setTextColor(Color.parseColor("#ff0000"));
                }
                backButton.setVisibility(View.VISIBLE);
                warningTextView.setVisibility(View.INVISIBLE);
                currentOperationNameTextView.setText("DONE");
                currentOperationFileNameTextView.setVisibility(View.INVISIBLE);
            });

            return result;
        });
    }

    public Task<Boolean> pull(Activity activity, DocumentFile rootFolder, String driveFolderName) {
        return Tasks.call(executor, () -> {

            TextView currentOperationNameTextView = activity.findViewById(R.id.currentOperationNameTextView);
            TextView currentOperationFileNameTextView = activity.findViewById(R.id.currentOperationFileNameTextView);
            ProgressBar loadingProgressBar = activity.findViewById(R.id.loadingProgressBar);
            TextView loadingStatusTextView = activity.findViewById(R.id.loadingStatusTextView);
            Button backButton = activity.findViewById(R.id.backMainScreenButton);
            TextView warningTextView = activity.findViewById(R.id.warningTextView);

            if (!isNetworkAvailable()) {
                throw msgHelper.getExceptionWithError("Can't connect to network");
            }

            HashMap<LocalFile, HashSet<DriveFile>> downloadFromDriveFiles = ConfirmPullActivity.downloadFromDriveFiles;
            HashMap<LocalFile, HashSet<LocalFile>> deleteInLocalFiles = ConfirmPullActivity.deleteInLocalFiles;

            final Integer totalSize = FetchHelper.getMapSize(downloadFromDriveFiles) + FetchHelper.getMapSize(deleteInLocalFiles);
            Integer currentCompleted = 0;

            System.out.println("DOWNLOAD:");
            System.out.println(downloadFromDriveFiles);

            for (LocalFile folder : downloadFromDriveFiles.keySet()) {
                for (DriveFile file : downloadFromDriveFiles.get(folder)) {
                    String name = file.getName();
                    activity.runOnUiThread(() -> {
                        currentOperationNameTextView.setText("Downloading from drive");
                        currentOperationFileNameTextView.setText(name);
                    });

                    if (downloadDriveFile(folder, file)) {
                        System.out.println("DOWNLOAD: " + name + " OK");

                        currentCompleted++;
                        int percent = Math.round((float)Math.ceil((float)currentCompleted / totalSize * 100));

                        activity.runOnUiThread(() -> {
                            loadingProgressBar.setProgress(percent);
                            loadingStatusTextView.setText("Loading " + percent + "%");
                        });
                    }
                    else {
                        System.out.println("DOWNLOAD: " + name + " FAIL");
                    }
                }
            }

            System.out.println("DELETE:");
            System.out.println(deleteInLocalFiles);

            for (LocalFile folder : deleteInLocalFiles.keySet()) {
                for (LocalFile file : deleteInLocalFiles.get(folder)) {
                    String name = file.getName();

                    activity.runOnUiThread(() -> {
                        currentOperationNameTextView.setText("Deleting in local storage");
                        currentOperationFileNameTextView.setText(name);
                    });

                    if (deleteLocalFile(folder, file)) {
                        System.out.println("DELETE: " + name + " OK");

                        currentCompleted++;
                        int percent = Math.round((float)Math.ceil((float)currentCompleted / totalSize * 100));

                        activity.runOnUiThread(() -> {
                            loadingProgressBar.setProgress(percent);
                            loadingStatusTextView.setText("Loading " + percent + "%");
                        });
                    }
                    else {
                        System.out.println("DELETE: " + name + " FAIL");
                    }
                }
            }

            activity.runOnUiThread(() -> {
                currentOperationNameTextView.setText("Comparing folders...");
                currentOperationFileNameTextView.setText("");
            });

            Boolean result = testLocalAndDriveFolders(rootFolder, driveFolderName);

            activity.runOnUiThread(() -> {
                if (result) {
                    loadingStatusTextView.setText("SUCCESS");
                    loadingStatusTextView.setTextColor(Color.parseColor("#00ff00"));
                }
                else {
                    loadingStatusTextView.setText("FAIL (folders are not equal)");
                    loadingStatusTextView.setTextColor(Color.parseColor("#ff0000"));
                }
                backButton.setVisibility(View.VISIBLE);
                warningTextView.setVisibility(View.INVISIBLE);
                currentOperationNameTextView.setText("DONE");
                currentOperationFileNameTextView.setVisibility(View.INVISIBLE);
            });

            return result;
        });
    }
}
