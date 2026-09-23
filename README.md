# EduFlow — Backend

Spring Boot API for the EduFlow education platform (2ᵉ / 3ᵉ AS informatique).
Development roadmap: [PLAN.md](PLAN.md).

## Requirements

| Tool | Version | Check |
|---|---|---|
| JDK | 21 or newer | `java -version` |
| PostgreSQL | 17 (Homebrew) | `brew services list` |

Databases (created once):

| Database | Used by | User / password |
|---|---|---|
| `eduflow` | the running app (`dev` profile) | `eduflow` / `eduflow` |
| `eduflow_test` | automated tests (`test` profile) | `eduflow` / `eduflow` |

```bash
# Only needed on a new machine:
brew services start postgresql@17
psql -h localhost -d postgres -c "CREATE ROLE eduflow WITH LOGIN PASSWORD 'eduflow';"
createdb -h localhost -O eduflow eduflow
createdb -h localhost -O eduflow eduflow_test
```

Optional: `cp .env.example .env` to override settings locally.

## Run the application

```bash
./gradlew bootRun
```

Starts on http://localhost:8080 with the `dev` profile. Flyway applies database migrations on startup.
Stop it with `Ctrl+C`.

## How to test

### 1. Automated tests — the full build

```bash
./gradlew build
```

This one command runs, in order:

| Check | What it verifies | Report if it fails |
|---|---|---|
| compile | code compiles, MapStruct mappers are complete | console |
| Checkstyle | code style (`config/checkstyle/checkstyle.xml`) | `build/reports/checkstyle/main.html` |
| tests | JUnit tests against `eduflow_test` | `build/reports/tests/test/index.html` |
| JaCoCo | line coverage ≥ 70 % | `build/reports/jacoco/test/html/index.html` |

Useful variations:

```bash
./gradlew test                                   # tests only
./gradlew test --tests SetupSmokeTest            # one test class
./gradlew test --tests '*healthIsUp*'            # one test method
./gradlew checkstyleMain                         # style only
```

### 2. Manual checks — with the app running

| What | How | Expected |
|---|---|---|
| App + database healthy | http://localhost:8080/actuator/health | `"status":"UP"` and `"db":{"status":"UP"}` |
| App info | http://localhost:8080/actuator/info | `{"app":{"name":"EduFlow",…}}` |
| Swagger UI | http://localhost:8080/swagger-ui.html | Swagger page opens |
| API description | http://localhost:8080/v3/api-docs | JSON |
| Security | `curl -i http://localhost:8080/api/v1/courses` | `401` (login required) |

Check the database directly:

```bash
psql -h localhost -U eduflow -d eduflow -c "select version, description, success from flyway_schema_history;"
```

(Homebrew does not put `psql` on the PATH: use `/usr/local/opt/postgresql@17/bin/psql`, or add that folder to your PATH.)

### 3. Code quality — SonarQube

The build is already configured (`sonar { … }` in `build.gradle`). You need a SonarQube server, then:

```bash
export SONAR_HOST_URL=https://sonarcloud.io     # or http://localhost:9000 for a local server
export SONAR_TOKEN=<your token>
./gradlew test sonar
```

Options for the server:
- **SonarCloud** (free for public GitHub repos) — no installation.
- **SonarQube Community** locally — download the zip from sonarsource.com, or run it with Docker.
- Meanwhile, the **SonarQube for IDE** plugin (IntelliJ / VS Code) shows the same issues while you type.

## Profiles

| Profile | When | Database | Swagger |
|---|---|---|---|
| `dev` (default) | local development | `eduflow` | on |
| `test` | `./gradlew test` (set automatically) | `eduflow_test` | on |
| `prod` | production (`SPRING_PROFILES_ACTIVE=prod`) | from `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | off |
