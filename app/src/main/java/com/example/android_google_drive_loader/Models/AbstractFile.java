package com.example.android_google_drive_loader.Models;

import androidx.annotation.NonNull;

public abstract class AbstractFile {
    public abstract String getAbsolutePath();

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (! (obj instanceof AbstractFile)) {
            return false;
        }

        return ((AbstractFile) obj).getAbsolutePath().equals(getAbsolutePath());
    }

    @Override
    public int hashCode() {
        return getAbsolutePath().hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return getAbsolutePath();
    }
}
