package com.example.android_google_drive_loader.Helpers;

import androidx.documentfile.provider.DocumentFile;

import com.example.android_google_drive_loader.Files.AbstractFile;
import com.google.api.services.drive.model.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class FetchHelper {

    private HashMap<AbstractFile, HashSet<AbstractFile>> driveFolderFiles;
    private HashMap<AbstractFile, HashSet<AbstractFile>> localFolderFiles;

    public FetchHelper(HashMap<AbstractFile, HashSet<AbstractFile>> driveFolderFiles,
                       HashMap<AbstractFile, HashSet<AbstractFile>> localFolderFiles) {
        this.driveFolderFiles = driveFolderFiles;
        this.localFolderFiles = localFolderFiles;
    }

    public HashMap<AbstractFile, HashSet<AbstractFile>> getDriveFolderFiles() {
        return driveFolderFiles;
    }

    public HashMap<AbstractFile, HashSet<AbstractFile>> getLocalFolderFiles() {
        return localFolderFiles;
    }

    public Boolean mapsAreEqual() {
        return driveFolderFiles.equals(localFolderFiles);
    }

    public static Integer getMapSize(HashMap<AbstractFile, HashSet<AbstractFile>> m) {
        Integer sum = 0;

        for (AbstractFile folder : m.keySet()) {
            sum += m.get(folder).size();
        }
        return sum;
    }
}
