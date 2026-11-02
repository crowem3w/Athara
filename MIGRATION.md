# Migration notes: offline-only to backend-backed

Code only; no backend exists yet (see `API_CONTRACT.md` for what it must provide). **Not compiled or run**: the
environment had no Android SDK, so run `./gradlew assembleDebug` first and expect small fixes.

## What changed
| Area | Before | Now |
|---|---|---|
| Auth | `LocalAuthRepository` (Room users table, PBKDF2 on device) | `RemoteAuthRepository`: server owns accounts/passwords; app keeps tokens + a cached profile. Same `AuthRepository` interface, so UI and other modules are untouched |
| API | none | new `:core`: Retrofit/OkHttp/kotlinx.serialization, bearer interceptor (own host only), single-flight refresh-token authenticator, optional cert pinning, `ApiResult` (no exceptions at call sites) |
| Sync | none; local auto-increment ids | outbox per table: `clientId` (idempotency), `serverId`, `version`, `syncState`; tombstone deletes; push-then-pull with cursors via WorkManager. **Server wins**: returned records (applied or conflict) overwrite local rows |
| Secure storage | plain `SharedPreferences` session, unencrypted DB | Keystore AES-256-GCM `SecureStore` for tokens and the cached profile; `allowBackup=false`; photos removed on sign-out |
| Environments | one | build types: `debug` (http://10.0.2.2:8080), `staging` (`.staging` app id), `release` |

## Removed
`AuthDatabase`, `UserDao`, `UserEntity`, `PasswordHasher`, `LocalAuthRepository`, demo Apple/social accounts, the Room/KSP setup in `:authentication`.
On first launch `RemoteAuthRepository` deletes the old `authentication.db`, the plain session prefs and old photos.

## Pre-production data policy
Room DBs are bumped (`business` v3, `organization` v2) with `fallbackToDestructiveMigrationFrom` limited to the old versions, so old rows (owned by
local user ids that no longer exist) are dropped. Anything after that needs real `Migration`s. If a DB is ever wiped, call `SyncCoordinator.resetCursors()`.

## Behaviour changes to know about
- Sign-in, sign-up, password reset and the profile-setup step now need a connection (`AuthError.NETWORK` shows a message). Business/org data stays usable offline and syncs later.
- Sign-out keeps unsynced rows (keyed by owner) so they sync after the next sign-in. They are not encrypted at rest yet.
- Apple sign-in has no implementation: the button now ends in "Something went wrong".
- The OTP screens do not exist. `PhoneVerifier` was never called by the UI, and the old password reset let anyone reset any account. The backend rejects reset/sign-up
  needing proof with `VERIFICATION_REQUIRED` until the UI calls `Authentication.phoneVerifier().sendCode/verify`.
- Staging uses package `org.example.test.staging`: register a separate Android OAuth client (SHA-1 + package) for Google sign-in.

## Before release
Placeholder URLs and debug signing in `app/build.gradle.kts` (TODOs), certificate pins in `App.kt`, R8 rules (Retrofit/serialization) if you enable minify,
and Room migration tests (schemas export to `<module>/schemas`).
