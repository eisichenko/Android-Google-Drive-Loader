package com.example.android_google_drive_loader.Helpers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import androidx.documentfile.provider.DocumentFile;

import com.example.android_google_drive_loader.Models.LocalFile;
import com.example.android_google_drive_loader.MainActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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

    public static HashSet<LocalFile> getNestedFolders(DocumentFile folder, LocalFile parent) throws Exception {
        if (!folder.isDirectory()) {
            throw MainActivity.msgHelper.getExceptionWithError("File is not directory");
        }

        LocalFile rootFile = new LocalFile(folder, parent);

        DocumentFile[] files = folder.listFiles();

        HashSet<LocalFile> res = new HashSet<>();

        if (parent == null) {
            res.add(rootFile);
        }

        for (DocumentFile file : files) {
            if (file.isDirectory()) {
                LocalFile localFile = new LocalFile(file, rootFile);
                if (res.contains(localFile)) {
                    throw MainActivity.msgHelper.getExceptionWithError("Duplicate local folders in " + folder.getName());
                }
                res.add(localFile);
                res.addAll(getNestedFolders(file, rootFile));
            }
        }

        return res;
    }

    public HashSet<LocalFile> getFolderFiles(LocalFile localFile) throws Exception {
        DocumentFile directory = localFile.getFile();

        if (!directory.isDirectory()) {
            throw MainActivity.msgHelper.getExceptionWithError("File is not directory");
        }

        DocumentFile[] files = directory.listFiles();

        HashSet<LocalFile> res = new HashSet<>();

        for (DocumentFile file : files) {
            if (!file.isDirectory()) {
                res.add(new LocalFile(file, localFile));
            }
        }

        return res;
    }

    public static File getFileFromDocumentFile(DocumentFile file) {
        return new File(PathHelper.getAbsolutePathStringFromUri(file.getUri()));
    }

}
