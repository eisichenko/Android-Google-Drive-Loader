package com.example.android_google_drive_loader.Helpers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import androidx.documentfile.provider.DocumentFile;

import com.example.android_google_drive_loader.MainActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class LocalFileHelper {

    public static DocumentFile getFileFromUri(Context context, Uri uri) {
        DocumentFile result = DocumentFile.fromTreeUri(context, uri);
        context.grantUriPermission(context.getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        context.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        return result;
    }

    public static List<String> getFolderFileNames(DocumentFile directory) throws Exception {
        if (!directory.isDirectory()) {
            throw MainActivity.driveHelper.getExceptionWithError("File is not directory");
        }

        DocumentFile[] files = directory.listFiles();

        List<String> res = new ArrayList<>();

        for (DocumentFile file : files) {
            if (file.isDirectory()) {
                throw MainActivity.driveHelper.getExceptionWithError("Directory in files");
            }
            res.add(file.getName());
        }

        return res;
    }

    public static String getPathStringFromUri(Uri uri) {
        ArrayList<String> path_strings = new ArrayList<>(Arrays.asList(uri.getPath().split(":|/")));

        while (path_strings.get(0).length() == 0) {
            path_strings.remove(0);
        }

        while (path_strings.get(path_strings.size() - 1).length() == 0) {
            path_strings.remove(path_strings.size() - 1);
        }

        if (path_strings.get(1).equals("primary")) {
            path_strings.set(1, Environment.getExternalStorageDirectory().getPath());
            path_strings.remove(0);
        }
        else {
            path_strings.set(0, "storage");
        }

        String res = "";

        for (String str : path_strings) {
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
