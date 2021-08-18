package com.example.android_google_drive_loader.Helpers;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GoogleDriveHelper {

    private Executor executor;
    private Drive drive;
    private Context appContext;

    public static String SCOPE = DriveScopes.DRIVE;
    private final int FILE_PAGE_SIZE = 100;

    public void showToast(String msg) {
        Toast.makeText(appContext, msg, Toast.LENGTH_LONG).show();
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

        if (files.size() == 0 || files.size() > 1) {
            throw new Exception("ERROR: Duplicate folders");
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

    public Task<Void> push(String localFolder, String driveFolderName) {
        return Tasks.call(executor, () -> {
            List<String> driveFolderFileNames = getFolderFileNames(driveFolderName);

            HashSet<String> driveFolderFileNamesSet = new HashSet<>(driveFolderFileNames);

            if (driveFolderFileNames.size() != driveFolderFileNames.size()) {
                throw new Exception("Duplicate items on drive");
            }

            return null;
        });
    }
}
