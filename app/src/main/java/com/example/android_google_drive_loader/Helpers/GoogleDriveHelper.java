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
import com.example.android_google_drive_loader.MainActivity;
import com.example.android_google_drive_loader.Models.DriveFile;
import com.example.android_google_drive_loader.Models.LocalFile;
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
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class GoogleDriveHelper {

    public static final String GOOGLE_DRIVE_FOLDER_TYPE = "application/vnd.google-apps.folder";
    private final Executor executor;
    private final Drive drive;
    private final Context appContext;

    public static String SCOPE = DriveScopes.DRIVE;
    private final int FILE_PAGE_SIZE = 1000;

    public static String invalidCharacters = "/\\:*?<>\"";

    public boolean isValidName(String name) {
        for (int i = 0; i < invalidCharacters.length(); i++) {
            if (name.contains(String.valueOf(invalidCharacters.charAt(i)))) {
                return false;
            }
        }
        return true;
    }

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

    public boolean driveFileExists(DriveFile driveFile) {
        try {
            File result = drive.files().get(driveFile.getId()).execute();
            return result != null;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public File getFileByName(String name, DriveType fileType) throws Exception {
        String query;

        if (fileType == DriveType.ANY) {
            query = "name = '" + name.replace("'", "\\'") + "' and trashed = false";
        }
        else {
            query = "name = '" + name.replace("'", "\\'") + "' and trashed = false and mimeType = '" + GOOGLE_DRIVE_FOLDER_TYPE + "'";
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

    public HashSet<DriveFile> getDriveFoldersAndFiles(DriveFile rootFile, DriveFile parent) throws Exception {
        if (!isValidName(rootFile.getName())) {
            throw msgHelper.getExceptionWithError(String.format("Invalid folder name %s", rootFile.getAbsolutePath()));
        }

        String query = "'" + rootFile.getId() + "' in parents and trashed = false";

        Drive.Files.List request = drive.files().list()
                .setPageSize(FILE_PAGE_SIZE)
                .setFields("nextPageToken, files(id, name, size, mimeType)")
                .setQ(query);

        HashSet<File> foldersAndFilesResponseSet = new HashSet<>();

        do {
            FileList foldersAndFiles = request.execute();

            foldersAndFilesResponseSet.addAll(foldersAndFiles.getFiles());

            request.setPageToken(foldersAndFiles.getNextPageToken());
        }
        while (request.getPageToken() != null && request.getPageToken().length() > 0);

        HashSet<DriveFile> folderSet = new HashSet<>();

        if (parent == null) {
            folderSet.add(rootFile);
        }

        for (File file : foldersAndFilesResponseSet) {
            DriveFile driveFile = new DriveFile(file, rootFile);

            if (file.getMimeType().equals(GOOGLE_DRIVE_FOLDER_TYPE)) {
                if (folderSet.contains(driveFile)) {
                    throw MainActivity.msgHelper.getExceptionWithError("Duplicate folders " + driveFile.getAbsolutePath());
                }

                if (!isValidName(driveFile.getName())) {
                    throw msgHelper.getExceptionWithError(String.format("Invalid drive folder name %s\nInvalid characters %s",
                            driveFile.getAbsolutePath(),
                            invalidCharacters));
                }

                folderSet.add(driveFile);
                folderSet.addAll(getDriveFoldersAndFiles(driveFile, rootFile));
            }
            else {
                if (!isValidName(driveFile.getName())) {
                    throw msgHelper.getExceptionWithError(String.format("Invalid drive file name %s\nInvalid characters %s",
                            driveFile.getAbsolutePath(),
                            invalidCharacters));
                }
            }
        }

        return folderSet;
    }

    public Boolean uploadToDriveFile(LocalFile file, DriveFile driveFolder) throws Exception {
        DocumentFile localDocumentFile = file.getFile();

        File metadata = new File()
                .setParents(Collections.singletonList(driveFolder.getId()))
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

    public Boolean deleteDriveFile(DriveFile file)  {
        try {
            drive.files().delete(file.getId()).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public DriveFile createDriveFolder(DriveFile rootFolder, String name) throws Exception {
        File metadata = new File();
        metadata.setParents(Collections.singletonList(rootFolder.getId()));
        metadata.setMimeType(GOOGLE_DRIVE_FOLDER_TYPE);
        metadata.setName(name);

        File newFolderFile = drive.files()
                .create(metadata).setFields("id, name")
                .execute();

        return new DriveFile(newFolderFile, rootFolder);
    }

    public HashSet<DriveFile> createDriveFoldersFromLocalFolders(Set<LocalFile> foldersToCreate, DriveFile rootFolder) throws Exception {
        HashSet<DriveFile> newFolders = new HashSet<>();

        for (LocalFile localFolder : foldersToCreate) {
            String dirName = localFolder.getName();

            if (localFolder.getParent().getAbsolutePath().equals(rootFolder.getAbsolutePath())) {
                DriveFile newFolder = createDriveFolder(rootFolder, dirName);

                if (newFolder == null || newFolder.getName() == null) {
                    throw new Exception("Couldn't create new folder");
                }

                System.out.println("CREATED NEW DRIVE FOLDER " + newFolder.getAbsolutePath());

                newFolders.addAll(createDriveFoldersFromLocalFolders(foldersToCreate, newFolder));
                newFolders.add(newFolder);
            }
        }

        for (DriveFile driveFolder : rootFolder.getChildFolders()) {
            if (!newFolders.contains(driveFolder)) {
                newFolders.addAll(createDriveFoldersFromLocalFolders(foldersToCreate, driveFolder));
            }
        }

        return newFolders;
    }

    public HashSet<LocalFile> createLocalFoldersFromDriveFolders(Set<DriveFile> foldersToCreate, LocalFile rootFolder) throws Exception {
        HashSet<LocalFile> newFolders = new HashSet<>();

        for (DriveFile driveFolder : foldersToCreate) {
            String dirName = driveFolder.getName();

            if (driveFolder.getParent().getAbsolutePath().equals(rootFolder.getAbsolutePath()) &&
                rootFolder.getFile().findFile(dirName) == null) {

                DocumentFile newFolder = rootFolder.getFile().createDirectory(dirName);

                if (newFolder == null || newFolder.getName() == null) {
                    throw new Exception("Couldn't create new local folder " + dirName);
                }

                System.out.println("CREATED NEW LOCAL FOLDER " + newFolder.getName());

                LocalFile newLocalFolder = new LocalFile(newFolder, rootFolder);
                newFolders.addAll(createLocalFoldersFromDriveFolders(foldersToCreate, newLocalFolder));
                newFolders.add(newLocalFolder);
            }
        }

        for (LocalFile currentFolder : rootFolder.getChildFolders()) {
            if (!newFolders.contains(currentFolder)) {
                newFolders.addAll(createLocalFoldersFromDriveFolders(foldersToCreate, currentFolder));
            }
        }

        return newFolders;
    }

    public Boolean testLocalAndDriveFolders(DocumentFile localFolder, String driveFolderName) throws Exception {
        FetchHelper helper = getFetcher(localFolder, driveFolderName);

        return helper.mapsAreEqual();
    }

    public FetchHelper getFetcher(DocumentFile localFolder, String driveFolderName) throws Exception {
        LocalFileHelper localHelper = new LocalFileHelper(appContext);
        File rootFolder = getFileByName(driveFolderName, DriveType.FOLDER);

        DriveFile driveRootFolder = new DriveFile(rootFolder, null);
        HashSet<DriveFile> driveFolderSet = getDriveFoldersAndFiles(driveRootFolder, null);

        HashMap<DriveFile, HashSet<DriveFile>> driveFoldersWithFiles = new HashMap<>();

        for (DriveFile folder : driveFolderSet) {
            driveFoldersWithFiles.put(folder, folder.getChildFiles());
        }

        LocalFile localRootFolder = new LocalFile(localFolder, null);
        HashSet<LocalFile> localFolderSet = localHelper.getLocalFilesAndFolders(localRootFolder, null);

        if (!localFolderSet.equals(driveFolderSet)) {
            throw msgHelper.getExceptionWithError("Drive and local folders are not equal");
        }

        HashMap<LocalFile, HashSet<LocalFile>> localFolderWithFiles = new HashMap<>();

        for (LocalFile folder : localFolderSet) {
            localFolderWithFiles.put(folder, folder.getChildFiles());
        }

        return new FetchHelper(driveFoldersWithFiles,
                localFolderWithFiles,
                driveFolderSet,
                localFolderSet,
                driveRootFolder,
                localRootFolder);
    }

    public FetchHelper getFetcher(Activity activity, DocumentFile localFolder, String driveFolderName) throws Exception {
        LocalFileHelper localHelper = new LocalFileHelper(appContext);
        TextView currentFetchOperationTextView = activity.findViewById(R.id.currentFetchOperationTextView);

        activity.runOnUiThread(() -> currentFetchOperationTextView.setText("Getting drive files..."));

        File rootFolder = getFileByName(driveFolderName, DriveType.FOLDER);

        DriveFile driveRootFolder = new DriveFile(rootFolder, null);
        HashSet<DriveFile> driveFolderSet = getDriveFoldersAndFiles(driveRootFolder, null);

        System.out.println("NESTED DRIVE FOLDERS");
        System.out.println(driveFolderSet);

        HashMap<DriveFile, HashSet<DriveFile>> driveFoldersWithFiles = new HashMap<>();

        for (DriveFile folder : driveFolderSet) {
            driveFoldersWithFiles.put(folder, folder.getChildFiles());
        }

        System.out.println("ALL DRIVE FILES WITH FOLDERS");
        System.out.println(driveFoldersWithFiles);

        activity.runOnUiThread(() -> currentFetchOperationTextView.setText("Getting local files..."));

        LocalFile localRootFolder = new LocalFile(localFolder, null);
        HashSet<LocalFile> localFolderSet = localHelper.getLocalFilesAndFolders(localRootFolder, null);

        System.out.println("NESTED LOCAL FOLDERS");
        System.out.println(localFolderSet);

        HashMap<LocalFile, HashSet<LocalFile>> localFolderWithFiles = new HashMap<>();

        for (LocalFile folder : localFolderSet) {
            localFolderWithFiles.put(folder, folder.getChildFiles());
        }

        System.out.println("ALL LOCAL FILES WITH FOLDERS");
        System.out.println(localFolderWithFiles);

        return new FetchHelper(driveFoldersWithFiles,
                localFolderWithFiles,
                driveFolderSet,
                localFolderSet,
                driveRootFolder,
                localRootFolder);
    }

    public Task<FetchHelper> fetchData(Activity activity, DocumentFile localFolder, String driveFolderName) {
        return Tasks.call(executor, () -> {

            if (!isNetworkAvailable()) {
                throw msgHelper.getExceptionWithError("Can't connect to network");
            }

            return getFetcher(activity, localFolder, driveFolderName);
        });
    }

    public Task<Boolean> push(Activity activity, LocalFile localRootFolder, DriveFile driveRootFolder) {
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

            HashMap<LocalFile, HashSet<LocalFile>> createFolderAndUploadToDriveFiles = ConfirmPushActivity.createFolderAndUploadToDriveFiles;
            HashMap<DriveFile, HashSet<LocalFile>> newUploadDriveFoldersAndFiles = new HashMap<>();

            HashMap<DriveFile, HashSet<DriveFile>> deleteDriveFolderAndFiles = ConfirmPushActivity.deleteDriveFolderAndFiles;

            long totalSizeInBytes = ConfirmPushActivity.totalSizeInBytes;
            long finalTotalSizeInBytes1 = totalSizeInBytes;
            activity.runOnUiThread(() -> {
                loadingStatusTextView.setText(String.format("Loading %d%%\n(%s to process)",
                        0,
                        SizeHelper.convertToStringRepresentation(finalTotalSizeInBytes1)));
            });

            System.out.println("INITIAL TOTAL SIZE IN BYTES " + totalSizeInBytes);

            final int totalSize = FetchHelper.getMapSize(deleteOnDriveFiles) +
                    FetchHelper.getMapSize(uploadToDriveFiles) +
                    FetchHelper.getMapSize(createFolderAndUploadToDriveFiles) +
                    FetchHelper.getMapSize(deleteDriveFolderAndFiles);

            int currentCompleted = 0;

            System.out.println("DELETE:");
            System.out.println(deleteOnDriveFiles);

            for (DriveFile folder : deleteOnDriveFiles.keySet()) {
                for (DriveFile file : deleteOnDriveFiles.get(folder)) {
                    activity.runOnUiThread(() -> {
                        currentOperationNameTextView.setText(String.format("Deleting from drive folder %s", folder.getAbsolutePath()));
                        currentOperationFileNameTextView.setText(file.getName());
                    });

                    if (deleteDriveFile(file)) {
                        System.out.println("DELETE: " + file.getName() + " OK");

                        currentCompleted++;
                        int percent = Math.round((float)Math.ceil((float)currentCompleted / totalSize * 100));

                        totalSizeInBytes -= file.getSizeInBytes();

                        long finalTotalSizeInBytes = totalSizeInBytes;
                        activity.runOnUiThread(() -> {
                            loadingProgressBar.setProgress(percent);
                            loadingStatusTextView.setText(String.format("Loading %d%%\n(%s to process)",
                                    percent,
                                    SizeHelper.convertToStringRepresentation(finalTotalSizeInBytes)));
                        });
                    }
                    else {
                        System.out.println("DELETE: " + file.getName() + " FAIL");
                    }
                }
            }

            System.out.println("DELETE WITH FOLDERS " + deleteDriveFolderAndFiles);

            for (DriveFile driveFolder : deleteDriveFolderAndFiles.keySet()) {
                activity.runOnUiThread(() -> {
                    currentOperationNameTextView.setText(String.format("Deleting drive folder %s", driveFolder.getAbsolutePath()));
                    currentOperationFileNameTextView.setText("");
                });

                deleteDriveFile(driveFolder);

                currentCompleted += deleteDriveFolderAndFiles.get(driveFolder).size();
                int percent = Math.round((float)Math.ceil((float)currentCompleted / totalSize * 100));

                totalSizeInBytes -= FetchHelper.getFolderSize(deleteDriveFolderAndFiles.get(driveFolder));

                long finalTotalSizeInBytes = totalSizeInBytes;
                activity.runOnUiThread(() -> {
                    loadingProgressBar.setProgress(percent);
                    loadingStatusTextView.setText(String.format("Loading %d%%\n(%s to process)",
                            percent,
                            SizeHelper.convertToStringRepresentation(finalTotalSizeInBytes)));
                });
            }

            System.out.println("UPLOAD:");
            System.out.println(uploadToDriveFiles);

            for (DriveFile folder : uploadToDriveFiles.keySet()) {
                for (LocalFile file : uploadToDriveFiles.get(folder)) {
                    activity.runOnUiThread(() -> {
                        currentOperationNameTextView.setText(String.format("Uploading to drive folder %s", folder.getAbsolutePath()));
                        currentOperationFileNameTextView.setText(file.getName());
                    });

                    if (uploadToDriveFile(file, folder)) {
                        System.out.println(file.getName() + " : " + "OK");

                        currentCompleted++;
                        int percent = Math.round((float)Math.ceil((float)currentCompleted / totalSize * 100));

                        totalSizeInBytes -= file.getSizeInBytes();

                        long finalTotalSizeInBytes = totalSizeInBytes;
                        activity.runOnUiThread(() -> {
                            loadingProgressBar.setProgress(percent);
                            loadingStatusTextView.setText(String.format("Loading %d%%\n(%s to process)",
                                    percent,
                                    SizeHelper.convertToStringRepresentation(finalTotalSizeInBytes)));
                        });
                    }
                    else {
                        System.out.println(file.getName() + " : FAIL");
                    }
                }
            }

            activity.runOnUiThread(() -> {
                currentOperationNameTextView.setText("Creating drive folders...");
                currentOperationFileNameTextView.setText("");
            });

            HashSet<DriveFile> newFolders =
                    createDriveFoldersFromLocalFolders(createFolderAndUploadToDriveFiles.keySet(),
                            driveRootFolder);

            System.out.println("NEW DRIVE FOLDERS:");
            System.out.println(newFolders);

            for (DriveFile newFolder : newFolders) {
                newUploadDriveFoldersAndFiles.put(newFolder, createFolderAndUploadToDriveFiles.get(newFolder));
            }

            for (DriveFile driveFolder : newUploadDriveFoldersAndFiles.keySet()) {
                for (LocalFile localFile : newUploadDriveFoldersAndFiles.get(driveFolder)) {
                    String name = localFile.getName();
                    activity.runOnUiThread(() -> {
                        currentOperationNameTextView.setText(String.format("Uploading to new drive folder %s", driveFolder.getAbsolutePath()));
                        currentOperationFileNameTextView.setText(name);
                    });

                    if (uploadToDriveFile(localFile, driveFolder)) {
                        System.out.println("UPLOAD: " + name + " OK");

                        currentCompleted++;
                        int percent = Math.round((float)Math.ceil((float)currentCompleted / totalSize * 100));

                        totalSizeInBytes -= localFile.getSizeInBytes();

                        long finalTotalSizeInBytes = totalSizeInBytes;
                        activity.runOnUiThread(() -> {
                            loadingProgressBar.setProgress(percent);
                            loadingStatusTextView.setText(String.format("Loading %d%%\n(%s to process)",
                                    percent,
                                    SizeHelper.convertToStringRepresentation(finalTotalSizeInBytes)));
                        });
                    }
                    else {
                        System.out.println("UPLOAD: " + name + " FAIL");
                    }
                }
            }

            activity.runOnUiThread(() -> {
                currentOperationNameTextView.setText("Comparing folders...");
                currentOperationFileNameTextView.setText("");
            });

            Boolean result = testLocalAndDriveFolders(localRootFolder.getFile(), driveRootFolder.getName());

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

    public Task<Boolean> pull(Activity activity, LocalFile localRootFolder, DriveFile driveRootFolder) {
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

            HashMap<DriveFile, HashSet<DriveFile>> createFolderAndDownloadFromDriveFiles = ConfirmPullActivity.createFolderAndDownloadFromDriveFiles;
            HashMap<LocalFile, HashSet<DriveFile>> downloadToNewFoldersDriveFiles = new HashMap<>();

            HashMap<LocalFile, HashSet<LocalFile>> deleteLocalFolderAndFiles = ConfirmPullActivity.deleteLocalFolderAndFiles;

            long totalSizeInBytes = ConfirmPullActivity.totalSizeInBytes;
            long finalTotalSizeInBytes1 = totalSizeInBytes;
            activity.runOnUiThread(() -> {
                loadingStatusTextView.setText(String.format("Loading %d%%\n(%s to process)",
                        0,
                        SizeHelper.convertToStringRepresentation(finalTotalSizeInBytes1)));
            });

            final int totalSize = FetchHelper.getMapSize(downloadFromDriveFiles) +
                    FetchHelper.getMapSize(deleteInLocalFiles) +
                    FetchHelper.getMapSize(createFolderAndDownloadFromDriveFiles) +
                    FetchHelper.getMapSize(deleteLocalFolderAndFiles);

            int currentCompleted = 0;

            System.out.println("DELETE:");
            System.out.println(deleteInLocalFiles);

            for (LocalFile folder : deleteInLocalFiles.keySet()) {
                for (LocalFile file : deleteInLocalFiles.get(folder)) {
                    String name = file.getName();

                    activity.runOnUiThread(() -> {
                        currentOperationNameTextView.setText(String.format("Deleting in local folder %s", folder.getAbsolutePath()));
                        currentOperationFileNameTextView.setText(name);
                    });

                    if (deleteLocalFile(folder, file)) {
                        System.out.println("DELETE: " + name + " OK");

                        currentCompleted++;
                        int percent = Math.round((float)Math.ceil((float)currentCompleted / totalSize * 100));

                        totalSizeInBytes -= file.getSizeInBytes();
                        System.out.println(totalSizeInBytes);

                        long finalTotalSizeInBytes = totalSizeInBytes;
                        activity.runOnUiThread(() -> {
                            loadingProgressBar.setProgress(percent);
                            loadingStatusTextView.setText(String.format("Loading %d%%\n(%s to process)",
                                    percent,
                                    SizeHelper.convertToStringRepresentation(finalTotalSizeInBytes)));
                        });
                    }
                    else {
                        System.out.println("DELETE: " + name + " FAIL");
                    }
                }
            }

            System.out.println("DELETE WITH FOLDERS" + deleteLocalFolderAndFiles);

            for (LocalFile folder : deleteLocalFolderAndFiles.keySet()) {
                activity.runOnUiThread(() -> {
                    currentOperationNameTextView.setText(String.format("Deleting local folder %s", folder));
                    currentOperationFileNameTextView.setText("");
                });

                if (folder.getFile().exists()) {
                    folder.getFile().delete();
                }

                currentCompleted += deleteLocalFolderAndFiles.get(folder).size();
                int percent = Math.round((float)Math.ceil((float)currentCompleted / totalSize * 100));

                totalSizeInBytes -= FetchHelper.getFolderSize(deleteLocalFolderAndFiles.get(folder));

                long finalTotalSizeInBytes = totalSizeInBytes;
                activity.runOnUiThread(() -> {
                    loadingProgressBar.setProgress(percent);
                    loadingStatusTextView.setText(String.format("Loading %d%%\n(%s to process)",
                            percent,
                            SizeHelper.convertToStringRepresentation(finalTotalSizeInBytes)));
                });
            }

            System.out.println("DOWNLOAD:");
            System.out.println(downloadFromDriveFiles);

            for (LocalFile folder : downloadFromDriveFiles.keySet()) {
                for (DriveFile file : downloadFromDriveFiles.get(folder)) {
                    String name = file.getName();
                    activity.runOnUiThread(() -> {
                        currentOperationNameTextView.setText(String.format("Downloading to local folder %s", folder));
                        currentOperationFileNameTextView.setText(name);
                    });

                    if (downloadDriveFile(folder, file)) {
                        System.out.println("DOWNLOAD: " + name + " OK");

                        currentCompleted++;
                        int percent = Math.round((float)Math.ceil((float)currentCompleted / totalSize * 100));

                        totalSizeInBytes -= file.getSizeInBytes();

                        long finalTotalSizeInBytes = totalSizeInBytes;
                        activity.runOnUiThread(() -> {
                            loadingProgressBar.setProgress(percent);
                            loadingStatusTextView.setText(String.format("Loading %d%%\n(%s to process)",
                                    percent,
                                    SizeHelper.convertToStringRepresentation(finalTotalSizeInBytes)));
                        });
                    }
                    else {
                        System.out.println("DOWNLOAD: " + name + " FAIL");
                    }
                }
            }

            System.out.println("DOWNLOAD AND CREATE FOLDER:");
            System.out.println(createFolderAndDownloadFromDriveFiles);

            activity.runOnUiThread(() -> {
                currentOperationNameTextView.setText("Creating local folders...");
                currentOperationFileNameTextView.setText("");
            });

            HashSet<LocalFile> newFolders =
                    createLocalFoldersFromDriveFolders(createFolderAndDownloadFromDriveFiles.keySet(), localRootFolder);

            System.out.println("NEW LOCAL FOLDERS:");
            System.out.println(newFolders);

            for (LocalFile localFolder : newFolders) {
                downloadToNewFoldersDriveFiles.put(localFolder, createFolderAndDownloadFromDriveFiles.get(localFolder));
            }

            for (LocalFile localFolder : downloadToNewFoldersDriveFiles.keySet()) {
                for (DriveFile file : downloadToNewFoldersDriveFiles.get(localFolder)) {
                    String name = file.getName();
                    activity.runOnUiThread(() -> {
                        currentOperationNameTextView.setText(String.format("Downloading to new local folder %s", localFolder.getAbsolutePath()));
                        currentOperationFileNameTextView.setText(name);
                    });

                    if (downloadDriveFile(localFolder, file)) {
                        System.out.println("DOWNLOAD: " + name + " OK");

                        currentCompleted++;
                        int percent = Math.round((float)Math.ceil((float)currentCompleted / totalSize * 100));

                        totalSizeInBytes -= file.getSizeInBytes();

                        long finalTotalSizeInBytes = totalSizeInBytes;
                        activity.runOnUiThread(() -> {
                            loadingProgressBar.setProgress(percent);
                            loadingStatusTextView.setText(String.format("Loading %d%%\n(%s to process)",
                                    percent,
                                    SizeHelper.convertToStringRepresentation(finalTotalSizeInBytes)));
                        });
                    }
                    else {
                        System.out.println("DOWNLOAD: " + name + " FAIL");
                    }
                }
            }

            activity.runOnUiThread(() -> {
                currentOperationNameTextView.setText("Comparing folders...");
                currentOperationFileNameTextView.setText("");
            });

            Boolean result = testLocalAndDriveFolders(localRootFolder.getFile(), driveRootFolder.getName());

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
