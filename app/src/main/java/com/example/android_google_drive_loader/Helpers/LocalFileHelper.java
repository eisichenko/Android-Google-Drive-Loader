package com.example.android_google_drive_loader.Helpers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import androidx.documentfile.provider.DocumentFile;

import com.example.android_google_drive_loader.MainActivity;

import java.io.File;
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
            throw MainActivity.msgHelper.getExceptionWithError("File is not directory");
        }

        DocumentFile[] files = directory.listFiles();

        List<String> res = new ArrayList<>();

        for (DocumentFile file : files) {
            if (file.isDirectory()) {
                throw MainActivity.msgHelper.getExceptionWithError("Directory in files");
            }
            res.add(file.getName());
        }

        return res;
    }

    public static File getFileInDirectoryByName(DocumentFile directory, String fileName) {
        String directoryPath = getAbsolutePathStringFromUri(directory.getUri());

        ArrayList<String> strings = new ArrayList<>();
        strings.add(directoryPath);
        strings.add(fileName);

        return new File(pathCombine(strings));
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
