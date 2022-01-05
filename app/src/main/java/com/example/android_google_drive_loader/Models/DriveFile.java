package com.example.android_google_drive_loader.Models;

import com.example.android_google_drive_loader.Helpers.GoogleDriveHelper;
import com.example.android_google_drive_loader.Helpers.PathHelper;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.util.ArrayList;
import java.util.HashSet;

public class DriveFile extends AbstractFile {
    private String id;
    private String name;
    private String absolutePath;
    private DriveFile parent;
    private long size;
    private HashSet<DriveFile> childFiles;
    private HashSet<DriveFile> childFolders;

    public DriveFile(File file, DriveFile parent) throws Exception {
        id = file.getId();
        name = file.getName();
        this.parent = parent;

        this.childFiles = new HashSet<>();
        this.childFolders = new HashSet<>();

        if (file.getSize() != null) {
            this.size = file.getSize();
        }
        else {
            this.size = 0L;
        }

        if (this.parent == null) {
            this.absolutePath = file.getName();
        }
        else {
            this.absolutePath = PathHelper.pathCombine(parent.getAbsolutePath(), file.getName());
        }

        if (parent != null) {
            if (file.getMimeType() != null && file.getMimeType().equals(GoogleDriveHelper.GOOGLE_DRIVE_FOLDER_TYPE)) {
                if (parent.childFolders.contains(this)) {
                    throw new Exception("Duplicate drive folders " + this.absolutePath);
                }
                parent.childFolders.add(this);
            }
            else {
                if (parent.childFiles.contains(this)) {
                    throw new Exception("Duplicate drive files " + this.absolutePath);
                }
                parent.childFiles.add(this);
            }
        }
    }

    public HashSet<DriveFile> getChildFolders() {
        return childFolders;
    }

    public void setChildFolders(HashSet<DriveFile> childFolders) {
        this.childFolders = childFolders;
    }

    public HashSet<DriveFile> getChildFiles() {
        return childFiles;
    }

    public void setChildFiles(HashSet<DriveFile> childFiles) {
        this.childFiles = childFiles;
    }

    @Override
    public long getSizeInBytes() {
        return size;
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
