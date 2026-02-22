# Profile Service

The Profile Service is a microservice within the CreatorOS platform that manages creator profiles, biographical information, and social media links.

## Overview

This service provides REST APIs for:
- Creating and managing creator profiles
- Storing profile metadata (display name, bio, images, location, etc.)
- Managing associated social media links
- Retrieving profile information by various identifiers

## Technology Stack

- **Framework**: Spring Boot 3.x
- **Language**: Java 17+
- **Database**: PostgreSQL (with Flyway migrations)
- **ORM**: Spring Data JPA / Hibernate
- **API Documentation**: Swagger/OpenAPI 3
- **Build Tool**: Gradle

## Project Structure

```
profile-service/
├── src/main/java/com/creatoros/profile/
│   ├── ProfileServiceApplication.java       # Main application entry point
│   ├── config/                              # Configuration classes
│   │   ├── DatabaseConfig.java             # JPA & database configuration
│   │   ├── SwaggerConfig.java              # API documentation configuration
│   │   └── SecurityStubConfig.java         # Temporary security configuration
│   ├── controllers/                         # REST API controllers
│   │   ├── ProfileController.java          # Profile management endpoints
│   │   └── SocialLinkController.java       # Social link management endpoints
│   ├── services/                            # Business logic layer
│   │   ├── ProfileService.java             # Profile business logic
│   │   └── SocialLinkService.java          # Social link business logic
│   ├── repositories/                        # Data access layer
│   │   ├── CreatorProfileRepository.java   # Profile repository
│   │   └── SocialLinkRepository.java       # Social link repository
│   ├── entities/                            # JPA entities
│   │   ├── CreatorProfile.java             # Profile entity
│   │   └── SocialLink.java                 # Social link entity
│   ├── dtos/                                # Data Transfer Objects
│   │   ├── request/                         # Request DTOs
│   │   │   ├── CreateProfileRequest.java
│   │   │   └── SocialLinkRequest.java
│   │   └── response/                        # Response DTOs
│   │       ├── ProfileResponse.java
│   │       └── SocialLinkResponse.java
│   ├── mappers/                             # Entity-DTO mappers
│   │   └── ProfileMapper.java
│   ├── utils/                               # Utility classes
│   │   └── UserContextUtil.java            # User context extraction (stub)
│   └── exceptions/                          # Exception handling
│       ├── ResourceNotFoundException.java
│       ├── BadRequestException.java
│       └── GlobalExceptionHandler.java
└── src/main/resources/
    ├── application.yml                      # Application configuration
    └── db/migration/                        # Database migrations
        └── V1__init_profile_tables.sql      # Initial schema
```

## API Endpoints

### Profile Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/profiles/me` | Create or update profile |
| GET | `/profiles/me` | Get authenticated user's profile |
| GET | `/profiles/{username}` | Get public profile by username |
| DELETE | `/profiles/me` | Delete authenticated user's profile |
| GET | `/profiles/me/exists` | Check if profile exists |

### Social Link Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/profiles/me/social-links` | Add a social link |
| GET | `/profiles/me/social-links` | Get all social links |
| PUT | `/profiles/me/social-links/{linkId}` | Update a social link |
| DELETE | `/profiles/me/social-links/{linkId}` | Delete a social link |
| DELETE | `/profiles/me/social-links` | Delete all social links |
| GET | `/profiles/me/social-links/profile/{profileId}` | Get social links by profile ID |

## Data Models

### CreatorProfile

```json
{
  "id": "uuid",
  "userId": "uuid",
  "username": "string",
  "displayName": "string",
  "bio": "string",
  "niche": "string",
  "profilePhotoUrl": "string",
  "coverPhotoUrl": "string",
  "location": "string",
  "language": "string",
  "isPublic": "boolean",
  "isVerified": "boolean",
  "verificationLevel": "string",
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

### SocialLink

```json
{
  "id": "uuid",
  "platform": "INSTAGRAM|YOUTUBE|LINKEDIN|TWITTER|FACEBOOK|WEBSITE",
  "handle": "string",
  "url": "string",
  "isVerified": "boolean",
  "createdAt": "timestamp"
}
```

## Configuration

The service is configured via `application.yml`. Key configuration properties:

```yaml
spring:
  application:
    name: profile-service
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:creatoros_profile}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
    baseline-on-migrate: true

server:
  port: ${SERVER_PORT:8082}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

## Building and Running

### Prerequisites

- Java 17 or higher
- PostgreSQL 14+
- Gradle 8.x (or use included wrapper)
- Docker & Docker Compose (optional)

### Local Development

#### 1. Using Gradle

```bash
# Build the application
./gradlew clean build

# Run the application
./gradlew bootRun
```

#### 2. Using Docker Compose (Recommended)

Build and run the service with PostgreSQL:

```bash
# Build and start all services
docker-compose up --build

# Run in detached mode
docker-compose up -d

# View logs
docker-compose logs -f profile-service

# Stop services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

#### 3. Using Docker Only

```bash
# Build the Docker image
docker build -t creatoros/profile-service:latest .

# Run the container (requires external PostgreSQL)
docker run -d \
  --name profile-service \
  -p 8082:8082 \
  -e DB_HOST=localhost \
  -e DB_PORT=5432 \
  -e DB_NAME=creatoros_profile \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  creatoros/profile-service:latest
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL host | `localhost` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | `creatoros_profile` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `postgres` |
| `SERVER_PORT` | Application port | `8082` |
| `SPRING_PROFILES_ACTIVE` | Active profile | `dev` |

### Accessing the Service

Once running, the service is available at:

- **API Base URL**: `http://localhost:8082/profiles`
- **Swagger UI**: `http://localhost:8082/swagger-ui.html`
- **OpenAPI Docs**: `http://localhost:8082/v3/api-docs`
- **Health Check**: `http://localhost:8082/actuator/health`

## Database Migrations

This service uses Flyway for database schema migrations. Migration scripts are located in `src/main/resources/db/migration/`.

Migrations run automatically on application startup.

## API Documentation

Once the service is running, Swagger UI is available at:

```
http://localhost:8082/swagger-ui.html
```

OpenAPI specification:

```
http://localhost:8082/v3/api-docs
```

## Development Notes

### TODO Items

1. **Authentication**: Replace `SecurityStubConfig` and `UserContextUtil` with proper JWT-based authentication
2. **Database Schema**: Complete the Flyway migration script `V1__init_profile_tables.sql`
3. **Validation**: Add more comprehensive validation rules for URLs and usernames
4. **Caching**: Implement Redis caching for frequently accessed profiles
5. **Events**: Publish events to Kafka when profiles are created/updated
6. **Testing**: Add comprehensive unit and integration tests
7. **Observability**: Add metrics, distributed tracing, and health checks

### Current Limitations

- **Mock Authentication**: The service uses a stub user context that returns hardcoded user IDs
- **No Event Publishing**: Profile changes are not yet published to Kafka
- **Basic Security**: Security configuration is minimal and not production-ready
- **No Rate Limiting**: API endpoints are not rate-limited

## Testing

```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport
```

## Dependencies

Key dependencies include:

- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Security
- Spring Boot Starter Validation
- PostgreSQL Driver
- Flyway Core
- SpringDoc OpenAPI (Swagger)
- SLF4J / Logback

See `build.gradle` for complete dependency list.

## Contributing

1. Follow the existing code structure and conventions
2. Write tests for new features
3. Update this README if adding new endpoints or features
4. Ensure all tests pass before submitting PRs

## License

Proprietary - CreatorOS Platform

## Support

For issues or questions, contact the CreatorOS development team.
