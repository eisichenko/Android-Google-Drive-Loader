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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;


public class LocalFileHelper {

    public static DocumentFile getFileFromUri(Context context, Uri uri) {
        DocumentFile result = DocumentFile.fromTreeUri(context, uri);
        context.grantUriPermission(context.getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        context.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        return result;
    }

    public static List<String> getLocalNamesFromFiles(List<DocumentFile> files) {
        List<String> res = new ArrayList<>();

        for (DocumentFile file : files) {
            res.add(file.getName());
        }

        return res;
    }

    public static HashSet<String> getLocalNamesFromSet(HashSet<DocumentFile> files) {
        HashSet<String> res = new HashSet<>();

        for (DocumentFile file : files) {
            res.add(file.getName());
        }

        return res;
    }

    public static int getMapSize(HashMap<DocumentFile, HashSet<DocumentFile>> localFiles) {
        int res = 0;
        for (HashSet<DocumentFile> set : localFiles.values()) {
            res += set.size();
        }
        return res;
    }

    public static HashSet<DocumentFile> getNestedFolders(DocumentFile folder) throws Exception {
        if (!folder.isDirectory()) {
            throw MainActivity.msgHelper.getExceptionWithError("File is not directory");
        }

        DocumentFile[] files = folder.listFiles();

        ArrayList<DocumentFile> res = new ArrayList<>();

        for (DocumentFile file : files) {
            if (file.isDirectory()) {
                res.add(file);
            }
        }

        HashSet<DocumentFile> resSet = new HashSet<>(res);

        if (resSet.size() != res.size()) {
            throw MainActivity.msgHelper.getExceptionWithError("Duplicate local folders in " + folder.getName());
        }

        HashSet<DocumentFile> folderSet = new HashSet<>();

        for (DocumentFile curFolder : resSet) {
            folderSet.add(curFolder);
            folderSet.addAll(getNestedFolders(curFolder));
        }

        return folderSet;
    }

    public static HashSet<DocumentFile> getFolderFiles(DocumentFile directory) throws Exception {
        if (!directory.isDirectory()) {
            throw MainActivity.msgHelper.getExceptionWithError("File is not directory");
        }

        DocumentFile[] files = directory.listFiles();

        List<DocumentFile> res = new ArrayList<>();

        for (DocumentFile file : files) {
            if (!file.isDirectory()) {
                res.add(file);
            }
        }

        return new HashSet<>(res);
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
