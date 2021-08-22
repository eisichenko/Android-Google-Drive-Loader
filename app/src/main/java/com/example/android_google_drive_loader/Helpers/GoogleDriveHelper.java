package com.example.android_google_drive_loader.Helpers;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import com.example.android_google_drive_loader.ConfirmPullActivity;
import com.example.android_google_drive_loader.ConfirmPushActivity;
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

import org.w3c.dom.Text;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

    public void showToast(String msg) {
        Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show();
    }

    public Exception getExceptionWithError(String msg) {
        return new Exception("ERROR: " + msg);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    public GoogleDriveHelper(Context appContext, GoogleSignInAccount account, String appName) {
        GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(
                        appContext, Collections.singleton(SCOPE));

        credential.setSelectedAccount(account.getAccount());

        com.google.api.services.drive.Drive googleDriveService =
                new com.google.api.services.drive.Drive.Builder(
                        AndroidHttp.newCompatibleTransport(),
                        new GsonFactory(),
                        credential)
                        .setApplicationName(appName)
                        .build();

        this.drive = googleDriveService;
        this.appContext = appContext;
        this.executor = Executors.newSingleThreadExecutor();
    }

    public String getIdByName(String name, DriveType fileType) throws Exception {
        String query;

        if (fileType == DriveType.ANY) {
            query = "name = '" + name + "' and trashed = false";
        }
        else {
            query = "name = '" + name + "' and trashed = false and mimeType = 'application/vnd.google-apps.folder'";
        }

        FileList result = drive.files().list()
                .setFields("files(id)")
                .setQ(query).execute();

        List<File> files = result.getFiles();

        if (files.size() > 1) {
            throw getExceptionWithError("Duplicate folders");
        }

        if (files.size() == 0) {
            throw getExceptionWithError("Folder on the drive was not found");
        }

        return files.get(0).getId();
    }

    public String getIdByNameInFolder(String name, DriveType fileType, String driveFolderId) throws Exception {
        String query;

        if (fileType == DriveType.ANY) {
            query = "'" + driveFolderId + "' in parents and name = '" + name + "' and trashed = false";
        }
        else {
            query = "'" + driveFolderId + "' in parents and name = '" + name + "' and trashed = false and mimeType = 'application/vnd.google-apps.folder'";
        }

        FileList result = drive.files().list()
                .setFields("files(id)")
                .setQ(query).execute();

        List<File> files = result.getFiles();

        if (files.size() > 1) {
            throw getExceptionWithError("Duplicate folders");
        }

        if (files.size() == 0) {
            throw getExceptionWithError("Folder on the drive was not found");
        }

        return files.get(0).getId();
    }

    public List<String> getFolderFileNames(String folderName) throws Exception {
        String folderId = getIdByName(folderName, DriveType.FOLDER);

        String query = "'" + folderId + "' in parents and mimeType != 'application/vnd.google-apps.folder' and trashed = false";

        Drive.Files.List request = drive.files().list()
                .setPageSize(FILE_PAGE_SIZE)
                .setFields("nextPageToken, files(id, name)")
                .setQ(query);

        List<String> result = new ArrayList<>();

        do {
            FileList files = request.execute();

            for (File file : files.getFiles()) {
                result.add(file.getName());
            }

            request.setPageToken(files.getNextPageToken());
        }
        while (request.getPageToken() != null && request.getPageToken().length() > 0);

        return result;
    }

    public Boolean uploadToDriveFile(DocumentFile localDirectory, String localFileName, String driveFolderName) throws Exception {

        File metadata = new File()
                .setParents(Arrays.asList(getIdByName(driveFolderName, DriveType.FOLDER)))
                .setName(localFileName);

        java.io.File localFile = LocalFileHelper.getFileInDirectoryByName(localDirectory, localFileName);

        InputStreamContent mediaContent =
                new InputStreamContent(null,
                        new BufferedInputStream(new FileInputStream(localFile)));
        mediaContent.setLength(localFile.length());

        Drive.Files.Create request = drive.files().create(metadata, mediaContent).setFields("id, name");

        File resFile = request.execute();

        return resFile != null;
    }

    public Boolean deleteDriveFile(String driveFolderName, String fileName) throws Exception {
        String folderId = getIdByName(driveFolderName, DriveType.FOLDER);
        String fileId = getIdByNameInFolder(fileName, DriveType.ANY, folderId);

        drive.files().delete(fileId).execute();

        return true;
    }

    public Boolean downloadDriveFile(DocumentFile localFolder, String driveFolderName, String fileName) throws Exception {
        String folderId = getIdByName(driveFolderName, DriveType.FOLDER);
        String fileId = getIdByNameInFolder(fileName, DriveType.ANY, folderId);

        DocumentFile downloadedFile = localFolder.createFile(null, fileName);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        drive.files().get(fileId).executeMediaAndDownloadTo(outputStream);

        OutputStream fileOutputStream = appContext.getContentResolver().openOutputStream(downloadedFile.getUri());

        outputStream.writeTo(fileOutputStream);

        outputStream.close();

        return true;
    }


    public Boolean deleteLocalFile(DocumentFile folder, String fileName) throws Exception {
        DocumentFile file = folder.findFile(fileName);

        if (file == null) {
            throw getExceptionWithError("File to delete was not found");
        }

        file.delete();

        return true;
    }

    public Boolean testLocalAndDriveFolders(DocumentFile localFolder, String driveFolderName) throws Exception {
        FetchHelper helper = getFetcher(localFolder, driveFolderName);

        return helper.getDriveFolderFileNamesSet().equals(helper.getLocalFolderFileNamesSet());
    }

    public FetchHelper getFetcher(DocumentFile localFolder, String driveFolderName) throws Exception {
        List<String> driveFolderFileNames = getFolderFileNames(driveFolderName);
        HashSet<String> driveFolderFileNamesSet = new HashSet<>(driveFolderFileNames);

        if (driveFolderFileNamesSet.size() != driveFolderFileNames.size()) {
            throw getExceptionWithError("Duplicate items in the drive folder");
        }

        List<String> localFolderFileNames = LocalFileHelper.getFolderFileNames(localFolder);
        HashSet<String> localFolderFileNamesSet = new HashSet<>(localFolderFileNames);

        if (localFolderFileNamesSet.size() != localFolderFileNames.size()) {
            throw getExceptionWithError("Duplicate items in local folder");
        }

        FetchHelper helper = new FetchHelper();
        helper.setDriveFolderFileNamesSet(driveFolderFileNamesSet);
        helper.setLocalFolderFileNamesSet(localFolderFileNamesSet);

        return helper;
    }

    public FetchHelper getFetcher(Activity activity, DocumentFile localFolder, String driveFolderName) throws Exception {
        TextView currentFetchOperationTextView = activity.findViewById(R.id.currentFetchOperationTextView);

        activity.runOnUiThread(() -> currentFetchOperationTextView.setText("Getting drive files..."));
        List<String> driveFolderFileNames = getFolderFileNames(driveFolderName);
        HashSet<String> driveFolderFileNamesSet = new HashSet<>(driveFolderFileNames);

        if (driveFolderFileNamesSet.size() != driveFolderFileNames.size()) {
            throw getExceptionWithError("Duplicate items in the drive folder");
        }

        activity.runOnUiThread(() -> currentFetchOperationTextView.setText("Getting local files..."));

        List<String> localFolderFileNames = LocalFileHelper.getFolderFileNames(localFolder);
        HashSet<String> localFolderFileNamesSet = new HashSet<>(localFolderFileNames);

        if (localFolderFileNamesSet.size() != localFolderFileNames.size()) {
            throw getExceptionWithError("Duplicate items in local folder");
        }

        FetchHelper helper = new FetchHelper();
        helper.setDriveFolderFileNamesSet(driveFolderFileNamesSet);
        helper.setLocalFolderFileNamesSet(localFolderFileNamesSet);

        return helper;
    }

    public Task<FetchHelper> fetchData(Activity activity, DocumentFile localFolder, String driveFolderName) {
        return Tasks.call(executor, () -> {

            if (!isNetworkAvailable()) {
                throw getExceptionWithError("Can't connect to network");
            }

            return getFetcher(activity, localFolder, driveFolderName);
        });
    }

    public Task<Boolean> push(Activity activity, DocumentFile localFolder, String driveFolderName) {
        return Tasks.call(executor, () -> {

            TextView currentOperationNameTextView = activity.findViewById(R.id.currentOperationNameTextView);
            TextView currentOperationFileNameTextView = activity.findViewById(R.id.currentOperationFileNameTextView);
            ProgressBar loadingProgressBar = activity.findViewById(R.id.loadingProgressBar);
            TextView loadingStatusTextView = activity.findViewById(R.id.loadingStatusTextView);
            Button backButton = activity.findViewById(R.id.backMainScreenButton);
            TextView warningTextView = activity.findViewById(R.id.warningTextView);

            if (!isNetworkAvailable()) {
                throw getExceptionWithError("Can't connect to network");
            }

            HashSet<String> uploadToDriveFiles = ConfirmPushActivity.uploadToDriveFiles;
            HashSet<String> deleteOnDriveFiles = ConfirmPushActivity.deleteOnDriveFiles;

            final Integer totalSize = uploadToDriveFiles.size() + deleteOnDriveFiles.size();
            Integer currentCompleted = 0;

            System.out.println("UPLOAD:");
            System.out.println(uploadToDriveFiles);

            for (String name : uploadToDriveFiles) {
                activity.runOnUiThread(() -> {
                    currentOperationNameTextView.setText("Uploading to drive");
                    currentOperationFileNameTextView.setText(name);
                });


                if (uploadToDriveFile(localFolder, name, driveFolderName)) {
                    System.out.println(name + " : " + "OK");

                    currentCompleted++;
                    int percent = Math.round((float)Math.ceil((float)currentCompleted / totalSize * 100));

                    activity.runOnUiThread(() -> {
                        loadingProgressBar.setProgress(percent);
                        loadingStatusTextView.setText("Loading " + percent + "%");
                    });
                }
                else {
                    System.out.println(name + " : FAIL");
                }
            }

            System.out.println("DELETE:");
            System.out.println(deleteOnDriveFiles);

            for (String name : deleteOnDriveFiles) {
                activity.runOnUiThread(() -> {
                    currentOperationNameTextView.setText("Deleting from drive");
                    currentOperationFileNameTextView.setText(name);
                });

                if (deleteDriveFile(driveFolderName, name)) {
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

            Boolean result = testLocalAndDriveFolders(localFolder, driveFolderName);

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

    public Task<Boolean> pull(Activity activity, DocumentFile localFolder, String driveFolderName) {
        return Tasks.call(executor, () -> {

            TextView currentOperationNameTextView = activity.findViewById(R.id.currentOperationNameTextView);
            TextView currentOperationFileNameTextView = activity.findViewById(R.id.currentOperationFileNameTextView);
            ProgressBar loadingProgressBar = activity.findViewById(R.id.loadingProgressBar);
            TextView loadingStatusTextView = activity.findViewById(R.id.loadingStatusTextView);
            Button backButton = activity.findViewById(R.id.backMainScreenButton);
            TextView warningTextView = activity.findViewById(R.id.warningTextView);

            if (!isNetworkAvailable()) {
                throw getExceptionWithError("Can't connect to network");
            }

            HashSet<String> downloadFromDriveFiles = ConfirmPullActivity.downloadFromDriveFiles;
            HashSet<String> deleteInLocalFiles = ConfirmPullActivity.deleteInLocalFiles;

            final Integer totalSize = downloadFromDriveFiles.size() + deleteInLocalFiles.size();
            Integer currentCompleted = 0;

            System.out.println("DOWNLOAD:");
            System.out.println(downloadFromDriveFiles);

            for (String name : downloadFromDriveFiles) {
                activity.runOnUiThread(() -> {
                    currentOperationNameTextView.setText("Downloading from drive");
                    currentOperationFileNameTextView.setText(name);
                });

                if (downloadDriveFile(localFolder, driveFolderName, name)) {
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

            System.out.println("DELETE:");
            System.out.println(deleteInLocalFiles);

            for (String name : deleteInLocalFiles) {
                activity.runOnUiThread(() -> {
                    currentOperationNameTextView.setText("Deleting in local storage");
                    currentOperationFileNameTextView.setText(name);
                });

                if (deleteLocalFile(localFolder, name)) {
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

            Boolean result = testLocalAndDriveFolders(localFolder, driveFolderName);

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
