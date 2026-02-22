FROM gradle:8.8.0-jdk17 AS builder

WORKDIR /app

COPY settings.gradle build.gradle ./
COPY gradle gradle/
RUN gradle dependencies --no-daemon || true

COPY src src/
RUN gradle clean bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache curl

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Run as non-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup && \
    chown -R appuser:appgroup /app
USER appuser

# Container-friendly JVM defaults
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=25.0 -XX:+UseContainerSupport -XX:+ExitOnOutOfMemoryError"

EXPOSE 8084

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8084/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
