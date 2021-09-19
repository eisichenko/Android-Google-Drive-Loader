package com.example.android_google_drive_loader.Helpers;

import androidx.documentfile.provider.DocumentFile;

import com.google.api.services.drive.model.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class FetchHelper {

    private HashMap<File, HashSet<File>> driveFolderFiles;
    private HashMap<DocumentFile, HashSet<DocumentFile>> localFolderFiles;
    private HashSet<String> driveFolderNames;
    private HashSet<String> localFolderNames;

    public FetchHelper(HashMap<File, HashSet<File>> driveFolderFiles,
                       HashMap<DocumentFile, HashSet<DocumentFile>> localFolderFiles,
                       HashSet<String> driveFolderNames,
                       HashSet<String> localFolderNames) {
        this.driveFolderFiles = driveFolderFiles;
        this.localFolderFiles = localFolderFiles;
        this.driveFolderNames = driveFolderNames;
        this.localFolderNames = localFolderNames;
    }

    public void setDriveFolderNames(HashSet<String> driveFolderNames) {
        this.driveFolderNames = driveFolderNames;
    }

    public void setLocalFolderNames(HashSet<String> localFolderNames) {
        this.localFolderNames = localFolderNames;
    }

    public HashSet<String> getDriveFolderNames() {
        return driveFolderNames;
    }

    public HashSet<String> getLocalFolderNames() {
        return localFolderNames;
    }

    public HashMap<File, HashSet<File>> getDriveFolderFiles() {
        return driveFolderFiles;
    }

    public void setDriveFolderFiles(HashMap<File, HashSet<File>> driveFolderFiles) {
        this.driveFolderFiles = driveFolderFiles;
    }

    public HashMap<DocumentFile, HashSet<DocumentFile>> getLocalFolderFiles() {
        return localFolderFiles;
    }

    public void setLocalFolderFiles(HashMap<DocumentFile, HashSet<DocumentFile>> localFolderFiles) {
        this.localFolderFiles = localFolderFiles;
    }

    public Boolean mapsAreEqual() throws Exception {
        for (File driveFolder : driveFolderFiles.keySet()) {
            DocumentFile documentFile = SearchHelper.findLocalFileByName(localFolderFiles, driveFolder.getName());

            HashSet<String> driveSet = GoogleDriveHelper.getDriveNamesFromSet(driveFolderFiles.get(driveFolder));
            HashSet<String> localSet = LocalFileHelper.getLocalNamesFromSet(localFolderFiles.get(documentFile));

            if (!driveSet.equals(localSet)) {
                return false;
            }
        }
        return true;
    }
}
