package com.example.android_google_drive_loader;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.android_google_drive_loader.Helpers.FetchHelper;
import com.example.android_google_drive_loader.Helpers.LocalFileHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;

import com.example.android_google_drive_loader.Helpers.GoogleDriveHelper;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SIGN_IN = 1;
    private static final int REQUEST_CODE_PERMISSION = 2;
    private static final int REQUEST_CODE_CHECK_SETTINGS = 3;
    private static final int REQUEST_CODE_CHOOSE_FOLDER = 4;

    public static GoogleDriveHelper driveHelper;
    public static DocumentFile pickedDir;
    public static String driveFolderName;
    public static FetchHelper fetchHelper;

    public Button pushButton;
    public Button pullButton;
    public Button chooseButton;
    public TextView chosenFolderTextView;
    public EditText driveFolderNameEditText;
    public TextView loadingTextView;
    public ProgressBar progressBar;

    public final String APP_PREFERENCES_NAME = "gd_loader_settings";
    public final String LOCAL_DIRECTORY_URI_CACHE_NAME = "LocalDirectory";
    public final String DRIVE_DIRECTORY_URI_CACHE_NAME = "DriveDirectory";

    public SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pushButton = findViewById(R.id.pushButton);
        driveFolderNameEditText = findViewById(R.id.driveFolderEditText);

        progressBar = findViewById(R.id.progressBar);
        loadingTextView = findViewById(R.id.loadingTextView);

        progressBar.setVisibility(View.INVISIBLE);
        loadingTextView.setVisibility(View.INVISIBLE);

        pushButton.setOnClickListener(view -> {
            try {
                driveFolderName = driveFolderNameEditText.getText().toString();

                if (driveFolderName.length() == 0) {
                    throw driveHelper.getExceptionWithError("Empty drive folder name");
                }
                else if (pickedDir == null) {
                    throw driveHelper.getExceptionWithError("Local folder was not picked");
                }

                settings.edit().putString(DRIVE_DIRECTORY_URI_CACHE_NAME, driveFolderName).apply();

                driveHelper.fetchData(pickedDir, driveFolderName).addOnSuccessListener(resFetchHelper -> {

                    fetchHelper = resFetchHelper;

                    if (!fetchHelper.getLocalFolderFileNamesSet().equals(fetchHelper.getDriveFolderFileNamesSet())) {
                        Intent intent = new Intent(this, ConfirmPushActivity.class);
                        startActivity(intent);
                    }
                    else {
                        driveHelper.showToast("Everything is up-to-date");
                    }

                    progressBar.setVisibility(View.INVISIBLE);
                    loadingTextView.setVisibility(View.INVISIBLE);
                }).addOnFailureListener(e -> {
                    driveHelper.showToast(e.getMessage());
                    System.out.println(e.getMessage());
                    e.printStackTrace();

                    progressBar.setVisibility(View.INVISIBLE);
                    loadingTextView.setVisibility(View.INVISIBLE);
                });

                progressBar.setVisibility(View.VISIBLE);
                loadingTextView.setVisibility(View.VISIBLE);
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                driveHelper.showToast(e.getMessage());

                progressBar.setVisibility(View.INVISIBLE);
                loadingTextView.setVisibility(View.INVISIBLE);
            }
        });

        pullButton = findViewById(R.id.pullButton);
        pullButton.setOnClickListener(view -> {
            try {
                driveFolderName = driveFolderNameEditText.getText().toString();

                if (driveFolderName.length() == 0) {
                    throw driveHelper.getExceptionWithError("Empty drive folder name");
                }
                else if (pickedDir == null) {
                    throw driveHelper.getExceptionWithError("Local folder was not picked");
                }

                settings.edit().putString(DRIVE_DIRECTORY_URI_CACHE_NAME, driveFolderName).apply();

                driveHelper.fetchData(pickedDir, driveFolderName).addOnSuccessListener(resFetchHelper -> {

                    fetchHelper = resFetchHelper;

                    Intent intent = new Intent(this, ConfirmPullActivity.class);
                    startActivity(intent);

                    progressBar.setVisibility(View.INVISIBLE);
                    loadingTextView.setVisibility(View.INVISIBLE);
                }).addOnFailureListener(e -> {
                    driveHelper.showToast(e.getMessage());
                    System.out.println(e.getMessage());
                    e.printStackTrace();

                    progressBar.setVisibility(View.INVISIBLE);
                    loadingTextView.setVisibility(View.INVISIBLE);
                });

                progressBar.setVisibility(View.VISIBLE);
                loadingTextView.setVisibility(View.VISIBLE);
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                driveHelper.showToast(e.getMessage());

                progressBar.setVisibility(View.INVISIBLE);
                loadingTextView.setVisibility(View.INVISIBLE);
            }
        });

        chooseButton = findViewById(R.id.chooseFolderBtn);
        chooseButton.setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(Intent.createChooser(intent, "Check Settings"), REQUEST_CODE_CHECK_SETTINGS);
            }
            else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
                    (ContextCompat.checkSelfPermission( this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                            PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                                    PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE_PERMISSION);
            }
            else {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                startActivityForResult(Intent.createChooser(intent, "Choose directory"), REQUEST_CODE_CHOOSE_FOLDER);
            }
        });

        chosenFolderTextView = findViewById(R.id.chosenFolder);

        settings = getSharedPreferences(APP_PREFERENCES_NAME, Context.MODE_PRIVATE);
        String pickedDirUri = settings.getString(LOCAL_DIRECTORY_URI_CACHE_NAME, "");

        if (pickedDirUri.length() > 0) {
            pickedDir = DocumentFile.fromTreeUri(getApplicationContext(), Uri.parse(pickedDirUri));
            if (pickedDir != null) {
                chosenFolderTextView.setText(LocalFileHelper.getAbsolutePathStringFromUri(pickedDir.getUri()));
            }
        }

        String driveFolderName = settings.getString(DRIVE_DIRECTORY_URI_CACHE_NAME, "");

        if (driveFolderName.length() > 0) {
            driveFolderNameEditText.setText(driveFolderName);
        }

    }

    @Override
    public void onStart() {
        super.onStart();

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplicationContext());

        if (account == null) {
            signIn();
        }
        else {
            driveHelper = new GoogleDriveHelper(getApplicationContext(), account, getResources().getString(R.string.app_name));
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
        return GoogleSignIn.getClient(getApplicationContext(), signInOptions);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (resultCode == Activity.RESULT_OK && resultData != null) {
            switch (requestCode) {
                case REQUEST_CODE_SIGN_IN:
                    handleSignInResult(resultData);
                    break;
                case REQUEST_CODE_CHECK_SETTINGS:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (Environment.isExternalStorageManager()) {
                            driveHelper.showToast("Access was given successfully");
                        }
                        else {
                            driveHelper.showToast("App won't work without access");
                        }
                    }
                    else {
                        driveHelper.showToast("Access was given successfully");
                    }
                    break;
                case REQUEST_CODE_CHOOSE_FOLDER:
                    pickedDir = LocalFileHelper.getFileFromUri(getApplicationContext(), resultData.getData());
                    chosenFolderTextView.setText(LocalFileHelper.getAbsolutePathStringFromUri(pickedDir.getUri()));

                    settings.edit().putString(LOCAL_DIRECTORY_URI_CACHE_NAME, pickedDir.getUri().toString()).apply();

                    break;
            }
        }

        super.onActivityResult(requestCode, resultCode, resultData);
    }

    private void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(account -> {
                    driveHelper = new GoogleDriveHelper(getApplicationContext(), account, getResources().getString(R.string.app_name));
                    driveHelper.showToast("Sign up was successful");
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    System.out.println(e.getMessage());
                    driveHelper.showToast("ERROR: Sign up was not finished");
                });
    }
}