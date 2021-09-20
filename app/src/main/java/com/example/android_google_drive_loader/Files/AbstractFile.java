package com.example.android_google_drive_loader.Files;

public abstract class AbstractFile {
    public abstract String getName();

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (! (obj instanceof AbstractFile)) {
            return false;
        }

        return ((AbstractFile) obj).getName().equals(getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }
}
