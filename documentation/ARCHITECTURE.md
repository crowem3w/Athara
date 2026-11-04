# Client architecture

Layout and responsibilities of each layer are described in `MODULE_STRUCTURE.txt`.
Gradle project names are flat (`:network`, `:personal`, ...); `settings.gradle.kts` maps each to its directory.

| Layer | Directory | Gradle project | Package |
|---|---|---|---|
| core | `src/core/secure-storage` | `:secure-storage` | `org.example.core.securestorage` |
| core | `src/core/network` | `:network` | `org.example.core.network` |
| core | `src/core/sync-engine` | `:sync-engine` | `org.example.core.syncengine` |
| core | `src/core/identity-api` | `:identity-api` | `org.example.core.identityapi` |
| shared | `src/shared/design-system` | `:design-system` | `org.example.shared.designsystem` |
| shared | `src/shared/authentication` | `:authentication` | `org.example.shared.authentication` |
| modules | `src/modules/personal` | `:personal` | `org.example.modules.personal` |
| modules | `src/modules/business` | `:business` | `org.example.modules.business` |
| modules | `src/modules/organization` | `:organization` | `org.example.modules.organization` |
| app | `src/app/application` | `:app` | `org.example.app` |
| app | `src/app/onboarding` | `:onboarding` | `org.example.app.onboarding` |

## Dependency rule

`app` -> `modules` -> `shared` -> `core`. Nothing depends upward, and the three account modules never depend on each other.

## Unchanged on purpose

- `applicationId` stays `org.example.test` (and `.staging`): changing it breaks installs and the Google OAuth Android client.
- The Keystore alias `org.example.core.secure_store.v1` stays, or stored tokens become undecryptable.

## Known deviations from the layer descriptions

- `core/identity-api` contains `AccountType` (Personal / Business / Organization), which is account-specific; `shared/design-system` and `shared/authentication` use it too.
- `shared/authentication` hosts the account-type selection and sign-up flow, so it knows every account type.

## Assets

Binary images (webp, png) live in `assets/images/<module>/`, grouped by resource type and then by owning module (see `assets/README.md`). Each module with images adds its folder through `androidComponents { onVariants { ... addStaticSourceDirectory(...) } }` in its `build.gradle.kts` (`identity-api`, `authentication`, `onboarding`). Vector icons, strings, colors and themes stay in each module's `src/main/res`.
