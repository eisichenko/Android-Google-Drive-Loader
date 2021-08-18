package com.example.android_google_drive_loader;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;

import com.example.android_google_drive_loader.Helpers.GoogleDriveHelper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SIGN_IN = 1;
    private GoogleDriveHelper driveHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button push_button = findViewById(R.id.pushButton);
        push_button.setOnClickListener(view -> {
            try {
                System.out.println("TRY");
                driveHelper.push("LOL", "audios").addOnSuccessListener(files -> {
                    System.out.println("SUCCESS");
                }).addOnFailureListener(e -> {
                    System.out.println("SHIT");
                    driveHelper.showToast(e.getMessage());
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                });
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                driveHelper.showToast(e.getMessage());
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplicationContext());

        if (account == null) {
            signIn();
        }
        else {
            driveHelper = new GoogleDriveHelper(getApplicationContext(),
                    account,
                    getResources().getString(R.string.app_name));
        }
    }

    private void signIn() {
        GoogleSignInClient googleSignInClient = buildGoogleSignInClient();
        startActivityForResult(googleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    private GoogleSignInClient buildGoogleSignInClient() {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(GoogleDriveHelper.SCOPE))
                        .build();
        System.out.println("DAMN");
        return GoogleSignIn.getClient(getApplicationContext(), signInOptions);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    handleSignInResult(resultData);
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, resultData);
    }

    private void handleSignInResult(Intent result) {
        System.out.println("ASS");
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(account -> driveHelper = new GoogleDriveHelper(getApplicationContext(),
                        account,
                        getResources().getString(R.string.app_name)))
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    System.out.println(e.getMessage());
                });
    }
}