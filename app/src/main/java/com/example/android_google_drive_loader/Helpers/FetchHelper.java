package com.example.android_google_drive_loader.Helpers;

import com.google.api.services.drive.model.File;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class FetchHelper {

    private HashSet<String> driveFolderFileNamesSet;
    private HashSet<String> localFolderFileNamesSet;

    public HashSet<String> getDriveFolderFileNamesSet() {
        return driveFolderFileNamesSet;
    }

    public void setDriveFolderFileNamesSet(HashSet<String> driveFolderFileNamesSet) {
        this.driveFolderFileNamesSet = driveFolderFileNamesSet;
    }

    public HashSet<String> getLocalFolderFileNamesSet() {
        return localFolderFileNamesSet;
    }

    public void setLocalFolderFileNamesSet(HashSet<String> localFolderFileNamesSet) {
        this.localFolderFileNamesSet = localFolderFileNamesSet;
    }
}
