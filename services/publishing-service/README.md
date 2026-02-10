# Publishing Service

Publishing Service is responsible for managing the publication of content across multiple social media platforms.

## Features

- Multi-platform content publishing (Twitter, Facebook, Instagram, LinkedIn, YouTube, etc.)
- Publish job scheduling and execution
- Retry mechanism for failed publishes
- Connected account management
- Event-driven architecture using Kafka
- Comprehensive logging and error tracking

## Technology Stack

- Java 17+
- Spring Boot 3.x
- Spring Data JPA
- PostgreSQL
- Kafka
- Docker

## Getting Started

### Prerequisites

- Java 17 or higher
- PostgreSQL
- Kafka
- Docker (optional)

### Running Locally

1. Clone the repository
2. Configure the database connection in `application-local.yml`
3. Build the application:
   ```bash
   ./gradlew build
   ```
4. Run the application:
   ```bash
   ./gradlew bootRun
   ```

### Running with Docker

```bash
docker build -t publishing-service .
docker run -p 8080:8080 publishing-service
```

## API Endpoints

- `GET /api/health` - Health check endpoint

## Database Migrations

Database migrations are managed using Flyway. Migration files are located in `src/main/resources/db/migration/`.

## Project Structure

```
publishing-service/
├── src/main/java/com/creatoros/publishing/
│   ├── PublishingServiceApplication.java
│   ├── config/                    # Configuration classes
│   ├── controllers/               # REST endpoints
│   ├── entities/                  # JPA entities
│   ├── repositories/              # Data access layer
│   ├── kafka/                     # Kafka consumers and producers
│   ├── services/                  # Business logic
│   ├── strategy/                  # Strategy pattern implementations
│   ├── models/                    # DTOs and models
│   └── exceptions/                # Custom exceptions
└── src/main/resources/            # Configuration and migrations
```

## Configuration

### Environment Variables

- `SPRING_DATASOURCE_URL` - Database URL
- `SPRING_DATASOURCE_USERNAME` - Database username
- `SPRING_DATASOURCE_PASSWORD` - Database password
- `SPRING_KAFKA_BOOTSTRAP_SERVERS` - Kafka bootstrap servers

## Contributing

Please follow the project's code style and conventions.

## License

This project is part of the CreatorOs backend ecosystem.
