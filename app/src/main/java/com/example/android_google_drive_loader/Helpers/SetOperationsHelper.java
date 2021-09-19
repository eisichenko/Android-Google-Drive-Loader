package com.example.android_google_drive_loader.Helpers;

import java.util.HashSet;
import java.util.List;

public class SetOperationsHelper {
    public static <T> HashSet<T> relativeComplement(HashSet<T> set1, HashSet<T> set2) {
        HashSet<T> res = new HashSet<>();

        for (T item : set1) {
            if (!set2.contains(item)) {
                res.add(item);
            }
        }

        return res;
    }
}
