# auth-service (Keycloak Resource Server)

Keycloak is the Identity Provider.

This service **does**:

- Validates Keycloak-issued JWTs (OAuth2 Resource Server)
- Exposes authenticated user info (`GET /auth/me`)
- Syncs the current user/roles/session into `auth_db` (`POST /auth/users/sync`)

This service **explicitly does NOT**:

- Login/signup/password/reset flows
- Token issuance or refresh-token issuance
- UI flows
- User profile domain ownership (bio, avatar, preferences, creator stats, etc.)
- Content/creator/business workflows

## Authentication model

- Keycloak issues access tokens.
- This service is a **JWT Resource Server only** (validates tokens; does not mint or introspect them).
- Downstream services MUST NOT call Keycloak directly for user info or role checks.

## Environment contract

This service **must** run with an explicit Spring profile set via `SPRING_PROFILES_ACTIVE`.

Supported profiles:

- `local` (developer machine defaults)
- `docker` (docker-compose defaults)
- `prod` (production: **no unsafe defaults**, required env vars)

Note: the `local` profile does not embed localhost URLs/passwords; it expects env vars (same names as `prod`).

### Required environment variables

Required (always):

- `SPRING_PROFILES_ACTIVE` (`local` | `docker` | `prod`)

Required (non-test deployments):

- `KEYCLOAK_ISSUER_URI` (must match token `iss` exactly)
- `SPRING_DATASOURCE_URL` (Postgres JDBC URL)
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

Optional / conditional:

- `KAFKA_BOOTSTRAP_SERVERS`
  - Required by the `prod` profile to avoid unsafe defaults.
  - Optional for local dev if you intentionally run without Kafka and accept that events cannot be delivered.
- `KEYCLOAK_JWK_SET_URI` (override only when the issuer host is not reachable from the service runtime)
- `SERVER_PORT`
- `APP_VERSION`

Docker Compose expectation:

- `SPRING_PROFILES_ACTIVE=docker`
- `SPRING_DATASOURCE_PASSWORD` (required)
- `KEYCLOAK_ISSUER_URI` (required)
- `KEYCLOAK_JWK_SET_URI` (recommended for Docker networking)

## Auth Service Responsibilities

- Source of truth for <b>user id</b> and <b>authorization roles</b> within CreatorOs.
- Owns and maintains: `users`, `roles`, `user_roles`, `login_sessions`, `refresh_tokens`.
- Emits <b>versioned</b> user lifecycle events to Kafka (fire-and-forget).
- Provides a stable REST contract under `/auth/**` for gateway and internal service calls.
- Never blocks HTTP requests when Kafka is unavailable.

## What Auth Service Does NOT Do

- No login/signup/password/reset flows.
- No token issuance or refresh-token issuance.
- No profile domain ownership (bio, avatar, preferences, creator stats, etc.).
- No content/publishing/business logic.
- No cross-service DB queries and no shared database joins.

## Auth-Service Boundaries (Do Not Violate)

- No user profile data.
- No creator/content knowledge.
- No business workflows.
- No frontend session management.
- No password handling outside Keycloak.

## Downstream Service Expectations

- Treat auth-service events as the integration boundary; do not query auth-service tables.
- Use the user id (`sub`) as the stable primary key across services.
- Cache roles locally if needed, but be prepared to handle role updates via events.
- <b>No cross-service DB joins allowed.</b>

## Local stack (docker compose)

### Environment file (.env)

This compose setup reads configuration from a local `.env` file (ignored by git) so credentials/URLs/ports are not hard-coded in `docker-compose.yml`.

1. Copy `.env.example` to `.env`
2. Fill in values (dev defaults are fine)
3. Run compose from this folder:

```bash
docker compose up --build
```

### Keycloak realm bootstrap (important for new machines)

This repo includes a dev-only Keycloak realm import at `keycloak/import/creatoros-realm.json`.

When you run `docker compose up` in this folder, the Keycloak container starts with `--import-realm` and will automatically create:

- Realm: `creatorOs`
- OIDC client: `auth-service` (public client)

So on a fresh PC, you should **not** need to manually create the realm/client in the Keycloak admin console.

Note: users are still created by self-registration in the Keycloak UI (no default users are shipped).

Default ports from `docker-compose.yml`:

- Keycloak UI: `http://localhost:8081`
- auth-service API: `http://localhost:8082` (mapped from container port 8080 to avoid common local conflicts)
- auth-db (Postgres): `localhost:5433`
- Kafka broker (DEV only): `localhost:9092`

### Swagger / OpenAPI (DEV only)

Enabled in `local` and `docker` profiles.

- Swagger UI: `http://localhost:8082/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8082/v3/api-docs`

How to call protected endpoints from Swagger:

1. Open Swagger UI
2. Click **Authorize**
3. Paste the **JWT access token** (the raw `eyJ...` string, not a user id)
4. Click **Authorize** and call `/auth/me`

Note: Swagger is disabled by default in `prod` (can be enabled via env vars):

- `SPRINGDOC_API_DOCS_ENABLED=true`
- `SPRINGDOC_SWAGGER_UI_ENABLED=true`

### Get a Keycloak access token (PowerShell, password grant)

Keycloak runs on `http://localhost:8081` in docker-compose (host `8080` may be used by other tools like Jenkins).

```powershell
$KeycloakBaseUrl = "http://localhost:8081"
$Realm           = "creatorOs"
$ClientId        = "auth-service"
$ClientSecret    = ""   # set only if client is Confidential
$Username        = "<username>"
$Password        = "<password>"

$tokenUrl = "$KeycloakBaseUrl/realms/$Realm/protocol/openid-connect/token"

$body = @{ grant_type = "password"; client_id = $ClientId; username = $Username; password = $Password }
if ($ClientSecret) { $body.client_secret = $ClientSecret }

$r = Invoke-WebRequest -Method Post -Uri $tokenUrl -ContentType "application/x-www-form-urlencoded" -Body $body -SkipHttpErrorCheck
"HTTP $($r.StatusCode)"; $r.Content
if ($r.StatusCode -ne 200) { throw "Token request failed (see response above)." }

($r.Content | ConvertFrom-Json).access_token | Set-Clipboard
"Copied access_token to clipboard."
```

If Keycloak returns errors:

- `unauthorized_client`: enable **Direct Access Grants** on the Keycloak client
- `invalid_client`: set `$ClientSecret` (client is Confidential)
- `invalid_grant`: wrong username/password or user is disabled

## Endpoints

Public (no token):

- `GET /actuator/health`
- `GET /actuator/info`

Requires `Authorization: Bearer <JWT>`:

External (gateway-facing):

- `GET /auth/me`
- `POST /auth/users/sync`
- `POST /auth/logout`

Internal (service-to-service):

- `GET /auth/users/{userId}/roles`

Expected behavior without a token:

- Public endpoints: **200**
- Any `/auth/**` endpoint: **401**

## Consumer guidance (VERY IMPORTANT)

### How other services should authenticate requests

- Services should validate `Authorization: Bearer <JWT>` locally (resource server), using the same Keycloak realm issuer as the platform standard.
- Use `sub` as the stable user id across all services.
- Do not forward or log tokens.

### How services should authorize users (roles vs scopes)

- Prefer **roles** for application authorization decisions (e.g., `admin`, `creator`).
- Treat **scopes** (if present) as coarse-grained OAuth permissions, not your application roles.
- This auth-service reads roles only from:
  - `realm_access.roles`
  - `resource_access[azp].roles` (client roles for the token’s `azp` client-id)

### Token validation requirements (issuer, audience)

- **Issuer** (`iss`) must match your service configuration exactly.
- **Audience** (`aud`) validation is deployment-specific in Keycloak; if your realm uses audience mappers, enforce an allow-list.
  - auth-service supports an optional allow-list via `creatoros.security.jwt.audiences` (leave empty to disable).

### Clear rule: services MUST NOT call Keycloak directly

- Downstream services MUST NOT call Keycloak for user info, role checks, or token introspection.
- Keycloak is a platform dependency owned by the auth/platform team.
- Services must rely on:
  - JWT validation + claims (for request-time authN/authZ)
  - auth-service Kafka events (for local projections/caches)
  - auth-service REST endpoints only when explicitly documented as internal contracts

## Token expectations

JWT requirements:

- `iss` (issuer) MUST exactly match `KEYCLOAK_ISSUER_URI` / `spring.security.oauth2.resourceserver.jwt.issuer-uri`.
- `sub` is treated as the stable user id.
- `aud` is validated only if an allow-list is configured via `creatoros.security.jwt.audiences`.

### Role claims used

This service reads roles only from:

- `realm_access.roles`
- `resource_access[azp].roles` (where `azp` is the token "authorized party" client-id)

It will **not** consume roles for other Keycloak clients under `resource_access`.

## Role model (realm roles vs client roles)

- Realm roles are global to the realm and appear under `realm_access.roles`.
- Client roles are scoped to a Keycloak client and appear under `resource_access[clientId].roles`.
- auth-service only accepts client roles for the token’s `azp` client-id to avoid role leakage from other clients.

Note for local Docker testing:

- Tokens are usually issued with `iss` like `http://localhost:8081/realms/<realm>`.
- auth-service validates that issuer, but fetches signing keys (JWKs) from the in-Docker Keycloak URL.

## Local development: manual Keycloak setup (DEV ONLY)

Do these steps in Keycloak (no automation is performed by this service):

1. Create a realm

- Open Keycloak Admin Console
- Select the realm dropdown (top-left)
- Click **Create realm**
- Name it (example: `creatoros`)
- Click **Create**

2. Create a client (confidential or public)

- Go to **Clients** → **Create client**
- Client type: **OpenID Connect**
- Client ID (example): `auth-service`
- Click **Next**
- Enable **Standard flow** as needed for your testing
- Set **Valid redirect URIs** if you plan to use the Swagger/UI tools (not required for curl testing)
- Click **Save**

3. Create a user

- Go to **Users** → **Create new user**
- Fill username/email
- Click **Create**

4. Assign roles

- Option A (Realm roles):
  - Go to **Realm roles** → **Create role** (e.g. `admin`, `user`)
  - Then open the user → **Role mapping** → assign realm roles
- Option B (Client roles):
  - Open your client (`auth-service`) → **Roles** → create roles (e.g. `svc`)
  - Then open the user → **Role mapping** → assign client roles

5. Get a JWT access token

- Use your preferred method (Keycloak UI, `curl`, Postman) against Keycloak token endpoint.
- Confirm the token contains:
  - `iss` matching `spring.security.oauth2.resourceserver.jwt.issuer-uri`
  - `sub` as the user id
  - `azp` matching your client-id (`auth-service`)
  - `realm_access.roles` and/or `resource_access[azp].roles`

## How to test

Example curl call:

1. Call `/auth/me`:

```bash
curl -i \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8082/auth/me
```

2. Expected `/auth/me` fields:

- `id` (from `sub`)
- `username` / `email` (from token claims when present)
- `roles` (from allowed role sources)

## Consumer contract note (Kafka events)

All events are immutable and versioned via `eventVersion`.

Emitted events:

- `UserCreatedEvent`
- `UserRoleUpdatedEvent`
- `UserDeletedEvent`

Kafka topics used by this service (defaults):

- `auth.user.created`
- `auth.user.role-updated`
- `auth.user.deleted`

Why `kafka-init` exists in docker-compose:

- It pre-creates these topics on startup so local runs are deterministic.
- This avoids relying on broker auto-create behavior and prevents “unknown topic” races when publishing/consuming.

Delivery guarantees:

- Best-effort publish (fire-and-forget).
- Kafka publish failures must not fail HTTP requests.
- No retries/backoff guarantee at this service boundary.

Example: `UserCreatedEvent` (v1)

```json
{
  "eventVersion": 1,
  "eventId": "4b8d3d7b-7f2e-4d44-92b2-b5a3c0a2fd73",
  "occurredAt": "2026-02-06T12:34:56Z",
  "userId": "22be8386-5f63-4d83-9edb-10bfc7d2c2e7",
  "username": "alice",
  "roles": ["user", "creator"]
}
```

Example: `UserRoleUpdatedEvent` (v1)

```json
{
  "eventVersion": 1,
  "eventId": "6c3d7a8a-9e74-4b5b-9d34-6b1e8e6f8b47",
  "occurredAt": "2026-02-06T12:35:10Z",
  "userId": "22be8386-5f63-4d83-9edb-10bfc7d2c2e7",
  "roles": ["user", "creator", "admin"]
}
```

Example: `UserDeletedEvent` (v1)

```json
{
  "eventVersion": 1,
  "eventId": "0d9a6c6d-3b53-44d4-8e7a-0b6a65f7b2a3",
  "occurredAt": "2026-02-06T12:40:00Z",
  "userId": "22be8386-5f63-4d83-9edb-10bfc7d2c2e7"
}
```

Consumer rule: no cross-service DB joins allowed. Persist what you need locally and react to these events.

## Database ownership

Tables owned by auth-service (in `auth_db`):

- `users`
- `roles`
- `user_roles`
- `login_sessions`
- `refresh_tokens`

Tables this service will NEVER manage:

- Keycloak database tables/schemas
- Tables owned by other services (profiles, content, payments, etc.)

## Operational contract

### Startup failure conditions

This service must fail fast on startup when critical configuration is missing:

- `SPRING_PROFILES_ACTIVE` must be set explicitly.
- In `docker`/`prod`: missing `KEYCLOAK_ISSUER_URI`.
- In `docker`/`prod`: missing `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, or `SPRING_DATASOURCE_PASSWORD`.
- In `prod`: missing `KAFKA_BOOTSTRAP_SERVERS`.

### Logs (safe to inspect)

- Logs may contain: `traceId`, `spanId`, `requestId`, `userId` when available (MDC).
- Logs must never contain: raw JWTs, refresh tokens, passwords, or DB credentials.

### Actuator endpoints allowed

- Allowed: `GET /actuator/health`, `GET /actuator/info`
- Health details are hidden by default outside `local`.

### Endpoints that MUST NEVER be exposed publicly

- `GET /auth/users/{userId}/roles` (internal-only)
- Any `/auth/**` endpoint must require `Authorization: Bearer <JWT>` and should only be reachable through the gateway / internal network.

## Changes That Require Platform Approval

- SecurityConfig / Spring Security filter chain behavior
- JWT claim mapping and role extraction rules
- JWT validation contract (issuer handling, optional audience allow-list)
- Database schema and migrations
- Public REST endpoints and response shapes
- Kafka event contract (fields, semantics, `eventVersion`, topic names)

## Final verification checklist

Local run:

- `SPRING_PROFILES_ACTIVE=local`
- Postgres reachable locally
- Keycloak reachable and issuer configured

Docker run:

- `docker compose up --build`
- `SPRING_PROFILES_ACTIVE=docker`
- auth-service reaches Postgres and JWKS

Test run:

- `./gradlew test` (must be green)

Keycloak dependency check:

- Tokens are issued with `iss` matching `KEYCLOAK_ISSUER_URI`
- JWKS is reachable from auth-service runtime (or overridden via `KEYCLOAK_JWK_SET_URI`)

## Auth-Service Status: READY

Handoff complete: contracts are locked, operational boundaries are documented, and deployment can proceed without tribal knowledge.
