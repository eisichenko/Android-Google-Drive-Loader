package com.example.android_google_drive_loader.Helpers;

import com.example.android_google_drive_loader.Files.AbstractFile;

import java.util.HashSet;

public class SetOperationsHelper {
    public static <T extends AbstractFile, U extends AbstractFile> HashSet<T> relativeComplement(HashSet<T> set1, HashSet<U> set2) {
        HashSet<T> res = new HashSet<>();

        for (T item : set1) {
            if (!set2.contains(item)) {
                res.add(item);
            }
        }

        return res;
    }
}
