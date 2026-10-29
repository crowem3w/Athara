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
    authentication/     Auth domain, sessions, per-account-type data layer, shared auth UI
    account/  location/  socialfeed/  travel/
business/               Empty: to be implemented
organization/           Empty: to be implemented
```

Gradle paths are flat (`:personal`, `:location`, ...); `settings.gradle.kts` maps them to folders.
Package roots are `social.ahtara.<module>` (e.g. `social.ahtara.location`, `social.ahtara.app.ui`).

**Business / Organization are stubs.** `AccountType` keeps the `BUSINESS` and `ORGANIZATION` entries
(strings, colors, icons, illustrations), but `AccountType.available` only offers `PERSONAL`,
`AccountStores.daoOf` throws `TODO` for them, and no registration module or database exists yet.

The account type picker (`AccountTypeScreen`, `AccountTypeShape`, `AccountTypeBadge`) stays in
`:authentication`: the sign-up flow and `:account` also use it, and they cannot depend on `:app`.
Only the pre-Home step wrapper (`AccountTypeStep`) lives in `app/`.

The feed (`:socialfeed`) has no theme of its own: `app` passes it `FeedColors` and an `onRequireAccount`
callback (see `HomeScreen`). Feature modules must never depend on `app`, so anything a feature needs
from the shell comes in as a parameter.

## Account types and their data

Each account type has its **own Room database file**, so a schema change or corruption in one
cannot touch the others:

| Type         | Database class         | File                               |
|--------------|------------------------|------------------------------------|
| PERSONAL     | `PersonalDatabase`     | `authentication_personal.db`       |
| BUSINESS     | `BusinessDatabase`     | `authentication_business.db`       |
| ORGANIZATION | `OrganizationDatabase` | `authentication_organization.db`   |

Code: `personal/features/authentication/.../data/{personal}/` (business / organization: not yet created).

- `AccountStores` is the single router from `AccountType` to its DAO (`of(type)`). Nothing else
  picks a database, and there is no "all DAOs" accessor.
- Identifier / username / email stay unique across all types, so sign-in and conflict checks must
  look in every store. That is confined to `AccountDirectory` (read-only lookups). Once a user is
  located, every write goes through `AccountStores.of(user.accountType)`.
- User ids are globally unique (`UserIdGenerator`). They key photos, the session and website
  ownership, so they must not collide across databases.
- **Upgrade path:** `LegacyAuthImporter` copies users from the old `authentication.db` into the
  right per-type DB (ids preserved) and keeps the old file as `authentication.db.migrated`.
  Remove it and `data/legacy/` once all installs have upgraded.
- Type-specific tables go in that type's `*Database` (bump its version, add a migration there).

## Session and device state

- `SessionManager`: one account signed in at a time. Each type's cached profile and setup-pending
  marker live in their own prefs file (`authentication_session_<type>`); `authentication_active`
  records which type is active. Saving a session wipes the other types' files; sign-out wipes the
  active one. The old single `authentication_session` file is migrated on first run.
- `DeviceFlowStore`: pre-Home choices (account type, places, step done). Device-level, so they work
  signed out and survive sign-out. The Business address is account-specific and is cleared on
  sign-out / discard.
- `Authentication` is a thin facade over the repository and `DeviceFlowStore`; its public API is
  unchanged.

## Adding an account type

1. Enum entry already exists for BUSINESS / ORGANIZATION; add it to `AccountType.available`.
2. Add `<Type>Database` in `data/<type>/` and replace the `TODO` branch in `AccountStores.daoOf`.
3. Create the `business/` or `organization/` module with its registration activity (class name must
   match `AccountType.activityClassName`) and register it in `settings.gradle.kts` and
   `app/build.gradle.kts`. Also revisit `BlankHomeScreen` in `app/`, the placeholder home these types get.

## Where to look when something breaks

| Symptom                              | Start here                                              |
|--------------------------------------|---------------------------------------------------------|
| Sign-in/sign-up result wrong         | `LocalAuthRepository` -> `AuthError` mapping            |
| Wrong/missing user after upgrade     | `LegacyAuthImporter`                                    |
| Setup screen flow for one type       | `<type>/...RegistrationActivity`               |
| Session / "stuck on setup"           | `SessionManager` / `SessionStore` (setup-pending id)    |
| Map/place search                     | `features/location` (`SearchPipeline`, `NominatimClient`) |
| Feed content                         | `features/socialfeed`, `features/travel/source/*`       |

## Known follow-ups (intentionally not changed in this pass)

- `AccountType` still carries UI resources and activity class-name strings; move to a UI-side mapper.
- Failures are swallowed in places (`runCatching{}.getOrNull()`, Boolean returns, `AuthError.UNKNOWN`);
  add a shared error type + logging in `app`.
- `Authentication` is still a singleton service locator (now only a facade); consider DI.
- Profile photos still live in one shared `profile_photos` folder; moving them per type needs a file + DB path migration.
- The session caches the full profile; storing only (type, id) and reloading from the DB would shrink it.
- No tests exist; `AccountStores` / `LocalAuthRepository` are the first candidates.
