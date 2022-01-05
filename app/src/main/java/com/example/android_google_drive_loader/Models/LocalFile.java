package com.example.android_google_drive_loader.Models;

import androidx.documentfile.provider.DocumentFile;

import com.example.android_google_drive_loader.Helpers.GoogleDriveHelper;
import com.example.android_google_drive_loader.Helpers.PathHelper;

import java.util.HashSet;

public class LocalFile extends AbstractFile{
    private String name;
    private DocumentFile documentFile;
    private String absolutePath;
    private LocalFile parent;
    private long size;

    private HashSet<LocalFile> childFiles;
    private HashSet<LocalFile> childFolders;

    public LocalFile(DocumentFile file, LocalFile parent) throws Exception {
        name = file.getName();
        this.documentFile = file;
        this.parent = parent;
        this.size = file.length();

        this.childFiles = new HashSet<>();
        this.childFolders = new HashSet<>();

        if (this.parent == null) {
            this.absolutePath = file.getName();
        }
        else {
            this.absolutePath = PathHelper.pathCombine(parent.getAbsolutePath(), file.getName());
        }

        if (parent != null) {
            if (file.isDirectory()) {
                if (parent.childFolders.contains(this)) {
                    throw new Exception("Duplicate local folders " + this.absolutePath);
                }
                parent.childFolders.add(this);
            }
            else {
                if (parent.childFiles.contains(this)) {
                    throw new Exception("Duplicate local files " + this.absolutePath);
                }
                parent.childFiles.add(this);
            }
        }
    }

    public HashSet<LocalFile> getChildFolders() {
        return childFolders;
    }

    public void setChildFolders(HashSet<LocalFile> childFolders) {
        this.childFolders = childFolders;
    }

    public HashSet<LocalFile> getChildFiles() {
        return childFiles;
    }

    public void setChildFiles(HashSet<LocalFile> childFiles) {
        this.childFiles = childFiles;
    }

    @Override
    public long getSizeInBytes() {
        return size;
    }

    public LocalFile getParent() {
        return parent;
    }

    public void setParent(LocalFile parent) {
        this.parent = parent;
    }

    @Override
    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }

    public String getName() {
        return name;
    }

    public DocumentFile getFile() {
        return documentFile;
    }

}
