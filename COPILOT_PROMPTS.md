# GitHub Copilot prompts — FlightLog (Kotlin / Spring Boot)

A sequenced list of prompts to feed to GitHub Copilot Chat (in agent mode, this
repo open as the workspace) to implement the FlightLog system described in
`docs.txt`. Each prompt is self-contained; run them **in order** and verify with
`./gradlew build` between major sections.

Conventions used in every prompt:

- Stack: Kotlin 2.1, Spring Boot 3.4, Spring Data JPA, PostgreSQL (H2 in tests).
- Package root: `eu.profinit.flightlog`.
- Date format `DD-MM-YYYY`, time `HH:mm` (24h), CSV separator `;`, encoding UTF-8.
- All API responses in JSON; UI labels in Czech.
- All authorization checks live on the backend; never trust the client.
- Add unit tests under `src/test/kotlin/...` mirroring the production package.

---

## 0. Bootstrap

> Read `docs.txt` (in the parent folder) and `flight-log-kotlin/README.md`.
> Summarize, in ≤15 bullets, the modules you will need to add or modify
> (controller, facade, operation, repository, entity, model, integration,
> security, frontend) to satisfy FR-01 through FR-13 and NFR-01 through NFR-15.
> Do not write code yet — produce a plan and an updated checklist saved as
> `IMPLEMENTATION_PLAN.md`.

---

## 1. Domain model & persistence (foundation for FR-03..FR-10, NFR-08)

### 1.1 Extend entities

> In `repository/entity/`, extend the JPA model so it supports the spec:
> - `Flight` gains `task: String`, `landingTime: OffsetDateTime?`,
>   `flightType: FlightType` (GLIDER / TOWPLANE), nullable `linkedFlight: Flight?`
>   (self-FK for tow ↔ glider pairing per FR-10), `createdBy`, `updatedBy`,
>   `createdAt`, `updatedAt` audit columns.
> - `Airplane` gains `category: AirplaneCategory` (GLIDER / POWERED) and
>   `type: String` (already partially present — make it required).
> - Add `User` entity with `id`, `email` (unique), `passwordHash`, `role`
>   (`WRITER` / `ADMIN`), `failedLoginAttempts`, `lockedUntil`.
> - Add `AuditLog` entity (`id`, `userId`, `timestamp`, `action`, `entityType`,
>   `entityId`, `level` ENUM INFO/WARN/ERROR, `details` text) — NFR-09.
> Use `@Column(nullable = ...)` faithfully. Add Flyway migrations under
> `src/main/resources/db/migration/V2__flightlog_core.sql` (V1 is whatever
> already exists; create one if missing).

### 1.2 Repositories

> For every new entity create a Spring Data JPA interface in
> `repository/jpa/` and a domain-style wrapper in `repository/impl/` matching
> the existing pattern (`FlightRepositoryImpl`, etc.). Include query methods:
> - `FlightRepository.search(filter: FlightFilter, pageable): Page<Flight>`
>   filtered by date range, airplane category, immatriculation, takeoff/landing
>   range, duration range, and a boolean `inAirOnly` (landingTime IS NULL).
> - `UserRepository.findByEmailIgnoreCase`.
> - `AuditLogRepository` (append-only).

---

## 2. Authentication & authorization (FR-01, NFR-05/06/07)

### 2.1 Security config

> Add Spring Security (`spring-boot-starter-security`) to
> `build.gradle.kts`. Configure:
> - Stateless session backed by an HTTP-only cookie `FLSESSION` (30 min idle
>   expiry per FR-01).
> - BCrypt password hashing (strength 12).
> - HTTPS-only cookie flags; HSTS enabled in production profile.
> - Rate limit / lockout: after 3 failed logins lock account for 15 minutes and
>   require CAPTCHA on next attempt (NFR-06). Use Bucket4j or a simple in-memory
>   `LoginAttemptService`.
> - URL rules: `/api/auth/**` public, `/api/admin/**` requires `ADMIN`,
>   `/api/writer/**` requires `WRITER` or `ADMIN`, everything else authenticated.
> - 401 returns JSON `{error:"unauthenticated"}`, 403 returns
>   `{error:"forbidden"}` (NFR-07, NFR-15).

### 2.2 Auth endpoints

> Create `AuthController` with:
> - `POST /api/auth/login` — body `{email, password, captchaToken?}` →
>   `{role, displayName}`; sets session cookie.
> - `POST /api/auth/logout`.
> - `POST /api/auth/password/change` (authenticated) — body `{oldPassword, newPassword}`.
> - `POST /api/auth/password/reset/request` — body `{email}` — always returns
>   202; emails a one-time token valid 24h.
> - `POST /api/auth/password/reset/confirm` — body `{token, newPassword}`.
> Generic error message "Neplatný e-mail nebo heslo." for failed login.
> Tokens: random 32-byte URL-safe, stored hashed in `password_reset_tokens` table.

### 2.3 Audit interceptor

> Add a Spring AOP aspect that writes an `AuditLog` row for every successful
> login, logout, flight create/update/delete, CSV import, and CSV export.
> Log level rules per NFR-09.

---

## 3. ClubDB integration (FR-12, NFR-03)

> Refactor `integration/ClubUserDatabaseHttp.kt`:
> - Use Spring `RestClient` with a 15 s timeout.
> - Retry policy: 2 retries with delays of 2 s and 5 s on IO/5xx (Spring Retry).
> - Wrap calls in a `ClubUserService` that has two caches:
>   - **Short-term**: Caffeine, TTL 60 s, used as primary cache.
>   - **Long-term**: a JPA-backed `ClubUserSnapshot` table refreshed on every
>     successful API call; used as fallback when both API and short-term cache
>     are empty.
> - Expose `findById(memberId)`, `listPilots()`, `isPilot(memberId)`.
> - `isPilot` returns true only when `roles` contains `"PILOT"` (FR-12, FR-03
>   validation).
> - When fallback is used, set a request-scoped flag so controllers can include
>   `{"degraded": true, "message": "Data nemusí být aktuální"}` in responses
>   (NFR-03).

---

## 4. Flight write-side (FR-03, FR-04, FR-08, FR-09)

### 4.1 Create flight (`FR-03`)

> Implement `POST /api/writer/flights` accepting:
> ```json
> {
>   "date":"DD-MM-YYYY","pilotName":"...","gliderImmatriculation":"...",
>   "task":"...","takeoffTime":"HH:mm",
>   "startType":"WINCH"|"TOW",
>   "towPilotName":null,"towImmatriculation":null
> }
> ```
> Behavior:
> - When `startType=TOW`, `towPilotName` and `towImmatriculation` required.
> - Resolve airplanes by immatriculation (case-insensitive); reject unknown.
> - Resolve pilots via `ClubUserService.isPilot`; reject if not a pilot;
>   tag pilot as `clubMember=true/false` accordingly.
> - Persist either one `Flight` (winch) or two linked `Flight`s (tow + glider)
>   inside a single transaction.
> - Return the created flight(s) with computed `clubMember` flags.
> Add bean-validation annotations on the DTO (max-length 50/20/200 etc.).

### 4.2 Land flight (`FR-04`)

> Replace stub `LandOperation`. `POST /api/writer/flights/{id}/land` body
> `{"landingTime":"HH:mm"}`:
> - Reject if landing ≤ takeoff.
> - Update both linked flights when applicable (tow + glider).
> - Auto-compute `durationHours = (landing - takeoff) in hours`, rounded to 2
>   decimals.

### 4.3 Edit & manual create (`FR-08`, `FR-09`)

> Add admin endpoints:
> - `POST /api/admin/flights` — same payload as FR-03 but allows past dates and
>   manually setting landing time.
> - `PUT /api/admin/flights/{id}` — full update with the same validations.
> Both write `AuditLog` rows including before/after JSON in `details`.

### 4.4 Delete (`FR-10`)

> `DELETE /api/admin/flights/{id}?cascade=true|false`:
> - If the flight has a `linkedFlight` and `cascade=false`, only the link is
>   removed; otherwise both rows are deleted.
> - Always log to `AuditLog` (level WARN) before deletion.

---

## 5. Admin read-side & exports (FR-05, FR-06, FR-07)

### 5.1 Filtered listing

> `GET /api/admin/flights` query params: `dateFrom`, `dateTo`, `category`,
> `immatriculation`, `takeoffFrom`, `takeoffTo`, `durationMin`, `durationMax`,
> `inAirOnly`, `page` (0-based), `size` (10/25/50, default 25).
> Returns paginated JSON with totals. Sorted by takeoff time DESC.

### 5.2 CSV export (`FR-06`)

> Replace `GetExportToCsvOperation`:
> - Accept the same filter params as FR-05.
> - Stream rows via `StreamingResponseBody`; columns and header per docs.txt §3.2.2.
> - Separator `;`, UTF-8 (with BOM), date `DD-MM-YYYY`, time `HH:mm`,
>   duration with two decimals using `.`.
> - Filename `flightlog_export_DD-MM-YYYY.csv`.
> - When the count exceeds 1000, instead of streaming inline, enqueue a job
>   (use Spring `@Async` + an in-memory `ExportJobRegistry`), respond `202` with
>   `{jobId}`, and add `GET /api/admin/exports/{jobId}` to poll/download.

### 5.3 CSV import (`FR-07`)

> `POST /api/admin/flights/import` (multipart `file`):
> - Parse the whole file into memory first; **do not persist anything** unless
>   the entire file is valid.
> - Validate: required columns, types, `pristani > start`, every `pripojeno`
>   value must reference an `id` present elsewhere in the file.
> - On error, return `400` with array `{rowNumber, column, message}`.
> - On success, return `200` with `{toImport, errors:[]}` plus an opaque
>   `confirmationToken`.
> - `POST /api/admin/flights/import/confirm` with `{confirmationToken}` performs
>   the actual insert in one transaction; new DB ids are assigned, the file's
>   `id` column is used only to resolve `pripojeno` links inside the upload.

---

## 6. PWA / offline (FR-02, NFR-04)

> Update `src/main/resources/static/`:
> - Improve `service-worker.js` to use Workbox-style strategies:
>   - **Network-first** for `/api/admin/**` and `/api/auth/**`.
>   - **Stale-while-revalidate** for `/api/clubdb/users` and `/airplane`.
>   - **Cache-first** for static assets and the app shell.
> - Add an IndexedDB-backed outbox (`idb` library) for `POST /api/writer/flights`
>   and `/land` requests created while offline. Replay them on `online` event;
>   on conflict (server returned 409) surface a Czech notification "Záznam byl
>   přepsán novějšími daty".
> - Sync triggers within 5 s of regaining connectivity (NFR-04).
> - Show an offline banner when `navigator.onLine` is false.
> - Cache the `/api/clubdb/users` and `/airplane` payloads on every successful
>   fetch so writers can pick from dropdowns offline.

---

## 7. Frontend forms (writer + admin)

> Add minimal Czech-language Thymeleaf or static-HTML pages backed by the JSON
> APIs (do not introduce a new SPA framework — extend existing static assets):
> - `/login.html` — email + password.
> - `/writer/new-flight.html` — single-page form with conditional tow fields,
>   datepicker default = today, time inputs HH:mm, big tap targets (≥44 px),
>   high-contrast theme (NFR-10).
> - `/admin/flights.html` — table with filters, paging selector (10/25/50),
>   action buttons (edit, delete, export CSV, import CSV).
> - All inline validation errors render next to the offending field (NFR-15).

---

## 8. Cross-cutting NFRs

### 8.1 Logging & error handling (NFR-09, NFR-15)

> Add `GlobalExceptionHandler` (`@RestControllerAdvice`):
> - Maps `MethodArgumentNotValidException`, `ConstraintViolationException`,
>   `EntityNotFoundException`, custom `BusinessRuleException`, etc.
> - Never expose stack traces; respond with `{code, message, fieldErrors?}`.
> - Logs unexpected errors at ERROR level; business-rule failures at WARN.

### 8.2 Health, metrics, retention

> Enable Spring Actuator (`/actuator/health`, `/actuator/info`, `/actuator/metrics`)
> behind ADMIN-only authentication. Add a scheduled job that purges
> `audit_log` rows older than 12 months (NFR-09 retention).

### 8.3 Backups & deploy notes

> Append a `OPERATIONS.md` describing:
> - Daily `pg_dump` cron (RPO 24 h, RTO 24 h — NFR-13).
> - HTTPS termination (TLS 1.2+) at the reverse proxy.
> - Horizontal-scaling notes (stateless app, sticky sessions not required because
>   the session cookie is signed JWT — NFR-12).

### 8.4 Localization (NFR-14)

> Centralize date/time/decimal formatting in
> `src/main/kotlin/eu/profinit/flightlog/util/Formats.kt` and use it everywhere
> (CSV, JSON serialization with custom Jackson modules, UI). Default locale `cs-CZ`.

---

## 9. Tests (acceptance for each FR/NFR)

> For every controller add MockMvc tests; for every operation add unit tests with
> Mockito-Kotlin. Required scenarios:
> - FR-01: lockout after 3 fails; generic error message; password reset token
>   expiry; session expires after 30 min idle.
> - FR-03: tow + glider linkage; pilot rejection when not `PILOT` in ClubDB;
>   degraded-mode flag when ClubDB down.
> - FR-04: landing-before-takeoff rejected; duration computed.
> - FR-05/06: filtering and CSV format match `docs.txt §3.2.2` exactly — extend
>   `src/test/resources/expected/export.csv`.
> - FR-07: malformed file rejected atomically; valid file imported in one tx;
>   `pripojeno` links resolved.
> - FR-09/10: audit log entries written.
> - NFR-04 (offline): cypress / playwright test toggling `context.setOffline`
>   that creates a flight and verifies replay.
> - NFR-08: foreign-key violation when deleting a referenced glider without
>   cascade.

Run `./gradlew build` after each section. CI must stay green before moving on.

---

## 10. Final pass

> Re-read `docs.txt` and produce `TRACEABILITY.md` mapping every FR-XX and
> NFR-XX to the file(s) and test(s) that satisfy it. Flag any gaps and open
> follow-up issues with title prefix `[FlightLog]`.
