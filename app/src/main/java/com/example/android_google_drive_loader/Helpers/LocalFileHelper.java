package com.example.android_google_drive_loader.Helpers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import androidx.documentfile.provider.DocumentFile;

import com.example.android_google_drive_loader.Files.AbstractFile;
import com.example.android_google_drive_loader.Files.LocalFile;
import com.example.android_google_drive_loader.MainActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;


public class LocalFileHelper {

    private Context context;

    public LocalFileHelper(Context context) {
        this.context = context;
    }

    public DocumentFile getFileFromUri(Uri uri) {
        DocumentFile result = DocumentFile.fromTreeUri(context, uri);
        context.grantUriPermission(context.getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        context.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        return result;
    }

    public static HashSet<AbstractFile> getNestedFolders(DocumentFile folder) throws Exception {
        if (!folder.isDirectory()) {
            throw MainActivity.msgHelper.getExceptionWithError("File is not directory");
        }

        DocumentFile[] files = folder.listFiles();

        HashSet<AbstractFile> res = new HashSet<>();

        for (DocumentFile file : files) {
            if (file.isDirectory()) {
                LocalFile localFile = new LocalFile(file);
                if (res.contains(localFile)) {
                    throw MainActivity.msgHelper.getExceptionWithError("Duplicate local folders in " + folder.getName());
                }
                res.add(localFile);
                res.addAll(getNestedFolders(file));
            }
        }

        return res;
    }

    public HashSet<AbstractFile> getFolderFiles(LocalFile localFile) throws Exception {
        DocumentFile directory = localFile.getFile();

        if (!directory.isDirectory()) {
            throw MainActivity.msgHelper.getExceptionWithError("File is not directory");
        }

        DocumentFile[] files = directory.listFiles();

        HashSet<AbstractFile> res = new HashSet<>();

        for (DocumentFile file : files) {
            if (!file.isDirectory()) {
                res.add(new LocalFile(file));
            }
        }

        return res;
    }

    public static File getFileFromDocumentFile(DocumentFile file) {
        return new File(getAbsolutePathStringFromUri(file.getUri()));
    }

    public static String getAbsolutePathStringFromUri(Uri uri) {
        ArrayList<String> strings = new ArrayList<>(Arrays.asList(uri.toString().split("/")));

        String pathString = Uri.decode(strings.get(strings.size() - 1));

        ArrayList<String> pathStrings = new ArrayList<>(Arrays.asList(pathString.split(":")));

        if (pathStrings.get(0).equals("primary")) {
            pathStrings.set(0, Environment.getExternalStorageDirectory().getPath());
        }
        else {
            pathStrings.add(0, "/storage");
        }

        return pathCombine(pathStrings);
    }

    public static String pathCombine(ArrayList<String> pathStrings) {
        String res = "";

        for (String str : pathStrings) {
            if (res.length() == 0) {
                res += str;
            }
            else if (res.charAt(res.length() - 1) == '/') {
                res += str;
            }
            else {
                res += '/' + str;
            }
        }

        return res;
    }
}
