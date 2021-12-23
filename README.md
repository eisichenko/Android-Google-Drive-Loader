# Android-Google-Drive-Loader

- Android application for pushing your local folder on phone to Google Drive folder and pulling from it.

- Credentials you can get here https://console.cloud.google.com/apis/credentials.

- Add target user to `test users` in OAuth consent screen tab to provide access to your drive.

- Enabled `day`and `night` themes.

- For generating signed APKs follow the steps below:
  
  1. If you don't have OAuth client, create it and choose Android type

  2. Get your local SHA1 certificate fingerprint by command:
    
      - `keytool -list -v -keystore {keystore_name} -alias {alias_name}` for Release mode

      - `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android` for Debug mode
  
  3. Paste SHA1 key to the following field and save
