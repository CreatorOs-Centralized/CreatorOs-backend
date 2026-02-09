
# CreatorOs Backend

This repository contains multiple Spring Boot services.

## Local development

### Auth + Keycloak (required for frontend login)

Keycloak is defined under the auth-service compose file (not the backend root compose).

- Compose file: `services/auth-service/docker-compose.yml`
- Keycloak UI (dev): `http://localhost:8081`
- auth-service API (dev): `http://localhost:8082`

The auth-service compose starts Keycloak with `--import-realm` and mounts `services/auth-service/keycloak/import/creatoros-realm.json`, so on a fresh machine the realm/client are created automatically.

Run:

```bash
cd services/auth-service
docker compose up --build
```

Note: users are created via self-registration in the Keycloak UI.

