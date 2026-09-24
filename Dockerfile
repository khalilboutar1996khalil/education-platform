# Build and run EduFlow. Two stages, so the JDK and the Gradle cache never reach the final image.
#
#   docker build -t eduflow-backend .
#   docker run --rm -p 8080:8080 --env-file .env eduflow-backend
#
# Java 21 to match sourceCompatibility in build.gradle, not whatever the host happens to run.

# ---------- build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Wrapper and build files first: these change rarely, so the dependency download below
# stays cached while only source changes.
COPY gradlew ./
COPY gradle gradle
COPY settings.gradle build.gradle ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies --quiet || true

# checkstyle.xml is referenced while the build script is configured, so it has to be present
COPY config config
COPY src src

# Tests need a database, which a build container has no business starting; CI runs them instead.
RUN ./gradlew --no-daemon bootJar -x test \
    && cp build/libs/*-SNAPSHOT.jar /workspace/app.jar

# ---------- run ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Never root: a container that is compromised should not own the filesystem it runs on.
RUN groupadd --system eduflow && useradd --system --gid eduflow --home /app eduflow

# Uploads live here. Mount a volume over it, or they vanish with the container.
RUN mkdir -p /app/var/uploads && chown -R eduflow:eduflow /app

COPY --from=build --chown=eduflow:eduflow /workspace/app.jar app.jar

USER eduflow
EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod \
    STORAGE_LOCATION=/app/var/uploads \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"

# Actuator already reports the database, so this checks the app end to end rather than just the port
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD ["sh", "-c", "wget -q -O- http://localhost:8080/actuator/health | grep -q '\"status\":\"UP\"'"]

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
