# EduFlow Backend — Development Plan

Spring Boot backend for the EduFlow education platform (design: `EduFlow Platform.dc.html`).
We build **one step at a time**: each step is reviewed and tested in Swagger before the next starts.

---

## 1. Tech stack

| Area | Choice |
|---|---|
| Language / framework | Java 21, Spring Boot 4.1, Gradle |
| Database | PostgreSQL everywhere: `eduflow` (dev), `eduflow_test` (tests), env-configured (prod) |
| Migrations | Flyway |
| Persistence | Spring Data JPA (Hibernate) |
| Mapping | MapStruct (+ Lombok binding) |
| Security | Spring Security, JWT (access + refresh tokens), BCrypt |
| API docs | springdoc-openapi (Swagger UI) |
| Validation | Jakarta Validation |
| Tests | JUnit 5, Mockito, Spring Boot Test (MockMvc) |
| Quality | SonarQube, JaCoCo (coverage), Checkstyle |
| Ops | Spring Boot Actuator, structured logging |

---

## 2. Conventions

### Package structure (package by feature)

```
com.example.education_platform
├── common/                 shared code: BaseEntity, exceptions, PageResponse, config
├── security/               JWT, SecurityConfig, CurrentUser
└── <feature>/              e.g. course, quiz, assignment…
    ├── controller/         REST endpoints only — no business logic
    ├── dto/request/        input payloads + validation annotations
    ├── dto/response/       output payloads
    ├── entity/             JPA entities + enums
    ├── mapper/             MapStruct mappers (entity ⇄ DTO)
    ├── repository/         Spring Data repositories
    └── service/            interface + impl/…ServiceImpl (business logic, @Transactional)
```

### Rules
- Entities never leave the service layer — controllers only see DTOs.
- Lombok on entities: `@Getter @Setter @NoArgsConstructor` only — **never `@Data`**.
- `spring.jpa.open-in-view=false`; use `@EntityGraph` / fetch joins to avoid N+1.
- All list endpoints are paginated (`Pageable` → `PageResponse<T>`).
- All endpoints under `/api/v1/...`.
- Errors use RFC 7807 `ProblemDetail`, one format everywhere.
- No secrets in the repo — passwords and JWT key come from environment variables.
- Status values are enums (`QuizStatus.IN_PROGRESS`), never magic strings.
- Code in English; user-facing messages in French via `messages_fr.properties`.
- One git commit per finished task; one branch per step.

---

## 3. Features (from the design)

| # | Feature | Step |
|---|---|---|
| 1 | Authentication (login, refresh, forgot password, access request) | 2 |
| 2 | Users / Élèves (invite, list, filter, status, level) | 2 |
| 3 | Profile & settings (name, email, photo, preferences) | 2 |
| 4 | Modules (CRUD, by level) | 3 |
| 5 | Chapters & lessons (video, PDF, quiz, TP) + progress | 3 |
| 6 | Quiz (questions, duration, deadline, statuses, attempts, scores) | 4 |
| 7 | TP & devoirs (TP in pairs / individual homework) | 5 |
| 8 | Submissions (file + comment, statuses) | 5 |
| 9 | Corrections (grading) | 5 |
| 10 | File storage (uploads up to 500 MB) | 5 |
| 11 | Notes (gradebook, averages) | 6 |
| 12 | Ressources (library, upload/download) | 7 |
| 13 | Annonces (to section or module) | 8 |
| 14 | Blog (articles, categories, drafts, views) | 8 |
| 15 | Dashboard (stats, charts, deadlines, activity) | 9 |
| 16 | Notifications | 9 |

---

## 4. Roadmap

Legend: `[ ]` todo · `[x]` done

### Step 0 — Project setup
- [x] Initialise git, update `.gitignore`, first commit
- [x] Add dependencies: springdoc-openapi, MapStruct, Flyway, Actuator, JaCoCo, Checkstyle, Sonar plugin
- [x] Profiles: `dev`, `test`, `prod` (`application-*.yml`), secrets via env variables (`.env.example`)
- [x] PostgreSQL running locally, `eduflow` database created
- [x] SonarQube config in `build.gradle` (ready to connect)
- [x] README with run & test instructions
- **Done when:** `./gradlew build` passes and the app starts on PostgreSQL. ✅

### Step 1 — Core (common)
- [x] `BaseEntity` (id, createdAt, updatedAt, createdBy, updatedBy) + JPA auditing
- [x] Soft delete support
- [x] Exceptions (`ResourceNotFoundException`, `BusinessException`, …) + `GlobalExceptionHandler` (ProblemDetail)
- [x] `PageResponse<T>`
- [x] i18n messages (`messages_fr.properties`)
- [x] Swagger config (title, version, JWT "Authorize" button)
- **Done when:** Swagger UI opens at `/swagger-ui.html` and errors return the standard format. ✅

### Step 2 — Auth, users & profile
- [x] `User` entity (role ADMIN/STUDENT, level 2AS/3AS, status) + Flyway migration
- [x] Login → access + refresh token; refresh; logout
- [x] Rate limiting on login
- [x] Admin: invite student, list/filter students (paginated), change status
- [x] Me: get/update profile, change password, preferences
- [x] Ownership checks helper (student sees only own data)
- [x] Unit + integration tests
- **Done when:** admin and student can log in from Swagger and see only what they are allowed to. ✅

Carried into later steps: the invitation returns a one-time temporary password instead of
sending mail (Step 9 adds the email service), and login rate limiting is in-memory, so it
resets on restart and is not shared between instances.

### Step 3 — Modules, chapters, lessons & progress
- [ ] Entities `Course`, `Chapter`, `Lesson`, `LessonCompletion` + migration
- [ ] CRUD modules / chapters / lessons (admin), ordering by position
- [ ] Students see only their level; mark lesson done / undone
- [ ] Progress % per student and class average per module
- [ ] Demo seed data (dev profile) from the design
- [ ] Tests
- **Done when:** the Modules and module-detail screens can be fully fed by the API.

### Step 4 — Quiz
- [ ] Entities `Quiz`, `Question`, `Choice`, `QuizAttempt`, `Answer` + migration
- [ ] Admin: create/edit quiz, statuses (DRAFT / IN_PROGRESS / CLOSED), deadline, duration
- [ ] Student: start attempt, submit answers, automatic scoring
- [ ] Stats: submissions, average score
- [ ] Tests

### Step 5 — TP & devoirs, submissions, corrections, file storage
- [ ] `StorageService` interface + local-disk implementation, file validation (type, size, name)
- [ ] Entities `Assignment` (TP / DEVOIR, pair / individual), `Submission` + migration
- [ ] Admin: create assignment, list submissions to grade, grade + feedback
- [ ] Student: upload submission (draft / submitted), see grade
- [ ] Ownership checks (student sees only own submissions)
- [ ] Tests

### Step 6 — Notes (gradebook)
- [ ] Grade aggregation from quizzes + assignments (+ manual grades, e.g. partiel)
- [ ] Admin: gradebook per module; student: averages per module
- [ ] Tests

### Step 7 — Ressources
- [ ] Entity `Resource` (PDF / VIDEO / ZIP, module or all modules) + migration
- [ ] Upload, list/filter (paginated), download
- [ ] Tests

### Step 8 — Annonces & Blog
- [ ] `Announcement` (section-wide or per module, recipients count)
- [ ] `BlogPost` (category, draft/published, excerpt, read time, views)
- [ ] Tests

### Step 9 — Dashboard, notifications & background jobs
- [ ] Domain events (submission created, quiz finished, …) → notifications + recent activity
- [ ] `Notification` entity, list / mark as read
- [ ] Dashboard endpoints (admin + student stats, weekly chart, deadlines, activity) with caching
- [ ] `@Scheduled` jobs: auto-close quizzes/TPs after deadline, deadline reminders
- [ ] Email service (invitations, password reset)
- [ ] Tests

### Step 10 — Production readiness
- [ ] Dockerfile
- [ ] GitHub Actions CI: build, tests, Sonar analysis
- [ ] Structured logging with request id
- [ ] Final Sonar pass: no blocker/critical issues, coverage ≥ 70 %

---

## 5. Open decisions

| Question | Default if not answered |
|---|---|
| Database locally | ✅ Homebrew PostgreSQL 17 — db `eduflow`, user `eduflow` (localhost:5432) |
| SonarQube | Config ready; connect to SonarCloud / local server later |
| Language | English code, French messages |

## 6. Note on existing code

Some code was drafted before this plan (JWT security, users, modules/chapters/lessons in a flat
structure, not yet compiled). It will be moved into the structure above during Steps 1–3 rather
than kept as is.
