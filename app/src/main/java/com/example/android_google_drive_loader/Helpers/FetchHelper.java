package com.example.android_google_drive_loader.Helpers;

import com.example.android_google_drive_loader.Models.AbstractFile;
import com.example.android_google_drive_loader.Models.DriveFile;
import com.example.android_google_drive_loader.Models.LocalFile;

import java.util.HashMap;
import java.util.HashSet;

public class FetchHelper {

    private HashMap<DriveFile, HashSet<DriveFile>> driveFolderFiles;
    private HashMap<LocalFile, HashSet<LocalFile>> localFolderFiles;
    private HashSet<DriveFile> driveFolders;
    private HashSet<LocalFile> localFolders;

    public FetchHelper(HashMap<DriveFile, HashSet<DriveFile>> driveFolderFiles,
                       HashMap<LocalFile, HashSet<LocalFile>> localFolderFiles,
                       HashSet<DriveFile> driveFolders,
                       HashSet<LocalFile> localFolders) {
        this.driveFolderFiles = driveFolderFiles;
        this.localFolderFiles = localFolderFiles;
        this.driveFolders = driveFolders;
        this.localFolders = localFolders;
    }

    public HashSet<DriveFile> getDriveFolders() {
        return driveFolders;
    }

    public HashSet<LocalFile> getLocalFolders() {
        return localFolders;
    }

    public HashMap<DriveFile, HashSet<DriveFile>> getDriveFolderFiles() {
        return driveFolderFiles;
    }

    public HashMap<LocalFile, HashSet<LocalFile>> getLocalFolderFiles() {
        return localFolderFiles;
    }

    public Boolean mapsAreEqual() {
        return driveFolderFiles.equals(localFolderFiles);
    }

    public static <T extends AbstractFile, U extends AbstractFile> Integer getMapSize(HashMap<T, HashSet<U>> m) {
        int sum = 0;

        for (AbstractFile folder : m.keySet()) {
            sum += m.get(folder).size();
        }
        return sum;
    }
}
