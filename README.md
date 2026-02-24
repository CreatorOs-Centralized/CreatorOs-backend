# CreatorOS Backend

CreatorOS Backend is a microservices platform for creator-focused products (auth, profiles, content, publishing, scheduling, notifications, analytics, and assets), designed for containerized local development and cloud deployment.

## Why this repo exists

This repository provides:

- A modular backend split by domain service
- Event-driven communication via Kafka
- Isolated Postgres databases per service
- Redis-backed caching/support infrastructure
- A single API Gateway as the public entry point

It is built to let teams develop and deploy independently while keeping a unified backend platform.

## Architecture at a glance

- **Public entry:** `api-gateway` (default `localhost:8080`)
- **Internal services:**
  - `auth-service`
  - `profile-service`
  - `content-service`
  - `asset-service`
  - `publishing-service`
  - `notification-service`
  - `scheduler-service`
  - `analytics-service`
- **Infrastructure:** PostgreSQL, Redis, Kafka, Zookeeper

All services run on a shared Docker network and are orchestrated via `docker-compose.yml`.

## Repository structure

```text
CreatorOs-Backend/
├─ gateway/
│  └─ api-gateway/
├─ services/
│  ├─ auth-service/
│  ├─ profile-service/
│  ├─ content-service/
│  ├─ asset-service/
│  ├─ publishing-service/
│  ├─ notification-service/
│  ├─ scheduler-service/
│  └─ analytics-service/
├─ shared/
│  ├─ api-contracts/
│  ├─ common/
│  └─ events/
├─ infra/
│  ├─ postgres/
│  ├─ kafka/
│  └─ redis/
├─ docs/
├─ docker-compose.yml
├─ docker-compose.dev.yml
└─ .env.example
```

## Quick start (local)

### Prerequisites

- Docker Desktop (or Docker Engine + Compose)
- 4GB+ available RAM

### 1) Configure environment

```bash
cp .env.example .env
```

Set at least these values in `.env`:

- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `REDIS_PASSWORD`
- `JWT_SECRET` (must be at least 32 bytes)

Optional integrations (only if needed):

- OAuth providers: LinkedIn, YouTube, Instagram, Facebook, Google
- GCP storage credentials (`GCP_*`)
- Notification provider credentials (`BREVO_*` / `MAILERSEND_*`)

### 2) Start the stack

```bash
docker compose up --build -d
```

### 3) Verify services

```bash
docker compose ps
curl http://localhost:8080/actuator/health
```

### 4) Follow logs

```bash
docker compose logs -f
```

## Service map

| Service | Default Internal Port | Responsibility |
|---|---:|---|
| api-gateway | 8080 | Public API entry, routing |
| auth-service | 8081 | Authentication, JWT, user auth workflows |
| profile-service | 8082 | Profile and account data |
| content-service | 8083 | Content domain workflows |
| asset-service | 8084 | Asset/file operations and storage integration |
| publishing-service | 8085 | Social platform OAuth + publishing flows |
| scheduler-service | 8086 | Scheduled and delayed jobs |
| analytics-service | 8087 | Analytics/event processing |
| notification-service | 8090 | Notification delivery workflows |

> Only `api-gateway` is exposed publicly by default in `docker-compose.yml`.

## Documentation

- [Architecture notes](docs/architecture.md)
- [API contracts](docs/api-contracts.md)
- [Database design](docs/db-design.md)
- [Event flow](docs/event-flow.md)
- [Deployment notes](docs/deployment.md)
- [Frontend user-scoping migration](docs/frontend-user-scoping-migration.md)

## Common commands

```bash
# Start / stop
docker compose up -d
docker compose down

# Rebuild all
docker compose up --build -d

# Logs (all or one service)
docker compose logs -f
docker compose logs -f auth-service

# Restart one service
docker compose restart publishing-service
```

## Contribution workflow

1. Create a feature branch
2. Make focused changes in the relevant service/module
3. Validate with local Docker stack and service tests
4. Open a pull request with clear scope and testing notes

## License

Add your project license here.
