package com.example.android_google_drive_loader.Helpers;

import androidx.documentfile.provider.DocumentFile;

import com.google.api.services.drive.model.File;

import java.util.HashMap;
import java.util.HashSet;

public class SearchHelper {
    public static File findDriveFileByName(HashMap<File, HashSet<File>> files, String name) throws Exception {
        for (File file : files.keySet()) {
            if (file.getName().equals(name)) {
                return file;
            }
        }
        throw new Exception("Drive file " + name + " was not found");
    }

    public static DocumentFile findLocalFileByName(HashMap<DocumentFile, HashSet<DocumentFile>> files, String name) throws Exception {
        for (DocumentFile file : files.keySet()) {
            if (file.getName().equals(name)) {
                return file;
            }
        }
        throw new Exception("Local file " + name + " was not found");
    }

    public static File findDriveFileByName(HashSet<File> files, String name) throws Exception {
        for (File file : files) {
            if (file.getName().equals(name)) {
                return file;
            }
        }
        throw new Exception("Drive file " + name + " was not found");
    }

    public static DocumentFile findLocalFileByName(HashSet<DocumentFile> files, String name) throws Exception {
        for (DocumentFile file : files) {
            if (file.getName().equals(name)) {
                return file;
            }
        }
        throw new Exception("Local file " + name + " was not found");
    }

    public static HashSet<File> strSetToFile(HashSet<File> setToFind, HashSet<String> names) throws Exception {
        HashSet<File> res = new HashSet<>();
        for (String name : names) {
            res.add(findDriveFileByName(setToFind, name));
        }
        return res;
    }

    public static HashSet<DocumentFile> strSetToDocumentFile(HashSet<DocumentFile> setToFind, HashSet<String> names) throws Exception {
        HashSet<DocumentFile> res = new HashSet<>();
        for (String name : names) {
            res.add(findLocalFileByName(setToFind, name));
        }
        return res;
    }
}
