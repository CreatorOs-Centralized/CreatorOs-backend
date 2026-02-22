# auth-service (Custom JWT Auth)

auth-service is CreatorOS' authentication and authorization service.

## What it does

- Registers users (`POST /auth/register`)
- Authenticates users (`POST /auth/login`) and issues access JWTs (HS256)
- Issues and rotates refresh tokens (`POST /auth/refresh`)
- Logs out by revoking refresh tokens (`POST /auth/logout`)
- Email verification (`POST /auth/verify-email`)
- Password reset request/confirm (`POST /auth/password-reset/request`, `POST /auth/password-reset/confirm`)
- RBAC via roles stored in `auth_db` (embedded into JWT `roles` claim)
- Best-effort Kafka publishing for user lifecycle events (must never block HTTP)

## Security model

- Stateless auth (`SessionCreationPolicy.STATELESS`)
- Access tokens: short-lived JWTs
- Refresh tokens: random secrets stored **hashed** in Postgres; rotation enabled; reuse triggers user-wide revocation
- No server-side HTTP sessions

## Environment contract

Required:

- `SPRING_PROFILES_ACTIVE` (`local` | `docker` | `prod`)
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET` (must be **at least 32 bytes**)

Optional:

- `ACCESS_TOKEN_EXPIRATION` (seconds; default `900`)
- `REFRESH_TOKEN_EXPIRATION` (seconds; default `604800`)
- `KAFKA_BOOTSTRAP_SERVERS`
- `SERVER_PORT`
- `APP_VERSION`
- `AUTH_DEBUG_TOKEN_RESPONSE` (default `false`; when `true`, register/password-reset return raw tokens for local/dev testing only)

## Local docker-compose

From the repo root, run the platform compose and provide `JWT_SECRET` in your `.env`.
