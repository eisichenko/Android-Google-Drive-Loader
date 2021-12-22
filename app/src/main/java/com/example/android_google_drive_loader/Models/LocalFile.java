package com.example.android_google_drive_loader.Models;

import androidx.documentfile.provider.DocumentFile;

import com.example.android_google_drive_loader.Helpers.PathHelper;

public class LocalFile extends AbstractFile{
    private String name;
    private DocumentFile documentFile;
    private String absolutePath;
    private LocalFile parent;
    private long size;

    public LocalFile(DocumentFile file, LocalFile parent) {
        name = file.getName();
        this.documentFile = file;
        this.parent = parent;
        this.size = file.length();

        if (this.parent == null) {
            this.absolutePath = file.getName();
        }
        else {
            this.absolutePath = PathHelper.pathCombine(parent.getAbsolutePath(), file.getName());
        }
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
