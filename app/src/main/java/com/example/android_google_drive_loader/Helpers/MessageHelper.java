package com.example.android_google_drive_loader.Helpers;

import android.content.Context;
import android.widget.Toast;

public class MessageHelper {
    private Context appContext;

    public MessageHelper(Context context) {
        appContext = context;
    }

    public void showToast(String msg) {
        Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show();
    }

    public Exception getExceptionWithError(String msg) {
        return new Exception("ERROR: " + msg);
    }

}
