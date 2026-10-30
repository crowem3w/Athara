# Architecture

## Layout

```
app/                    Shell, shared by ALL account types: splash, onboarding + Terms (social.ahtara.app.onboarding),
                        account type step (social.ahtara.app.accounttype), app theme + account dialog
                        (social.ahtara.app.ui.theme / .ui.components), navigation, home UI.
                        Only module that sees every other module.
personal/               Everything for the Personal account type
  registration/         Personal sign-up + setup flow (Gradle path :personal)
  features/
    authentication/     Auth domain, API client (net/), backend repository, sessions, shared auth UI
    account/  location/  socialfeed/  travel/
business/
  registration/         Business sign-up + setup flow (Gradle path :business), copied from Personal for now
organization/           Empty: to be implemented
```

Gradle paths are flat (`:personal`, `:location`, ...); `settings.gradle.kts` maps them to folders.
Package roots are `social.ahtara.<module>` (e.g. `social.ahtara.location`, `social.ahtara.app.ui`).

**Business** has its own module (`business/registration`) and database (`BusinessDatabase`); its setup UI
(`BusinessSetupFields`), data (`BusinessSetup`, `Authentication.saveBusinessSetup`) and home (`BusinessHomeScreen`) are based on Personal's.
The Business home drops the travel-feed parts (feed, destination search, category filters, Transport and
Places tabs), has placeholder `BusinessTabs` to be replaced, and swaps the Create button for a QR scanner
button (`BottomNavBar(centerIcon = ...)`); the scan action itself is not implemented yet. **Organization is still a stub**: `AccountType` keeps the
`ORGANIZATION` entry, but `available` does not offer it, `AccountStores.daoOf` throws `TODO` for it, and
no registration module or database exists yet.

The account type picker (`AccountTypeScreen`, `AccountTypeShape`, `AccountTypeBadge`) stays in
`:authentication`: the sign-up flow and `:account` also use it, and they cannot depend on `:app`.
Only the pre-Home step wrapper (`AccountTypeStep`) lives in `app/`.

The feed (`:socialfeed`) has no theme of its own: `app` passes it `FeedColors` and an `onRequireAccount`
callback (see `HomeScreen`). Feature modules must never depend on `app`, so anything a feature needs
from the shell comes in as a parameter.

## Backend

The client talks only to the Ahtara gateway (`backend/`, FastAPI behind nginx) under `/api/v1`.
Base URL: Gradle property `ahtara.apiBaseUrl` (debug, default `http://10.0.2.2:8080/api/v1` = host machine from
the emulator; use `http://<LAN-IP>:8080/api/v1` on a phone) and `ahtara.apiBaseUrlRelease` (release, HTTPS).
Both land in `BuildConfig.API_BASE_URL` of `:authentication`. Debug builds allow cleartext HTTP
(`app/src/debug/AndroidManifest.xml`); release builds do not.

| Concern | Code | Endpoints |
|---|---|---|
| HTTP, JSON errors, token refresh | `authentication/net/ApiClient`, `TokenStore` | `POST /auth/refresh` (rotating; one retry on 401) |
| Sign-up/in, Google, reset, delete/discard | `data/RemoteAuthRepository` | `/auth/signup`, `/signin`, `/social`, `/password/reset/*`, `/account/discard`, `/signout` |
| Profile, places, setup | `RemoteAuthRepository` | `GET/PATCH /users/me`, `PUT /users/me/places`, `POST /users/me/setup/complete` |
| Photos | `ImageUpload` + `ApiClient.uploadImage` | `POST /media/images?kind=profile\|cover`, then `PATCH /users/me {photo_media_id}` |
| Home feed, like, save | `socialfeed/feed/FeedApi`, `SocialFeedState` | `GET /feed`, `PUT/DELETE /feed/posts/{id}/like`, `/save` |
| Phone OTP (optional) | `data/RemotePhoneVerifier` | `/auth/otp/send`, `/auth/otp/verify` |

- The server is the source of truth for accounts. There is no local user database any more (Room/KSP removed);
  identifier/username/email uniqueness across account types is enforced by the `auth` service.
- `AuthUser.photoUrl` / `coverUrl` are absolute URLs of `/media/{id}/content` (loaded with Coil).
- The signed-in profile is cached in `SessionManager`/`SessionStore` (SharedPreferences, one file per account type)
  so the app opens offline; it is refreshed after sign-in and every profile write, and on app start.
  The `setup pending` marker mirrors `setup_pending` from `/users/me`.
- Tokens live in `authentication_tokens` prefs (not encrypted yet; see follow-ups).
- If a refresh token is rejected, `ApiClient.onSessionExpired` signs the user out locally.
- Password reset needs a code: **Send code** calls `/auth/password/reset/request`; the code comes from your SMS
  webhook / SMTP (in dev the auth service logs it).
- Travel content (Overpass, Wikivoyage, Wikidata, OpenTripMap, website scraping) was removed from the client;
  `:travel` now only holds `PlaceCategory`. The backend `/travel/*` endpoints are not used by the app yet.

## Session and device state

- `SessionManager`: one account signed in at a time. Each type's cached profile and setup-pending
  marker live in their own prefs file (`authentication_session_<type>`); `authentication_active`
  records which type is active. Saving a session wipes the other types' files; sign-out wipes the
  active one.
- `DeviceFlowStore`: pre-Home choices (account type, places, step done). Device-level, so they work
  signed out and survive sign-out. The Business address is account-specific and is cleared on
  sign-out / discard.
- `Authentication` is a thin facade over the repository and `DeviceFlowStore`; its public API is
  unchanged (`resetPassword` gained a `code`, and `AuthUser.photoPath/coverPath` became `photoUrl/coverUrl`).

## Adding an account type

1. Enum entry already exists for ORGANIZATION; add it to `AccountType.available`.
2. Enable the type in the backend (`AVAILABLE_TYPES` in `auth/app/schemas.py`).
3. Create the `organization/` module (copy `business/registration`) with its registration activity (class name must
   match `AccountType.activityClassName`) and register it in `settings.gradle.kts` and
   `app/build.gradle.kts`. Also revisit `BlankHomeScreen` in `app/`, the placeholder home these types get.

## Where to look when something breaks

| Symptom                              | Start here                                              |
|--------------------------------------|---------------------------------------------------------|
| Sign-in/sign-up result wrong         | `RemoteAuthRepository` -> `AuthError` mapping, gateway logs |
| Signed out unexpectedly              | `ApiClient.refresh` (refresh token rejected/revoked)    |
| Setup screen flow for one type       | `<type>/...RegistrationActivity`               |
| Session / "stuck on setup"           | `SessionManager` / `SessionStore` (setup-pending id)    |
| Map/place search                     | `features/location` (`SearchPipeline`, `NominatimClient`) |
| Feed content                         | `features/socialfeed/feed/FeedApi`, backend `feed` service |

## Known follow-ups (intentionally not changed in this pass)

- `AccountType` still carries UI resources and activity class-name strings; move to a UI-side mapper.
- Failures are swallowed in places (`runCatching{}.getOrNull()`, Boolean returns, `AuthError.UNKNOWN`);
  add a shared error type + logging in `app`.
- `Authentication` is still a singleton service locator (now only a facade); consider DI.
- Tokens are stored in plain SharedPreferences; move them to Android Keystore-backed storage before release.
- Creating posts, comments and the Saved tab have endpoints but no UI yet.
- Location search still calls Nominatim directly (`:location`); consider proxying through the backend.
- No tests exist; `RemoteAuthRepository` (with a fake `ApiClient`) and `FeedApi.timeAgo` are the first candidates.
