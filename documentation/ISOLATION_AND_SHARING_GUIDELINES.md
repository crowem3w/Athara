# Client Module Isolation and Sharing (instructions for AI coding assistants)

Applies to the Android client in `client/`. Follow these rules whenever you add or change client code. If a task cannot be done within them, stop and ask the user instead of bending a rule.

## 1. Dependency direction

```
app  ->  modules  ->  shared  ->  core
```

- Depend downward only. Never add an upward dependency.
- Only `:app` may depend on every feature module.

## 2. Module map

| Layer | Gradle projects | Role |
|---|---|---|
| core | `:secure-storage`, `:network`, `:sync-engine`, `:identity-api` | Infrastructure. No account-specific rules |
| shared | `:design-system`, `:authentication`, `:payments` | Reusable across account types |
| modules | `:personal`, `:business`, `:organization` | **Isolated** account features |
| app | `:app`, `:onboarding` | Composition root and first-run flow |

## 3. Allowed `project(...)` dependencies

Check this table before adding or editing any `implementation(project(...))` or `api(project(...))` line.

| Module | May depend on |
|---|---|
| `:app` | network, sync-engine, identity-api, onboarding, authentication, personal, business, organization |
| `:business` | identity-api, design-system, authentication, payments, network, sync-engine |
| `:organization` | identity-api, design-system, authentication, network, sync-engine |
| `:personal` | identity-api, design-system, authentication, payments |
| `:authentication` | identity-api, design-system, network, secure-storage |
| `:payments` | network |
| `:design-system` | identity-api |
| `:sync-engine` | network |
| `:network` | secure-storage |
| `:identity-api`, `:secure-storage`, `:onboarding` | nothing |

Do not add a dependency that is not in this table without asking the user.

## 4. Isolation rules (NEVER / ALWAYS)

- NEVER make `:personal`, `:business` and `:organization` depend on each other, in either direction. Never import another feature's classes.
- NEVER share a Room database between modules. Each feature that stores data owns its own database.
- NEVER merge `:business` and `:organization`. They share only the sync pattern, which lives in `core`.
- ALWAYS keep feature internals `internal`: entities, DAOs, ViewModels, screens, API interfaces. The only public surface of a feature is its entry activity and its `SyncInstaller` object.
- ALWAYS get the signed-in user in a feature through `Identity` (`identity-api`), never through `Authentication`.
- From `:authentication`, a feature may use only `AccountSignUp`, `AccountSetupScreen` and `PersonalSetupFields`. Do not import anything else from it.
- ALWAYS let `:app` reach a feature by class name (`AccountType.activityClassName` + `Intent.setClassName`), not by importing its activity.
- ALWAYS register feature sync through a `SyncInstaller`. `:app` only holds a list of installers. Do not add feature-specific sync code to `:app`.

## 5. Shared code rules

| Module | Holds | Constraints |
|---|---|---|
| `design-system` | `AppTheme`, `AccountTypeBadge`, `ProfileHeader`, `ImageDecoding` | UI used by 2+ modules. Keep generic |
| `authentication` | Login, sign-up, setup UI, `RemoteAuthRepository`, `AuthApi`, Google sign-in, photos | Features use only the three UI pieces in section 4 |
| `payments` | `Payments` (tap-to-pay + wallet API client), `PaymentSession`, `PaymentResult` | Server contract only, no UI. Personal pays and Business collects, but each module keeps its own screens and flow |
| `identity-api` | `Identity`, `AuthRepository`, `AuthUser`, `AccountType`, `PhoneVerifier` | Contracts and data classes only. No Compose UI, no network code |
| `network` | `Core`, `apiCall`/`ApiResult`, interceptor, token refresh, `SyncApi` | No feature-specific endpoints |
| `sync-engine` | `SyncAdapter`, `TableSync`, `SyncCoordinator`, `SyncInstaller` | Generic. Not tied to any one table |
| `secure-storage` | `SecureStore`, `TokenStore`, Keystore cipher | No dependencies |

- `core` must not depend on `shared`, `modules` or `app`, and must stay free of account-specific rules.
- `shared` must not depend on `modules` or `app`, and must stay generic.
- Use `api(...)` only when a module's types are part of its public surface. Existing cases: `network` exposes `secure-storage`, `sync-engine` exposes `network`, `design-system` exposes `identity-api`. Otherwise use `implementation(...)`.

## 6. Deciding where new code goes

Work through these in order and stop at the first match.

1. Specific to one account type? Put it in that account's module.
2. Used by only one module? Keep it in that module, even if it looks reusable.
3. Needed by two or more modules and generic?
   - UI or theme: `design-system`.
   - Login, sign-up, setup, profile photos: `authentication`.
   - A contract about the current user: `identity-api`.
   - HTTP plumbing or error mapping: `network`.
   - Generic sync logic: `sync-engine`.
   - Secrets or tokens: `secure-storage`.
4. A feature-specific API interface? Put it in the owning module. Never in `network`.
5. App wiring, lifecycle, build types, routing between accounts? `app`.
6. Needed by two account modules but account-specific in meaning? Do not import across modules and do not push it into `shared`. Ask the user whether to duplicate it or generalise it.

## 7. Known leaks: work with them as they are, do not extend them

These exist today. Do not fix them unless the user asks, and do not add more of the same.

- `AccountType` lives in `core/identity-api` although it is account-specific.
- `shared/authentication` knows every account type, because it hosts account selection and sign-up.
- Do NOT add new account-specific logic, resources or branching to `identity-api`, `design-system` or `authentication`, beyond what an `AccountType` entry strictly requires.
- Adding a new account type necessarily touches `AccountType`, `settings.gradle.kts`, `:app`'s dependencies, and any account-type branching in `authentication`. If the user asks for a new account type, tell them this up front.

## 8. Stop and ask the user if

- The task needs a dependency that is not in the section 3 table.
- The task needs two account modules to share account-specific code.
- The task would add account-specific logic to `core` or `shared`.
- The task would put a feature-specific endpoint or table into `network` or `sync-engine`.

## 9. Self-check before you finish

Review your own diff against this list. This is a manual check; no automated enforcement exists.

- [ ] No dependency between `personal`, `business` and `organization`.
- [ ] No upward dependency.
- [ ] Every changed `project(...)` line is allowed by section 3.
- [ ] No new use of `:authentication` beyond the three allowed UI pieces.
- [ ] Feature code reads the user via `Identity`.
- [ ] Code used by only one module stayed in that module.
- [ ] Nothing feature-specific was added to `network` or `sync-engine`.
- [ ] Feature types are still `internal`.
- [ ] No new account-specific logic in `identity-api`, `design-system` or `authentication`.
