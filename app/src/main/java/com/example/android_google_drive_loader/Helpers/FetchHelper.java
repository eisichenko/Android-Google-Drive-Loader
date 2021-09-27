package com.example.android_google_drive_loader.Helpers;

import com.example.android_google_drive_loader.Files.AbstractFile;
import com.example.android_google_drive_loader.Files.DriveFile;
import com.example.android_google_drive_loader.Files.LocalFile;

import java.util.HashMap;
import java.util.HashSet;

public class FetchHelper {

    private HashMap<DriveFile, HashSet<DriveFile>> driveFolderFiles;
    private HashMap<LocalFile, HashSet<LocalFile>> localFolderFiles;

    public FetchHelper(HashMap<DriveFile, HashSet<DriveFile>> driveFolderFiles,
                       HashMap<LocalFile, HashSet<LocalFile>> localFolderFiles) {
        this.driveFolderFiles = driveFolderFiles;
        this.localFolderFiles = localFolderFiles;
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
        Integer sum = 0;

        for (AbstractFile folder : m.keySet()) {
            sum += m.get(folder).size();
        }
        return sum;
    }
}
