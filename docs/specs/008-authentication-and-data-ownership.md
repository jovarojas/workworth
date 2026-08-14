# SPEC 008: Authentication and Data Ownership

**Status:** Approved
**Owner:** WorkWorth
**Related documentation:** [SPEC process](README.md), [Project foundation](000-project-foundation.md), [Salary](001-salary-net-estimation.md), [Workday](002-work-schedule-and-workday.md), [Earnings](003-earnings-and-earning-periods.md), [Reward affordability](004-reward-affordability.md), [Dashboard](005-dashboard.md), [Goals](006-goals.md), [Statistics](007-statistics.md)

## Objective

Protect every WorkWorth resource with an authenticated identity and make domain data private to its owner. The first production release is intentionally private: only a pre-authorized account may access the application. The backend must nevertheless support multiple users from its first authenticated release, so one user can never read or change another user's data.

## Context

WorkWorth currently has an implicit single user and no authentication. Its existing Salary, Workday, Earnings, Rewards, Goals, Dashboard, Preferences, and Statistics behavior remains functionally unchanged by this specification; authentication adds identity, authorization, and data ownership around those capabilities.

Production starts with an empty database. Local development data is not migrated to production.

Auth0 is the external identity provider. It owns credentials, email verification, password reset, login, and logout at the identity-provider level. WorkWorth owns authorization and all domain data. The application never uses an identity-provider subject directly as a foreign key throughout the domain.

## Scope

### In scope

- Auth0 email-and-password login with verified email.
- OpenID Connect and OAuth 2.0 Authorization Code flow with PKCE.
- Spring Boot as an OAuth 2.0 Resource Server validating access-token JWTs.
- A local `app_users` domain identity mapped one-to-one to the Auth0 `sub` claim.
- Private initial access through pre-provisioned, active local users; public registration is disabled.
- User ownership and repository/service scoping for persisted WorkWorth data.
- Authentication and authorization behavior for web Angular and Android Capacitor clients.
- Bearer-token session expiry, refresh, logout, CORS, and production configuration rules.

### Out of scope

- Implementation of Spring Security, Auth0 configuration, database migrations, or Angular authentication code.
- Creating an Auth0 tenant, application, account, client secret, callback URL, or production secret.
- Public sign-up, self-service invitations, roles beyond the owner of personal data, organizations, teams, or sharing.
- Social login, passwordless login, MFA, biometric login, account-management UI, or account deletion.
- Migration of existing local data into production.
- Changes to Salary, Workday, Earnings, Rewards, Goals, Dashboard, Statistics, their economic rules, or their public business DTO semantics beyond future authentication enforcement and ownership isolation.

## Functional requirements

- [ ] All functional `/api/v1/**` endpoints require a valid access token, except explicitly public operational endpoints such as health checks.
- [ ] WorkWorth accepts only JWT access tokens issued by the configured Auth0 issuer and intended for the configured WorkWorth API audience.
- [ ] The backend validates token signature, issuer, audience, expiration, and standard temporal claims before authorizing a request.
- [ ] The authenticated Auth0 `sub` resolves to exactly one active local `AppUser`; a token for an unmapped or disabled user is not authorized.
- [ ] The client never supplies a `userId` that determines ownership. The backend derives the current user only from the validated authenticated token.
- [ ] Every read, write, mutation, aggregation, and automatic reconciliation is scoped to the current local user.
- [ ] An authenticated user cannot discover, read, mutate, aggregate, or influence data owned by another user, including through IDs, dates, filters, dashboard requests, statistics, or derived data.
- [ ] The initial release authorizes only one pre-provisioned account. Auth0 authentication alone does not grant WorkWorth API access.
- [ ] Angular web and Android Capacitor use Authorization Code with PKCE through Auth0 Universal Login; neither client contains an Auth0 client secret.
- [ ] Logout clears locally held session material and ends the Auth0 browser session when the provider logout flow is available.
- [ ] Currency, time zone, and all other application settings are resolved per authenticated local user.

## Domain model and ownership

### AppUser

`AppUser` is WorkWorth's local domain identity. It is not an Auth0 user record and it stores no password, access token, refresh token, or Auth0 client secret.

| Field | Meaning |
|---|---|
| `id` | Stable internally generated UUID used by WorkWorth foreign keys. |
| `identitySubject` | Immutable, unique Auth0 OIDC `sub`; the authoritative external identity link. |
| `email` | Verified-email snapshot for operational identification only; it is not used as the token identity or ownership key. |
| `status` | `ACTIVE` or `DISABLED`; only `ACTIVE` users are authorized. |
| `createdAt` / `disabledAt` | Audit timestamps for the local authorization record. |

An `AppUser` is provisioned before it receives API access. In the initial release, provisioning is an out-of-band operational action after the account has been created and verified in Auth0. There is no public registration or application endpoint that creates an authorized user.

### Ownership roots

Only aggregate roots store the local owner foreign key. Dependent records inherit ownership from their aggregate root and must be reached through owner-scoped queries; they do not receive redundant `user_id` columns merely for convenience.

| Aggregate root | Ownership rule |
|---|---|
| `ApplicationSettings` | One settings record per `AppUser`; global singleton semantics and fixed singleton ID are removed. |
| `SalaryProfile` | Every salary basis belongs to one `AppUser`. Salary lookups and effective-date selection are owner-scoped. |
| `Workday` | Every workday belongs to one `AppUser`. A local date is unique only within that owner. |
| `Reward` | Every reward belongs to one `AppUser`; reward lists, relevance, acquisition, and combinations are owner-scoped. |
| `Goal` | Every goal belongs to one `AppUser`; active/history lists and progress resolution are owner-scoped. |

Dependent ownership is inherited as follows:

| Dependent data | Inherited owner |
|---|---|
| `WorkdayEarning`, earning corrections | Their `Workday` / `WorkdayEarning` chain. |
| Meal breaks, partial absences, workday time corrections | Their `Workday`. |
| Salary estimates | Their `SalaryProfile`. |

Dashboard motivation, Earnings periods, and Statistics do not own records. They resolve their data only through owner-scoped application services and never use global repository queries.

### Persistence constraints and queries

- Every owned root has a non-null foreign key to `app_users(id)` and an index suitable for its normal owner-scoped queries.
- The existing global uniqueness of a Workday local date becomes `UNIQUE(user_id, local_date)`.
- Existing uniqueness and lookup constraints for Salary profiles must be reviewed so that effective-date behavior is unique or ordered within one owner, never globally across all users.
- `ApplicationSettings` becomes unique by `user_id`, rather than a database-wide singleton record.
- IDs exposed by APIs do not bypass ownership: a lookup by ID must include the current user scope and return the standard not-found result when the record is not owned by that user.
- Foreign-key paths and aggregate services must reject cross-user association attempts even when a caller guesses a valid record ID.

## Authentication and authorization rules

### Access token validation

Spring Boot is an OAuth 2.0 Resource Server. For every protected request it validates the bearer JWT against Auth0's published signing keys and configured issuer and audience. It validates signature, issuer, audience, expiration, not-before time when present, and accepted token type before extracting `sub`.

The backend resolves `sub` to an active `AppUser` and creates the request's current-user context. It does not trust `userId`, email, ownership flags, or roles supplied in request bodies, query parameters, path values, or browser storage.

### HTTP outcomes

| Situation | HTTP outcome |
|---|---|
| Missing, malformed, expired, wrongly signed, wrong-issuer, or wrong-audience access token | `401 Unauthorized` with the established `ProblemDetail` format where applicable. |
| Valid token whose `sub` has no active authorized `AppUser`, or an attempted cross-user access | `403 Forbidden` without revealing whether another user's record exists. |
| Valid authorized user requesting a resource absent from their own scope | Existing not-found behavior, normally `404`, without cross-user disclosure. |

Authentication and authorization are enforced before a controller invokes application business operations. CORS is a browser-origin policy only; it is never an authorization mechanism.

### Private initial access

Auth0's database connection accepts the configured email-and-password login only for accounts provisioned by the operator and verified by Auth0. Public sign-up is disabled. WorkWorth additionally requires a matching active `AppUser`, so creating or possessing another valid Auth0 account cannot grant API access.

Future invitation or self-registration flows require a separate approved specification. They must not weaken subject-to-local-user mapping or user-data isolation.

## Session lifecycle and logout

- Access tokens are short-lived JWTs. The implementation proposal will configure an access-token lifetime of **10 minutes**.
- Refresh tokens use Auth0 refresh-token rotation with reuse detection. The implementation proposal will configure an initial idle lifetime of **30 days** and an absolute lifetime of **90 days**, subject to Auth0 plan capabilities.
- Refresh material is used only to obtain new access tokens; it is never sent to WorkWorth business endpoints.
- The web client keeps session material in the provider-supported in-memory/session mechanism and must not store refresh tokens in `localStorage` or `sessionStorage`.
- The Android client stores any refresh material only in Android Keystore-backed secure storage. Capacitor Preferences, plain files, WebView storage, logs, and URLs are not secure token stores.
- Logout clears local access/session material, clears Android secure storage where applicable, and initiates Auth0 provider logout. It revokes refresh capability when Auth0 supports the configured flow.
- Access tokens already issued cannot be recalled by client logout alone; their maximum residual validity is the configured access-token lifetime. Disabling an `AppUser` prevents subsequent API authorization regardless of a still-valid token.

## Client flows

### Angular web

1. A protected route redirects an unauthenticated user to Auth0 Universal Login.
2. Angular starts Authorization Code with PKCE using the public web client ID, its configured callback URL, and the WorkWorth API audience.
3. After Auth0 verifies the email-and-password account, it redirects to the registered Angular callback route with an authorization code.
4. The client completes the PKCE exchange through the supported Auth0 SDK, then calls the API with `Authorization: Bearer <access token>`.
5. An Angular HTTP interceptor adds the access token only to configured WorkWorth API requests. Guards prevent protected UI routes from loading before the identity state is known.
6. A `401` initiates the approved renewal/login behavior; a `403` displays an access-denied state and does not retry with altered ownership data.

### Android Capacitor

1. The app starts the same Authorization Code with PKCE flow in the system browser, not an embedded credential WebView.
2. Auth0 returns to a registered Android deep-link callback, for example `com.workworth.app://auth/callback`.
3. Capacitor resumes the app, completes the supported token flow, and sends the access token to the HTTPS API in the Authorization header.
4. Refresh material, if enabled, is kept only in Android Keystore-backed secure storage.
5. Logout clears the secure store and completes the provider logout flow. The Android callback, package identifier, signing configuration, and allowed logout URI must exactly match the Auth0 configuration.

The implementation must not put tokens in URLs, logs, analytics payloads, Capacitor Preferences, or an API base-URL configuration file.

## CORS and production configuration

- Production API traffic is HTTPS only.
- Railway configures Auth0 issuer/audience validation and existing database secrets exclusively through environment variables; no Auth0 secret is committed.
- Cloudflare Pages and the Android application receive only public configuration: Auth0 domain, public client ID, API audience, redirect URI, and HTTPS API base URL. A public client ID is not a secret.
- Separate Auth0 public applications may be registered for Angular web and Android when their callback/logout URIs differ. Both map the same authenticated subject to the same `AppUser`.
- CORS allows only the deployed Cloudflare Pages origin, approved local development origin, and Capacitor's required `https://localhost` origin. It allows the `Authorization` header and necessary methods, never wildcard production origins with credentials.
- Auth0 callback and logout URL allowlists are exact production/development URLs and the registered Android deep-link callback; arbitrary redirect URLs are prohibited.

## Responsibilities

| Backend | Angular / Capacitor |
|---|---|
| Validate access tokens and derive the current `AppUser` | Initiate the approved Auth0 login/logout flows and keep UI authentication state. |
| Enforce ownership at every repository/service boundary | Send only the bearer token supplied by the authentication layer; never send a user ID to select data. |
| Return `401`, `403`, and owner-scoped resource results | Guard routes, attach the token to WorkWorth API requests, and present access/session errors. |
| Maintain local allowlist and user-status enforcement | Use secure platform-appropriate token storage; clear it on logout. |
| Keep business and economic rules unchanged and user-scoped | Never infer authorization, ownership, currency, or economic decisions locally. |

## Use cases

### Authorized owner opens WorkWorth

**Given** a verified Auth0 account whose `sub` is mapped to an active `AppUser`
**When** the user logs in through Universal Login and calls a protected endpoint
**Then** Spring validates the JWT, resolves the local user, and returns only that user's data.

### Unknown identity attempts access

**Given** a valid Auth0 account without an active local `AppUser` mapping
**When** it calls a protected WorkWorth endpoint
**Then** the API returns `403` and exposes no WorkWorth data.

### Owner guesses another resource ID

**Given** two active local users and a record owned by the second user
**When** the first user requests or mutates the record ID
**Then** the operation is scoped to the first user and does not expose or change the second user's record.

### Owner logs out on Android

**Given** an authenticated Capacitor app with refresh material in secure storage
**When** the user logs out
**Then** the app clears secure session material, completes provider logout when available, and subsequent protected API calls require a new authenticated session.

## Acceptance criteria

- [ ] Auth0 is the sole identity provider; WorkWorth stores no password or Auth0 secret in client code.
- [ ] Web and Android authenticate through Authorization Code with PKCE and Auth0 Universal Login.
- [ ] Spring validates JWT signature, issuer, audience, expiry, and temporal validity before authorizing protected endpoints.
- [ ] All functional API operations are authenticated and derive ownership from the token's subject through an active local `AppUser`.
- [ ] Each owned aggregate root has one local owner; dependent records inherit ownership from their approved aggregate path.
- [ ] A user cannot access or mutate another user's Salary, Preferences, Workdays, Earnings, Rewards, Goals, Dashboard motivation, or Statistics.
- [ ] Workday local-date uniqueness is per user, and current global application settings are per user.
- [ ] The initial production database is empty and only the explicitly pre-provisioned active user can access it.
- [ ] Valid but unmapped/disabled identities receive `403`; invalid or absent access tokens receive `401`.
- [ ] Angular and Capacitor never calculate authorization or select data ownership; they send only a bearer token and present outcomes.
- [ ] Refresh material is never stored in web local storage, URLs, logs, or Android insecure storage.
- [ ] Production uses HTTPS, exact CORS origins, and exact Auth0 callback/logout allowlists; CORS is not treated as authentication.
- [ ] Existing economic behavior, currencies, Earnings corrections, Reward rules, Goal progress, Dashboard motivation, and Statistics calculations remain unchanged except for user scoping.

## Technical considerations

- The implementation belongs to a cross-cutting `identity` / security module plus controlled changes in the owned aggregate modules. It remains a modular monolith and preserves Controller → Service → Repository boundaries.
- Spring Security must use constructor-injected current-user and authorization services; controllers must not parse JWTs or pass raw claims into repositories.
- A future migration introduces `app_users` before non-null owner foreign keys. Since production is empty, it does not backfill local data; test fixtures must create explicit users.
- Ownership filtering must be visible in repository method signatures or an equivalent audited service boundary. Global `findById`, unscoped list, and unscoped aggregation paths are prohibited for owned data.
- Existing public DTOs should not gain a client-selectable `userId`. Authentication is transport context, not business-request input.
- The Auth0 SDK and any Android secure-storage dependency require a separate technical proposal before introduction. The proposal must verify current Angular, Capacitor, Auth0 SDK, and Android Keystore compatibility.
- Security tests must use signed test JWTs or an equivalent resource-server test configuration; production Auth0 credentials are never used in automated tests.

## Edge cases

- A valid token may identify a disabled or no-longer-allowlisted local user: return `403` without exposing data.
- A request that arrives as an access token expires returns `401`; the client may renew or require login, but it must not submit a stale mutation again automatically without preserving user intent safely.
- Two users can have workdays on the same local date; only same-user duplicates are forbidden.
- A user changing global currency remains subject to the existing per-user economic-data lock; another user's economic records do not block the change.
- A guessed ID, reward-combination exclusion ID, correction ID, or nested workday child ID belonging to another user must be ignored/rejected without cross-user disclosure.
- A background reconciliation, scheduled workday materialization, dashboard read, earnings aggregation, or statistics query must receive an explicit user scope; no background task may accidentally operate across all users without a separately approved administrative use case.
- Auth0 service outage or JWKS validation failure is an authentication infrastructure failure, not an empty or unauthenticated domain-data result.

## Expected tests

| Requirement / rule | Test level | Expected test |
|---|---|---|
| JWT validation | Backend security integration | Invalid signature, issuer, audience, expiry, and missing token return `401`. |
| Local authorization | Backend integration | Valid mapped active subject is authorized; valid unmapped or disabled subject returns `403`. |
| Root ownership | Backend integration / PostgreSQL | Two users see and mutate only their own Salary, settings, Workdays, Rewards, and Goals. |
| Dependent ownership | Backend integration | Earnings, corrections, breaks, absences, and estimates cannot be reached through another user's aggregate IDs. |
| Owner-scoped derived data | Backend integration | Earnings periods, Dashboard motivation, Statistics, Rewards relevance, and combinations contain only the current user's data. |
| Per-user uniqueness | PostgreSQL/Testcontainers | Equal `local_date` is accepted for two users and rejected for a duplicate belonging to one user. |
| Currency isolation | Backend integration | Economic-data lock and settings apply to the current user only. |
| Web authentication boundary | Angular unit | Guard/interceptor attach tokens only to WorkWorth API calls and present `401`/`403` without selecting a user ID. |
| Android secure session | Android / integration | OAuth callback resumes Capacitor; session material uses secure storage and logout clears it. |
| CORS | Backend integration | Only configured web, development, and Capacitor origins with Authorization header succeed; unapproved origins do not. |

## Traceability and verification

This specification is **Approved**. It authorizes a technical implementation proposal, not direct implementation. Before implementation, that proposal must define exact Auth0 tenant/application settings, Spring Security classes, migrations, owner-scoped repository changes, token test fixtures, Angular SDK integration, Android secure-storage dependency, redirect URIs, and rollout order.

The documented product decisions are: private initial access, one authorized pre-provisioned account, email-and-password login with verified email, empty production data, and multi-user backend ownership from the first authenticated release.
