# Google sign-in setup

Google sign-in uses Android Credential Manager. It needs two OAuth clients in the same
Google Cloud project: a **Web** client (its ID goes in the app) and an **Android** client
(it lets Google trust the app).

## 1. Create the project and consent screen
1. Open <https://console.cloud.google.com> and create or select a project.
2. **APIs & Services > OAuth consent screen**: choose *External*, fill in the app name and
   support email, and add your test users' Google accounts while the app is in *Testing*.

## 2. Get your package name and SHA-1
- Package name: the `applicationId` of the `:app` module.
- Debug SHA-1: run `./gradlew signingReport` and copy the `SHA1` of the `debug` variant.
- Release SHA-1: the SHA-1 of your release keystore (or the Play App Signing certificate in
  Play Console > Setup > App signing).

## 3. Create the two OAuth clients (**APIs & Services > Credentials > Create credentials > OAuth client ID**)
1. **Android** client: enter the package name and the SHA-1. Create one per SHA-1
   (debug and release).
2. **Web application** client: no redirect URIs are needed. Copy its **Client ID**
   (`1234567890-abc...apps.googleusercontent.com`).

## 4. Put the Web client ID in the app
Edit `authentication/src/main/res/values/google_auth.xml`:

```xml
<string name="authentication_google_web_client_id" translatable="false">
    1234567890-abc.apps.googleusercontent.com
</string>
```

While it still starts with `YOUR_`, the Google buttons show "Google sign-in isn't set up yet."

## 5. Test
- Use a device or emulator image with Google Play services and at least one Google account.
- Sign up: Personal registration > **Google** > pick an account > add a mobile number.
- Sign in: login screen > **Continue with Google** (works only for a Google account that
  already finished sign-up).

## Troubleshooting
| Symptom | Likely cause |
|---|---|
| "Google sign-in isn't set up yet" | Web client ID is still the placeholder. |
| "No Google account found" | No Google account on the device, or the account picker found none. |
| "Couldn't sign in with Google" | SHA-1 or package name does not match the Android client; wrong Web client ID; or the account is not a listed test user. |
| Works in debug, fails in release | The release / Play App Signing SHA-1 has no Android client. |

## Security note
The app reads the Google ID token on the device only. Before trusting a Google account in
production, send the token to your backend and verify it (signature, `aud` = your Web client
ID, `exp`) there. Apple sign-in is still a local demo account.
