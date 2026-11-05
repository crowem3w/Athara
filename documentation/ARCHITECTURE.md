# Client architecture

Layout and responsibilities of each layer are described in `MODULE_STRUCTURE.txt`.
Gradle project names are flat (`:network`, `:personal`, ...); `settings.gradle.kts` maps each to its directory.

| Layer | Directory | Gradle project | Package |
|---|---|---|---|
| core | `src/core/secure-storage` | `:secure-storage` | `soc.athara.app.core.securestorage` |
| core | `src/core/network` | `:network` | `soc.athara.app.core.network` |
| core | `src/core/sync-engine` | `:sync-engine` | `soc.athara.app.core.syncengine` |
| core | `src/core/identity-api` | `:identity-api` | `soc.athara.app.core.identityapi` |
| shared | `src/shared/design-system` | `:design-system` | `soc.athara.app.shared.designsystem` |
| shared | `src/shared/authentication` | `:authentication` | `soc.athara.app.shared.authentication` |
| modules | `src/modules/personal` | `:personal` | `soc.athara.app.modules.personal` |
| modules | `src/modules/business` | `:business` | `soc.athara.app.modules.business` |
| modules | `src/modules/organization` | `:organization` | `soc.athara.app.modules.organization` |
| app | `src/app/application` | `:app` | `soc.athara.app.app` |
| app | `src/app/onboarding` | `:onboarding` | `soc.athara.app.app.onboarding` |

## Dependency rule

`app` -> `modules` -> `shared` -> `core`. Nothing depends upward, and the three account modules never depend on each other.

## Launch flow

```
splash (system) -> onboarding (first launch only) -> account type -> Home of that type (guest)
                                                                       \-> avatar / Log in -> login or sign-up -> Home (signed in)
```

`MainActivity` (app layer) only routes: onboarding if not completed, otherwise the signed-in user's own Home, otherwise the account type screen.
The account type screen (`AccountTypeActivity`) and login / sign-up (`AuthenticationActivity`) live in `:authentication`.
Each account module owns the Home of its type, guest and signed-in (`PerRegForm`, `BusRegForm`, `OrgRegForm`) and starts login through
`AuthenticationContract` (in `:identity-api`), so the three modules never depend on each other. Result code `RESULT_OK` from an account activity means the session
changed and `MainActivity` routes again; any other result means the user left.

## Personal home

`:personal` owns the home screen (`home/`: `HomeScreen`, `CollapsingHeaderState`, `components/`) and the screen state in `PersonalHomeScreen`.
Personal's place models (`...personal.features.travel`) and social feed (`...personal.features.socialfeed`: UI, state, `FeedRepository`) live inside `:personal` as `internal` code. Feed data currently comes from `SampleFeedRepository`. Note: `:personal` may not depend on `:network` (see ISOLATION_AND_SHARING_GUIDELINES.md), so a backend-backed `FeedRepository` needs a decision first. Brand colours, typography and `AtharaTheme`
live in `:design-system` (`soc.athara.app.shared.designsystem.theme`).

## Unchanged on purpose

- `applicationId` is `soc.athara.app` (staging: `.staging`). Re-register the Google OAuth Android client for the new package.
- The Keystore alias was renamed with the package; tokens stored by earlier builds will not decrypt (reinstall or sign in again).

## Known deviations from the layer descriptions

- `core/identity-api` contains `AccountType` (Personal / Business / Organization), which is account-specific; `shared/design-system` and `shared/authentication` use it too.
- `shared/authentication` hosts the account-type selection and sign-up flow, so it knows every account type.

## Assets

Binary images (webp, png) live in `assets/images/<module>/`, grouped by resource type and then by owning module (see `assets/README.md`). Each module with images adds its folder through `androidComponents { onVariants { ... addStaticSourceDirectory(...) } }` in its `build.gradle.kts` (`identity-api`, `authentication`, `onboarding`). Vector icons, strings, colors and themes stay in each module's `src/main/res`.
