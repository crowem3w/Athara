# Client → backend migration

The client now talks to the Ahtara backend. Local Room data stays as an **offline cache**.

## What changed
| Area | Change |
|---|---|
| `:network` (new) | `ApiClient` (JSON/multipart, bearer auth, single-flight token refresh), `TokenStore`, `ApiConfig` |
| `:authentication` | `RemoteAuthRepository` replaces `LocalAuthRepository` as the default (`Authentication.createDefault`). `AuthError.NETWORK` added. `SocialAccount.idToken` carries the Google ID token |
| `:socialfeed` | `SocialFeedState` loads/pages the feed, optimistic like/save, `createPost` |
| `:travel` | `Remote*Source` classes (same interfaces as the direct Overpass/Wikivoyage/Wikidata sources) |
| `:app` | `network_security_config.xml`: cleartext HTTP allowed for 127.0.0.1 / localhost / 10.0.2.2 only |

## Backend URL
Default `http://127.0.0.1:8080` (Termux gateway on the same phone). Change without rebuilding:
`ApiConfig.setBaseUrl(context, "http://192.168.1.20:8080")` (and add that host to `network_security_config.xml`).
Kill switch back to fully local: `ApiConfig.setBackendEnabled(context, false)`.

## Legacy accounts (old local Room users)
Imported lazily on first sign-in after the update. If the server rejects the credentials but the
password matches the local PBKDF2 hash, the client creates the account on the backend with that
password, pushes the profile, re-keys cached photos to the new backend id and replaces the old local row.
A pre-update signed-in session has no tokens, so the user is signed out once and signs in again.
Accounts whose data the backend rejects (e.g. an invalid username) fail with a generic error.

## Not done / known gaps
- **Forgot password**: backend needs an emailed/SMS code; the screen has no code step. `resetPassword` returns an error for now.
- **Photos**: uploads are best-effort and retried at sign-in/app start; they fail harmlessly until object storage exists. Photos are shown from the local cache only (no download of `photo_url` on a fresh install).
- **Feed UI hooks**: `toggleLike/toggleSave/createPost/loadMore` exist on `SocialFeedState`; wire them to `PostCard`/`CreateTab` where those are still stubs.
- **Travel UI**: the `Remote*Source` classes are not instantiated anywhere yet (no screen consumes the direct sources in this snapshot).
- Offline: opening the app signed-in works from cache; sign-in and sign-up need the server.
- Tokens are in plain SharedPreferences (testing only).
- Not compiled or run: no Android SDK/Gradle in the sandbox. Expect to fix small compile errors on the first build.
