package com.example.android_google_drive_loader.Helpers;

import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class PathHelper {
    public static String getAbsolutePathStringFromUri(Uri uri) {
        if (uri.toString().startsWith("file:///")) {
            return uri.getPath();
        }

        ArrayList<String> strings = new ArrayList<>(Arrays.asList(uri.toString().split(File.separator)));

        String pathString = Uri.decode(strings.get(strings.size() - 1));

        ArrayList<String> pathStrings = new ArrayList<>(Arrays.asList(pathString.split(":")));

        if (pathStrings.get(0).equals("primary")) {
            pathStrings.set(0, Environment.getExternalStorageDirectory().getPath());
        }
        else {
            pathStrings.add(0, File.separator + "storage");
        }

        String res = pathCombine(pathStrings);

        if (!res.endsWith(File.separator)) {
            return res + File.separator;
        }
        return res;
    }

    public static String pathCombine(ArrayList<String> pathStrings) {
        return pathCombine(pathStrings.toArray(new String[0]));
    }

    public static String pathCombine(String... pathStrings) {
        StringBuilder res = new StringBuilder();

        for (String str : pathStrings) {
            if (res.length() == 0) {
                res.append(str);
            }
            else if (res.charAt(res.length() - 1) == File.separatorChar) {
                res.append(str);
            }
            else {
                res.append(File.separator).append(str);
            }
        }

        return res.toString();
    }
}
