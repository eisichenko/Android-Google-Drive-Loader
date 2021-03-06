package com.example.android_google_drive_loader;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import com.example.android_google_drive_loader.Enums.OperationType;
import com.example.android_google_drive_loader.Enums.StopwatchState;
import com.example.android_google_drive_loader.Enums.Theme;
import com.example.android_google_drive_loader.Helpers.FetchHelper;
import com.example.android_google_drive_loader.Helpers.GoogleDriveHelper;
import com.example.android_google_drive_loader.Helpers.LocalFileHelper;
import com.example.android_google_drive_loader.Helpers.MessageHelper;
import com.example.android_google_drive_loader.Helpers.PathHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SIGN_IN = 1;
    private static final int REQUEST_CODE_CHOOSE_FOLDER = 2;
    private static final int REQUEST_PERMISSIONS_ON_PUSH = 3;
    private static final int REQUEST_PERMISSIONS_ON_PULL = 4;

    public static GoogleDriveHelper driveHelper;
    public LocalFileHelper localHelper = new LocalFileHelper(this);
    public static MessageHelper msgHelper;
    public static DocumentFile pickedDir;
    public static String driveFolderName;
    public static FetchHelper fetchHelper;
    public static StopwatchState stopwatchState;
    public static Theme currentTheme = Theme.DAY;

    public Button pushButton;
    public Button pullButton;
    public Button chooseButton;
    public Button signInButton;
    public Button logoutButton;
    public TextView chosenFolderTextView;
    public TextView loadingTextView;
    public TextView driveFolderNameTextView;
    public ProgressBar progressBar;
    public TextView accountNameTextView;

    public final String APP_PREFERENCES_NAME = "gd_loader_settings";
    public final String LOCAL_DIRECTORY_URI_CACHE_NAME = "LocalDirectory";
    public static final String THEME_CACHE_NAME = "Theme";

    public static SharedPreferences settings;

    public static OperationType operationType = OperationType.NONE;

    public Boolean checkPermissions(final Integer requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            Toast.makeText(this, "Please check all files permission to GD Loader", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            startActivityForResult(Intent.createChooser(intent, "Check Settings"), requestCode);
            return false;
        }
        else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
                (ContextCompat.checkSelfPermission( this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                        PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                                PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    requestCode);
            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS_ON_PUSH) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    msgHelper.showToast("Access was given successfully");
                    pushButton.callOnClick();
                }
                else {
                    msgHelper.showToast("App won't work without access");
                    return;
                }
            }
            else if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Access was given successfully", Toast.LENGTH_SHORT).show();
                pushButton.callOnClick();
            }
            else {
                Toast.makeText(this, "App won't work without access", Toast.LENGTH_SHORT).show();
            }
        }
        else if (requestCode == REQUEST_PERMISSIONS_ON_PULL) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    msgHelper.showToast("Access was given successfully");
                    pullButton.callOnClick();
                }
                else {
                    msgHelper.showToast("App won't work without access");
                    return;
                }
            }
            else if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Access was given successfully", Toast.LENGTH_SHORT).show();
                pullButton.callOnClick();
            }
            else {
                Toast.makeText(this, "App won't work without access", Toast.LENGTH_SHORT).show();
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);
        MenuItem themeItem = menu.findItem(R.id.theme_menu);

        if (currentTheme == Theme.DAY) {
            themeItem.setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_theme_night));
        }
        else {
            themeItem.setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_theme_day));
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.theme_menu) {
            if (currentTheme == Theme.DAY) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                settings.edit().putString(THEME_CACHE_NAME, Theme.NIGHT.toString()).apply();
                item.setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_theme_day));
                currentTheme = Theme.NIGHT;
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                settings.edit().putString(THEME_CACHE_NAME, Theme.DAY.toString()).apply();
                item.setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_theme_night));
                currentTheme = Theme.DAY;
            }

            recreate();

            return true;
        }
        else if (item.getItemId() == R.id.aboutMenuItem) {
            String versionName = "";
            try {
                versionName = getPackageManager()
                        .getPackageInfo(getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            new AlertDialog.Builder(this)
                    .setTitle("About")
                    .setMessage("??? Written by Egor Isichenko, 2021\n\n" +
                            String.format("??? Version %s", versionName))
                    .setPositiveButton("OK", (dialog, whichButton) -> {

                    })
                    .show();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        msgHelper = new MessageHelper(this);

        settings = getSharedPreferences(APP_PREFERENCES_NAME, Context.MODE_PRIVATE);

        String themeString = settings.getString(THEME_CACHE_NAME, null);

        if (themeString != null) {
            if (themeString.equals(Theme.DAY.toString())) {
                currentTheme = Theme.DAY;
                setTheme(R.style.Theme_Day);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            else {
                currentTheme = Theme.NIGHT;
                setTheme(R.style.Theme_Night);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pushButton = findViewById(R.id.pushButton);
        pullButton = findViewById(R.id.pullButton);
        chooseButton = findViewById(R.id.chooseFolderBtn);
        chosenFolderTextView = findViewById(R.id.chosenFolder);
        driveFolderNameTextView = findViewById(R.id.driveFolderNameTextView);
        accountNameTextView = findViewById(R.id.accountNameTextView);

        String pickedDirUri = settings.getString(LOCAL_DIRECTORY_URI_CACHE_NAME, "");

        if (pickedDirUri.length() > 0) {
            pickedDir = DocumentFile.fromTreeUri(this, Uri.parse(pickedDirUri));
            if (pickedDir != null && pickedDir.getName() != null) {
                driveFolderName = pickedDir.getName();
                chosenFolderTextView.setText(PathHelper.getAbsolutePathStringFromUri(pickedDir.getUri()));
                driveFolderNameTextView.setText(String.format("Target drive folder: %s", driveFolderName));
            }
        }

        progressBar = findViewById(R.id.progressBar);
        loadingTextView = findViewById(R.id.currentFetchOperationTextView);

        progressBar.setVisibility(View.INVISIBLE);
        loadingTextView.setVisibility(View.INVISIBLE);

        pushButton.setOnClickListener(view -> {
            try {
                GoogleSignInAccount currentAccount = GoogleSignIn.getLastSignedInAccount(getApplicationContext());

                if (currentAccount == null) {
                    throw new Exception("ERROR: Please, sign in with google account");
                }

                if (pickedDir == null || pickedDir.getName() == null) {
                    throw msgHelper.getExceptionWithError("Local folder was not set");
                }

                driveFolderName = pickedDir.getName();

                if (driveFolderName.length() == 0) {
                    throw msgHelper.getExceptionWithError("Empty drive folder name");
                }

                if (!checkPermissions(REQUEST_PERMISSIONS_ON_PUSH)) {
                    return;
                }

                driveHelper.fetchData(this, pickedDir, driveFolderName).addOnSuccessListener(resFetchHelper -> {

                    fetchHelper = resFetchHelper;

                    operationType = OperationType.PUSH;

                    try {
                        if (!fetchHelper.mapsAreEqual()) {
                            Intent intent = new Intent(this, ConfirmPushActivity.class);
                            startActivity(intent);
                        }
                        else {
                            msgHelper.showToast("Everything is up-to-date");
                        }
                    } catch (Exception e) {
                        msgHelper.showToast(e.getMessage());
                        System.out.println(e.getMessage());
                        e.printStackTrace();
                    }

                    progressBar.setVisibility(View.INVISIBLE);
                    loadingTextView.setVisibility(View.INVISIBLE);
                }).addOnFailureListener(e -> {
                    msgHelper.showToast(e.getMessage());
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
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();

                progressBar.setVisibility(View.INVISIBLE);
                loadingTextView.setVisibility(View.INVISIBLE);
            }
        });

        pullButton.setOnClickListener(view -> {
            try {
                GoogleSignInAccount currentAccount = GoogleSignIn.getLastSignedInAccount(getApplicationContext());

                if (currentAccount == null) {
                    throw new Exception("ERROR: Please, sign in with google account");
                }

                if (pickedDir == null || pickedDir.getName() == null) {
                    throw msgHelper.getExceptionWithError("Local folder was not set");
                }

                driveFolderName = pickedDir.getName();

                if (driveFolderName.length() == 0) {
                    throw msgHelper.getExceptionWithError("Empty drive folder name");
                }

                if (!checkPermissions(REQUEST_PERMISSIONS_ON_PULL)) {
                    return;
                }

                driveHelper.fetchData(this, pickedDir, driveFolderName).addOnSuccessListener(resFetchHelper -> {

                    fetchHelper = resFetchHelper;

                    operationType = OperationType.PULL;

                    try {
                        if (!fetchHelper.mapsAreEqual()) {
                            Intent intent = new Intent(this, ConfirmPullActivity.class);
                            startActivity(intent);
                        }
                        else {
                            msgHelper.showToast("Everything is up-to-date");
                        }
                    } catch (Exception e) {
                        msgHelper.showToast(e.getMessage());
                        System.out.println(e.getMessage());
                        e.printStackTrace();
                    }

                    progressBar.setVisibility(View.INVISIBLE);
                    loadingTextView.setVisibility(View.INVISIBLE);
                }).addOnFailureListener(e -> {
                    msgHelper.showToast(e.getMessage());
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
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();

                progressBar.setVisibility(View.INVISIBLE);
                loadingTextView.setVisibility(View.INVISIBLE);
            }
        });

        chooseButton.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            startActivityForResult(Intent.createChooser(intent, "Choose directory"), REQUEST_CODE_CHOOSE_FOLDER);
        });

        signInButton = findViewById(R.id.signInButton);
        signInButton.setOnClickListener(view -> {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplicationContext());

            if (account == null) {
                signIn();
            }
            else {
                Toast.makeText(this, "Already signed up", Toast.LENGTH_LONG).show();
            }
        });

        logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(view -> {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplicationContext());

            if (account == null) {
                Toast.makeText(this, "Already logged out", Toast.LENGTH_LONG).show();
            }
            else {
                GoogleSignInClient googleSignInClient = buildGoogleSignInClient();
                googleSignInClient.signOut().addOnSuccessListener(unused -> {
                    accountNameTextView.setText("Current account: None");
                    Toast.makeText(getApplicationContext(),"Successfully logged out",Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getApplicationContext(), "ERROR: " + e.getMessage(),Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                });
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

        if (account == null) {
            signIn();
        }
        else {
            driveHelper = new GoogleDriveHelper(this, account, getResources().getString(R.string.app_name));
            accountNameTextView.setText(String.format("Current account: %s", account.getEmail()));
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
                case REQUEST_CODE_CHOOSE_FOLDER:
                    pickedDir = localHelper.getFileFromUri(resultData.getData());
                    driveFolderName = pickedDir.getName();
                    chosenFolderTextView.setText(PathHelper.getAbsolutePathStringFromUri(pickedDir.getUri()));
                    driveFolderNameTextView.setText(String.format("Target drive folder: %s", driveFolderName));

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
                    accountNameTextView.setText(String.format("Current account: %s", account.getEmail()));
                    msgHelper.showToast("Sign up was successful");
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    System.out.println(e.getMessage());
                    msgHelper.showToast("ERROR: Sign up was not finished");
                });
    }
}