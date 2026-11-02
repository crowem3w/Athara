# Backend contract the app now expects

Base URL per build type (`app/build.gradle.kts`). JSON everywhere, UTF-8, HTTPS only (cleartext only in `debug`).
All paths below are relative to the base URL. Everything outside `/v1/auth/**` needs `Authorization: Bearer <accessToken>`.

## Errors
Any 4xx returns `{ "code": "STRING", "message": "optional", "existingType": "optional" }`. The app reads `code`:

| code | HTTP | meaning |
|---|---|---|
| `INVALID_CREDENTIALS` | 401 | wrong identifier or password (same answer for unknown user: no enumeration) |
| `IDENTIFIER_TAKEN` | 409 | mobile number already used; `existingType` = `PERSONAL`/`BUSINESS`/`ORGANIZATION` |
| `USERNAME_TAKEN` / `EMAIL_TAKEN` | 409 | |
| `ACCOUNT_NOT_FOUND` | 404 | social account not registered yet (`/social/login`), or unknown account on reset |
| `VERIFICATION_REQUIRED` | 403 | request needs a valid `phoneVerificationToken` |

A 401 on any other endpoint triggers one refresh attempt. 408, 429 and 5xx are retried by the sync engine.

## Auth
| Method + path | Request | Success |
|---|---|---|
| `POST /v1/auth/signup` | `{name, identifier?, password, accountType, details?{industry,phone,registrationId}, username?, email?, birthday?, phoneVerificationToken?}` | `200 {accessToken, refreshToken, user}` |
| `POST /v1/auth/login` | `{identifier, password}` (identifier = username, email or E.164 mobile; lowercase/trimmed by the app) | `200 {accessToken, refreshToken, user}` |
| `POST /v1/auth/social/login` | `{provider: "google", idToken}`; server verifies signature, `aud`, `exp` | `200 {accessToken, refreshToken, user}` or `404 ACCOUNT_NOT_FOUND` |
| `POST /v1/auth/social/signup` | `{provider, idToken, identifier, accountType, phoneVerificationToken?}` | `200 {accessToken, refreshToken, user}` |
| `POST /v1/auth/password/reset` | `{identifier, newPassword, phoneVerificationToken}` | `200 user` (does NOT sign in; should revoke all refresh tokens) |
| `POST /v1/auth/phone/send-code` | `{phone}` (E.164) | `204` |
| `POST /v1/auth/phone/verify` | `{phone, code}` | `200 {verificationToken}` (short-lived, single-use, bound to the number) |
| `POST /v1/auth/refresh` | `{refreshToken}` | `200 {accessToken, refreshToken, user?}`. Rotate the refresh token; a 4xx here signs the user out |
| `POST /v1/auth/logout` | `{refreshToken}` | `204` (revoke it) |

`user` = `{id, identifier?, displayName, provider, accountType, industry?, phone?, registrationId?, username?, email?, birthday?, about?, location?, photoUrl?, coverUrl?}`.
`id` is a string and becomes `ownerId` of every synced row. The server hashes passwords (argon2id/bcrypt); the app only sends them over TLS.

## Profile
| Method + path | Notes |
|---|---|
| `GET /v1/me` | `user`; called on every app start (server wins) |
| `PATCH /v1/me` | `{about?, location?}` returns `user` |
| `DELETE /v1/me` | `204`; used when the user backs out of sign-up right after creating the account |
| `PUT /v1/me/photo/{kind}` | `multipart/form-data`, part `file` (JPEG), `kind` = `profile` or `cover`; returns `user` |
| `DELETE /v1/me/photo/{kind}` | returns `user`; 404 is tolerated |

`photoUrl` / `coverUrl` are downloaded with the app's HTTP client (bearer attached only if the URL is on the API host).

## Sync (server authoritative)
Resources: `customers`, `orders` (business) and `members`, `teams`, `events` (organization). Same endpoints for all.

`POST /v1/sync/{resource}/push` with `{mutations: [{clientId, serverId?, baseVersion, op: "upsert"|"delete", data?}]}` (max 50).
Respond `{results: [{clientId, status, record?}]}` with one result per mutation, in any order:
- `applied`: change accepted. Return `record` (needed for upserts: it carries `serverId` and `version`).
- `conflict`: the server kept its own state (`baseVersion` was stale). Return the current `record`; the app overwrites its local copy.
- `rejected`: invalid change. App drops it and re-pulls.
- `gone`: record no longer exists. App removes it locally.

`clientId` is a UUID and the idempotency key: a repeated push of the same `clientId` must return the same result, not create a duplicate.
`record` = `{serverId, clientId?, version, deleted, data}`; `version` increases on every server-side change.

`GET /v1/sync/{resource}?since=<cursor>&limit=200` returns `{records: [record], cursor, hasMore}`.
Deleted records come back with `deleted: true` (tombstones must be kept long enough for every device to see them). `since` absent = full download.
Cursor is opaque to the app; the same cursor must always continue where it left off.

`data` fields: customers `{name}`; orders `{title, customerName?, amountCents, createdAt}`; members `{name, role?}`; teams `{name}`; events `{title, date}` (`yyyy-MM-dd`).
