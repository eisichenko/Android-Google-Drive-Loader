package com.example.android_google_drive_loader.Files;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

public class LocalFile extends AbstractFile{
    private String name;
    private DocumentFile documentFile;

    public LocalFile(DocumentFile file) {
        name = file.getName();
        this.documentFile = file;
    }

    @Override
    public String getName() {
        return name;
    }

    public DocumentFile getFile() {
        return documentFile;
    }

    @NonNull
    @Override
    public String toString() {
        return getName();
    }
}
