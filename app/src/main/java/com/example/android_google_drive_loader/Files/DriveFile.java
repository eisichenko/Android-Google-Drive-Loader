package com.example.android_google_drive_loader.Files;

import androidx.annotation.NonNull;

import com.google.api.services.drive.model.File;

public class DriveFile extends AbstractFile {
    private String id;
    private String name;

    public DriveFile(File file) {
        id = file.getId();
        name = file.getName();
    }

    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @NonNull
    @Override
    public String toString() {
        return getName();
    }
}
