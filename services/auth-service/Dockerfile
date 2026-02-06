FROM gradle:8.8.0-jdk17 AS build

WORKDIR /home/gradle/project

COPY settings.gradle build.gradle ./
COPY src ./src

RUN gradle --no-daemon clean bootJar

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
COPY --from=build /home/gradle/project/build/libs/*.jar /app/app.jar

# Run as non-root.
RUN useradd --system --uid 10001 --create-home appuser \
	&& chown -R appuser:appuser /app
USER 10001

# Container-friendly JVM defaults.
# - MaxRAMPercentage keeps heap aligned to container limits.
# - ExitOnOutOfMemoryError makes OOM visible to orchestrators.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=25 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

