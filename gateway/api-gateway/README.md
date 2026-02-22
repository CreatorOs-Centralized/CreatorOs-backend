# API Gateway - Spring Cloud Gateway

## Overview

Production-grade API Gateway providing:

- **Centralized Routing** - Single entry point for all services
- **JWT Authentication** - Validates tokens and injects user context
- **Request Tracing** - Correlation IDs for observability
- **CORS Handling** - Cross-origin resource sharing
- **Request/Response Logging** - Performance monitoring

## Architecture

```
Frontend → API Gateway (port 8080)
              ↓
    ┌─────────┴─────────┐
    ↓         ↓         ↓
  Auth    Content    Publishing
           (etc.)
```

## Routes Configuration

| Path Pattern        | Service              | Port | Auth Required |
| ------------------- | -------------------- | ---- | ------------- |
| `/auth/**`          | Auth Service         | 8081 | ❌ Public     |
| `/oauth/**`         | Publishing Service   | 8085 | ❌ Public     |
| `/profiles/**`      | Profile Service      | 8082 | ✅ JWT        |
| `/contents/**`      | Content Service      | 8083 | ✅ JWT        |
| `/assets/**`        | Asset Service        | 8084 | ✅ JWT        |
| `/publishing/**`    | Publishing Service   | 8085 | ✅ JWT        |
| `/scheduler/**`     | Scheduler Service    | 8086 | ✅ JWT        |
| `/analytics/**`     | Analytics Service    | 8087 | ✅ JWT        |
| `/notifications/**` | Notification Service | 8088 | ✅ JWT        |

## Global Filters

### 1. Correlation ID Filter (Priority: -200)

- Generates unique request ID
- Adds `X-Correlation-Id` header
- Enables distributed tracing

### 2. JWT Authentication Filter (Priority: -100)

- Validates JWT tokens
- Extracts user ID
- Injects `X-User-Id` header for downstream services
- Skips public routes

### 3. Logging Filter (Priority: Lowest)

- Logs request/response
- Tracks duration
- Records status codes

## Local Development

### Prerequisites

- Java 21
- Gradle 8.x

### Run Locally

```bash
# From gateway/api-gateway directory
./gradlew bootRun
```

Gateway will run on `http://localhost:8080`

### Environment Variables

```env
JWT_SECRET=your-secret-key-min-32-chars
```

## Docker Deployment

### Build Image

```bash
docker build -t api-gateway .
```

### Run with Docker Compose

```bash
docker-compose up api-gateway
```

### Docker Environment

The gateway automatically uses service names in Docker:

- `http://auth-service:8081`
- `http://content-service:8083`
- etc.

## Testing

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

### Test Routing (Public)

```bash
# Should route to auth service
curl http://localhost:8080/auth/ping
```

### Test with JWT (Protected)

```bash
# Get token from auth service first
TOKEN="your-jwt-token"

# Access protected endpoint
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/profiles/me
```

### Verify Correlation ID

```bash
curl -v http://localhost:8080/auth/ping
# Check response headers for X-Correlation-Id
```

## Security Features

### JWT Validation

- Uses HMAC-SHA256 signing
- Extracts user ID from `subject` claim
- Validates token signature and expiration
- Injects `X-User-Id` header for services

### Public Routes

Routes that bypass JWT:

- `/auth/**` - Authentication endpoints
- `/oauth/**` - OAuth callbacks
- `/actuator/**` - Health checks

### CORS Configuration

- Allows `localhost:3000` and `localhost:5173`
- Credentials enabled
- All headers allowed
- Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH

## Production Enhancements

### 🔒 Security

- [ ] Use RSA asymmetric keys
- [ ] Store JWT secret in Secret Manager
- [ ] Implement rate limiting (Redis)
- [ ] Add request size limits
- [ ] Enable HTTPS/TLS

### 📊 Observability

- [ ] OpenTelemetry tracing
- [ ] Prometheus metrics
- [ ] Distributed logging (ELK)
- [ ] APM integration

### 🔄 Resilience

- [ ] Circuit breaker (Resilience4j)
- [ ] Retry logic
- [ ] Timeout configuration
- [ ] Load balancing

### ⚡ Performance

- [ ] Response caching
- [ ] Request deduplication
- [ ] Connection pooling
- [ ] WebSocket support

## Troubleshooting

### Gateway returns 401 Unauthorized

- Check JWT secret matches auth service
- Verify token hasn't expired
- Ensure `Bearer ` prefix in Authorization header

### Route not found (404)

- Check route patterns in `RouteConfig.java`
- Verify service URLs and ports
- Check service is running

### Connection refused to downstream service

- Verify service is running
- Check service URL configuration
- In Docker: ensure services are on same network

## Configuration Reference

### application.yml

```yaml
spring:
  cloud:
    gateway:
      routes: # Auto-configured via RouteConfig.java

server:
  port: 8080

security:
  jwt:
    secret: ${JWT_SECRET}
```

### application-docker.yml

Uses service names instead of localhost for Docker networking.

## Support

For issues or questions, check:

- Gateway logs: `docker logs api-gateway`
- Service health: `http://localhost:8080/actuator/health`
- Spring Cloud Gateway docs
