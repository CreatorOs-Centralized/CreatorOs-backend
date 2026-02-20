# CreatorOS Backend

A scalable microservices-based backend system for the CreatorOS platform, featuring modular services, event-driven architecture, and containerized deployment.

## Overview

CreatorOS Backend is a comprehensive backend infrastructure providing:

- **8 Microservices** for different business domains
- **Event-Driven Architecture** using Kafka
- **PostgreSQL Database** with multi-database setup
- **Redis Caching** for performance
- **Complete Docker Setup** for local development and production
- **API Gateway** for unified access

## Quick Start

### Prerequisites

- Docker 20.10+
- Docker Compose 2.0+
- 4GB+ RAM
- GCP credentials (for Asset Service - optional)
- LinkedIn OAuth credentials (for Publishing Service - optional)

### 1. Clone and Setup

```bash
# Navigate to project
cd CreatorOs-Backend

# Copy environment template
cp .env.example .env
```

### 2. Configure

Edit `.env` file with your values:

```env
# REQUIRED
POSTGRES_PASSWORD=your_secure_password

# OPTIONAL (if using Asset Service)
GCP_PROJECT_ID=your-project-id
GCP_BUCKET_NAME=your-bucket-name
GCP_CREDENTIALS_PATH=./path/to/credentials.json

# OPTIONAL (if using Publishing Service)
LINKEDIN_CLIENT_ID=your_client_id
LINKEDIN_CLIENT_SECRET=your_client_secret
```

### 3. Start

```bash
# Start all services
docker-compose up --build -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f
```

### 4. Verify

```bash
# Test API Gateway
curl http://localhost:8080/actuator/health

# List all services
docker-compose ps
```

**Done!** Your entire backend stack is running. 🎉

## Architecture

```
┌─────────────────────────────────────┐
│       API Gateway (Port 8080)        │
├─────────────────────────────────────┤
│  Routes to microservices            │
└────────┬────────────────────────────┘
         │
    ┌────┼────┐
    │    │    │
    ▼    ▼    ▼
  Auth Profile Content
  Service Service Service
    │       │       │
    └───────┼───────┴─────────┐
            │                 │
            ▼                 ▼
        PostgreSQL        Asset/Publishing/
        (Multi-DB)        Scheduler Services
         │   ▲                 │
         │   │            ┌────┴───┐
         │   │            │        │
         ▼   │            ▼        ▼
       Redis │          Kafka    GCP
         │   │            │
         └───┴────────────┴────────┘
         Data Layer & Message Bus
```

## Services

| Service                | Port | Purpose                            | Database      |
| ---------------------- | ---- | ---------------------------------- | ------------- |
| **API Gateway**        | 8080 | Request routing and load balancing | -             |
| **Auth Service**       | 8081 | Authentication & authorization     | auth_db       |
| **Profile Service**    | 8082 | User profile management            | profile_db    |
| **Content Service**    | 8083 | Content management                 | content_db    |
| **Asset Service**      | 8084 | File uploads & GCS integration     | asset_db      |
| **Publishing Service** | 8085 | Social media publishing            | publishing_db |
| **Scheduler Service**  | 8086 | Task scheduling                    | scheduler_db  |
| **Analytics Service**  | 8087 | Platform analytics & metrics       | analytics_db  |

### Infrastructure Services

- **PostgreSQL** (Port 5432) - Primary database for all services
- **Redis** (Port 6379) - Caching layer
- **Kafka** (Port 9092) - Event streaming
- **Zookeeper** (Port 2181) - Kafka coordination

## Getting Started

### For Developers

1. **Initial Setup**

   ```bash
   make setup
   # or: .\docker.ps1 setup (Windows)
   ```

2. **Edit Configuration**

   ```bash
   # Update .env with your values
   nano .env
   ```

3. **Start Development**

   ```bash
   make up
   # or: .\docker.ps1 up (Windows)
   ```

4. **View Logs**

   ```bash
   make logs-auth
   # or: .\docker.ps1 logs-auth (Windows)
   ```

5. **Rebuild Service**
   ```bash
   make dev-profile
   # or: .\docker.ps1 dev-profile (Windows)
   ```

### Validation

Before starting, validate your setup:

```bash
# Unix/Linux/Mac
./validate-setup.sh

# Windows
.\validate-setup.ps1
```

## Available Commands

### Unix/Linux/Mac (Makefile)

```bash
make setup          # Initial setup
make build          # Build all services
make up            # Start all services
make down          # Stop all services
make restart       # Restart all services
make rebuild       # Rebuild and restart
make logs          # Show all logs
make logs-auth     # Show auth service logs
make clean         # Stop containers
make clean-all     # Remove everything including data
make health        # Check service health
make dev-<service> # Rebuild specific service
```

### Windows (PowerShell)

```powershell
.\docker.ps1 setup          # Initial setup
.\docker.ps1 build          # Build all services
.\docker.ps1 up            # Start all services
.\docker.ps1 down          # Stop all services
.\docker.ps1 restart       # Restart all services
.\docker.ps1 rebuild       # Rebuild and restart
.\docker.ps1 logs          # Show all logs
.\docker.ps1 logs-auth     # Show auth service logs
.\docker.ps1 clean         # Stop containers
.\docker.ps1 clean-all     # Remove everything including data
.\docker.ps1 health        # Check service health
.\docker.ps1 dev-<service> # Rebuild specific service
```

## Configuration

### Environment Variables

All configuration is driven by `.env` file:

**Database:**

- `POSTGRES_USER` - PostgreSQL username
- `POSTGRES_PASSWORD` - PostgreSQL password (required)
- `POSTGRES_PORT` - PostgreSQL port (default: 5432)

**Service Ports:**

- `API_GATEWAY_PORT` - API Gateway port (default: 8080)
- `AUTH_SERVICE_PORT` - Auth Service port (default: 8081)
- `PROFILE_SERVICE_PORT` - Profile Service port (default: 8082)
- `CONTENT_SERVICE_PORT` - Content Service port (default: 8083)
- `ASSET_SERVICE_PORT` - Asset Service port (default: 8084)
- `PUBLISHING_SERVICE_PORT` - Publishing Service port (default: 8085)
- `SCHEDULER_SERVICE_PORT` - Scheduler Service port (default: 8086)
- `ANALYTICS_SERVICE_PORT` - Analytics Service port (default: 8087)

**External Services:**

- `GCP_PROJECT_ID` - Google Cloud Project ID
- `GCP_BUCKET_NAME` - GCS bucket name
- `GCP_CREDENTIALS_PATH` - Path to GCP credentials JSON
- `LINKEDIN_CLIENT_ID` - LinkedIn OAuth client ID
- `LINKEDIN_CLIENT_SECRET` - LinkedIn OAuth client secret

See [.env.example](.env.example) for complete list.

### Docker Compose Overrides

For local development customizations, create `docker-compose.override.yml`:

```bash
cp docker-compose.override.yml.example docker-compose.override.yml
```

This allows:

- Adding debug ports
- Mounting source code for hot reload
- Adding monitoring tools (Kafka UI, Redis Commander, Adminer)

## Documentation

- **[QUICKSTART.md](QUICKSTART.md)** - 5-minute quick start guide
- **[DOCKER.md](DOCKER.md)** - Complete Docker documentation
- **[DOCKER_SETUP.md](DOCKER_SETUP.md)** - Setup summary and architecture
- **[docs/architecture.md](docs/architecture.md)** - System architecture
- **[docs/api-contracts.md](docs/api-contracts.md)** - API contracts
- **[docs/db-design.md](docs/db-design.md)** - Database design
- **[docs/event-flow.md](docs/event-flow.md)** - Event-driven flows
- **[docs/deployment.md](docs/deployment.md)** - Production deployment

## Database Management

### Connect to PostgreSQL

```bash
docker-compose exec postgres psql -U postgres

# Connect to specific database
\c auth_db

# List tables
\dt

# Exit
\q
```

### Databases

All services have separate databases:

- `auth_db` - Auth Service
- `profile_db` - Profile Service
- `content_db` - Content Service
- `asset_db` - Asset Service
- `publishing_db` - Publishing Service
- `scheduler_db` - Scheduler Service
- `analytics_db` - Analytics Service

Databases are automatically created and migrated on first startup.

## Kafka Topics

```bash
# List topics
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Create topic
docker-compose exec kafka kafka-topics --create \
  --topic my-topic \
  --bootstrap-server localhost:9092

# Watch messages
docker-compose exec kafka kafka-console-consumer \
  --topic my-topic \
  --from-beginning \
  --bootstrap-server localhost:9092
```

## Troubleshooting

### Port Already in Use

```bash
# Update .env with different port
AUTH_SERVICE_PORT=8181  # Changed from 8081
```

### Service Won't Start

```bash
# Check logs
docker-compose logs -f <service-name>

# Verify .env configuration
cat .env

# Check Docker has enough memory
docker stats
```

### Database Connection Failed

```bash
# Wait 30 seconds or check:
docker-compose ps postgres

# Check PostgreSQL logs
docker-compose logs postgres
```

### GCP Credentials Error

```bash
# Verify path exists
ls -la path/to/credentials.json

# Update .env with correct path
GCP_CREDENTIALS_PATH=./credentials/gcp.json
```

### Memory Issues

```bash
# Check available memory
docker stats

# Stop services and free memory
docker-compose down
```

## Development Features

### Hot Reload

To enable hot reload for development, create `docker-compose.override.yml`:

```yaml
services:
  auth-service:
    volumes:
      - ./services/auth-service/src:/app/src:ro
    environment:
      - SPRING_DEVTOOLS_RESTART_ENABLED=true
```

### Debug Mode

Add debug ports to `docker-compose.override.yml`:

```yaml
services:
  auth-service:
    ports:
      - "5005:5005"
    command: >
      sh -c "java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
      $JAVA_OPTS -jar /app/app.jar"
```

Then connect your IDE debugger to `localhost:5005`.

### Monitoring Tools

Add to `docker-compose.override.yml`:

```yaml
# Adminer - Database Management
adminer:
  image: adminer:latest
  ports:
    - "8090:8080"
  depends_on:
    - postgres

# Kafka UI
kafka-ui:
  image: provectuslabs/kafka-ui:latest
  ports:
    - "8091:8080"
  environment:
    - KAFKA_CLUSTERS_0_NAME=local
    - KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS=kafka:29092
    - KAFKA_CLUSTERS_0_ZOOKEEPER=zookeeper:2181

# Redis Commander
redis-commander:
  image: rediscommander/redis-commander
  ports:
    - "8092:8081"
  environment:
    - REDIS_HOSTS=local:redis:6379
```

## Production Deployment

For production deployment, see [docs/deployment.md](docs/deployment.md).

### Security Considerations

- ✅ Use strong, unique passwords
- ✅ Enable SSL/TLS for all services
- ✅ Use secrets management (AWS Secrets Manager, Azure Key Vault)
- ✅ Don't expose database ports externally
- ✅ Enable authentication for all services
- ✅ Use managed services (Cloud SQL, ElastiCache, Managed Kafka)
- ✅ Implement rate limiting and DDoS protection
- ✅ Set up monitoring and alerting
- ✅ Regular security audits and updates

## Contributing

1. Create a feature branch
2. Make your changes
3. Ensure tests pass
4. Submit a pull request

## Testing

Services are tested automatically on startup. To run tests manually:

```bash
# Build with tests
docker-compose build --no-cache

# Or run tests directly
docker-compose exec auth-service gradle test
```

## Performance Optimization

### Current Setup

- Multi-stage Docker builds for smaller images
- Alpine Linux for minimal size
- JVM optimized for containers
- Connection pooling (HikariCP)
- Redis caching layer

### Scaling

For production scaling:

```bash
# Scale a service
docker-compose up -d --scale publishing-service=3

# Use load balancer (NGINX, HAProxy)
# Use managed container orchestration (Kubernetes)
```

## Monitoring & Logging

### Check Health

```bash
# All services
docker-compose ps

# Specific service
docker-compose exec auth-service curl http://localhost:8080/actuator/health
```

### View Logs

```bash
# All services (follow updates)
docker-compose logs -f

# Specific service
docker-compose logs -f auth-service

# Last 100 lines
docker-compose logs --tail=100 auth-service

# Specific time
docker-compose logs --since 5m auth-service
```

### Metrics

Services expose metrics via Spring Boot Actuator:

```
http://localhost:8081/actuator/prometheus  # Auth Service
http://localhost:8082/actuator/prometheus  # Profile Service
```

## Support

For issues or questions:

1. Check [DOCKER.md](DOCKER.md) troubleshooting section
2. Check service logs: `docker-compose logs -f <service>`
3. Review [docs/](docs/) for detailed documentation
4. Open an issue on GitHub

## License

[Your License Here]

## Project Structure

```
CreatorOS-Backend/
├── gateway/                    # API Gateway
│   └── api-gateway/
├── services/                   # Microservices
│   ├── auth-service/
│   ├── profile-service/
│   ├── content-service/
│   ├── asset-service/
│   ├── publishing-service/
│   ├── scheduler-service/
│   └── analytics-service/
├── shared/                     # Shared libraries
│   ├── api-contracts/
│   ├── common/
│   └── events/
├── infra/                      # Infrastructure configs
│   ├── postgres/
│   ├── kafka/
│   └── redis/
├── docs/                       # Documentation
├── docker-compose.yml          # Docker Compose configuration
├── .env.example                # Environment template
├── Makefile                    # Unix/Linux/Mac commands
├── docker.ps1                  # Windows PowerShell commands
├── DOCKER.md                   # Docker documentation
├── QUICKSTART.md               # Quick start guide
└── README.md                   # This file
```

## Getting Help

Start here:

1. **[QUICKSTART.md](QUICKSTART.md)** - Get running in 5 minutes
2. **[DOCKER.md](DOCKER.md)** - Complete Docker documentation
3. **[docs/](docs/)** - Architecture and API documentation

## What's Next?

- ☑ Docker setup completed
- ◻ Add authentication with auth-service (JWT)
- ◻ Configure CI/CD pipeline
- ◻ Set up monitoring (Prometheus/Grafana)
- ◻ Deploy to cloud (AWS/GCP/Azure)

---

**Ready to get started?** Follow [QUICKSTART.md](QUICKSTART.md) for a 5-minute setup!
