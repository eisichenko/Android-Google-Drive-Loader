package com.example.android_google_drive_loader.Helpers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import com.example.android_google_drive_loader.MainActivity;
import com.example.android_google_drive_loader.Models.LocalFile;

import java.io.File;
import java.util.HashSet;


public class LocalFileHelper {

    private final Context context;

    public LocalFileHelper(Context context) {
        this.context = context;
    }

    public DocumentFile getFileFromUri(Uri uri) {
        DocumentFile result = DocumentFile.fromTreeUri(context, uri);
        context.grantUriPermission(context.getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        context.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        return result;
    }

    public HashSet<LocalFile> getLocalFilesAndFolders(LocalFile rootFolder, LocalFile parent) throws Exception {
        if (!rootFolder.getFile().isDirectory()) {
            throw MainActivity.msgHelper.getExceptionWithError("File is not directory");
        }

        DocumentFile[] files = rootFolder.getFile().listFiles();

        HashSet<LocalFile> localFolders = new HashSet<>();

        if (parent == null) {
            localFolders.add(rootFolder);
        }

        for (DocumentFile file : files) {
            LocalFile localFile = new LocalFile(file, rootFolder);
            if (file.isDirectory()) {
                if (localFolders.contains(localFile)) {
                    throw MainActivity.msgHelper.getExceptionWithError("Duplicate local folders " + localFile.getAbsolutePath());
                }
                localFolders.add(localFile);
                localFolders.addAll(getLocalFilesAndFolders(localFile, rootFolder));
            }
        }

        return localFolders;
    }

    public static File getFileFromDocumentFile(DocumentFile file) {
        return new File(PathHelper.getAbsolutePathStringFromUri(file.getUri()));
    }

}
