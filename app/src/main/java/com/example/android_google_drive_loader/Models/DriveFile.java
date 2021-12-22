package com.example.android_google_drive_loader.Models;

import androidx.annotation.NonNull;

import com.example.android_google_drive_loader.Helpers.PathHelper;
import com.google.api.services.drive.model.File;

public class DriveFile extends AbstractFile {
    private String id;
    private String name;
    private String absolutePath;
    private DriveFile parent;

    public DriveFile(File file, DriveFile parent) {
        id = file.getId();
        name = file.getName();
        this.parent = parent;

        if (this.parent == null) {
            this.absolutePath = file.getName();
        }
        else {
            this.absolutePath = PathHelper.pathCombine(parent.getAbsolutePath(), file.getName());
        }
    }

    public DriveFile getParent() {
        return parent;
    }

    public void setParent(DriveFile parent) {
        this.parent = parent;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

}
