package com.example.android_google_drive_loader;

import com.example.android_google_drive_loader.Enums.DriveType;

public class RecyclerViewItem {
    private String name;
    private DriveType driveType;

    public RecyclerViewItem(String name) {
        this.name = name;
        driveType = DriveType.ANY;
    }

    public RecyclerViewItem(String name, DriveType driveType) {
        this.name = name;
        this.driveType = driveType;
    }

    public String getName() {
        return name;
    }

    public DriveType getType() { return driveType; }
}
