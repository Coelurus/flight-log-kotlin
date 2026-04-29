# Traceability — FlightLog (Kotlin)

Maps every requirement from `../docs.txt` to the implementing files / tests.

## Functional requirements

| Req | Implementation | Tests |
| --- | --- | --- |
| FR-01 Login / password reset / change / 30 min idle | [security/AuthService.kt](src/main/kotlin/eu/profinit/flightlog/security/AuthService.kt), [security/SecurityConfig.kt](src/main/kotlin/eu/profinit/flightlog/security/SecurityConfig.kt), [security/LoginAttemptService.kt](src/main/kotlin/eu/profinit/flightlog/security/LoginAttemptService.kt), [controller/AuthController.kt](src/main/kotlin/eu/profinit/flightlog/controller/AuthController.kt), session timeout in [resources/application.yml](src/main/resources/application.yml) | [security/AuthControllerIT.kt](src/test/kotlin/eu/profinit/flightlog/security/AuthControllerIT.kt) |
| FR-02 PWA / offline | [resources/static/service-worker.js](src/main/resources/static/service-worker.js), [resources/static/pwa-register.js](src/main/resources/static/pwa-register.js), [resources/static/manifest.webmanifest](src/main/resources/static/manifest.webmanifest) | _manual_ |
| FR-03 Create flight (writer) | [operation/FlightWriteService.kt](src/main/kotlin/eu/profinit/flightlog/operation/FlightWriteService.kt), [controller/WriterFlightController.kt](src/main/kotlin/eu/profinit/flightlog/controller/WriterFlightController.kt), [model/RequestModels.kt](src/main/kotlin/eu/profinit/flightlog/model/RequestModels.kt) | covered indirectly via parser/import tests + manual UI |
| FR-04 Add landing time | `FlightWriteService.land(...)` in [operation/FlightWriteService.kt](src/main/kotlin/eu/profinit/flightlog/operation/FlightWriteService.kt) | _manual_ |
| FR-05 Admin listing & filters | [operation/FlightQueryService.kt](src/main/kotlin/eu/profinit/flightlog/operation/FlightQueryService.kt), [controller/AdminFlightController.kt](src/main/kotlin/eu/profinit/flightlog/controller/AdminFlightController.kt) | _manual_ |
| FR-06 CSV export (sync + async >1000) | [operation/GetExportToCsvOperation.kt](src/main/kotlin/eu/profinit/flightlog/operation/GetExportToCsvOperation.kt), [operation/ExportJobRegistry.kt](src/main/kotlin/eu/profinit/flightlog/operation/ExportJobRegistry.kt) | [operation/GetExportToCsvOperationTests.kt](src/test/kotlin/eu/profinit/flightlog/operation/GetExportToCsvOperationTests.kt) |
| FR-07 CSV import (atomic, two-phase) | [operation/FlightImportService.kt](src/main/kotlin/eu/profinit/flightlog/operation/FlightImportService.kt), import endpoints in [controller/AdminFlightController.kt](src/main/kotlin/eu/profinit/flightlog/controller/AdminFlightController.kt) | [operation/FlightImportServiceTests.kt](src/test/kotlin/eu/profinit/flightlog/operation/FlightImportServiceTests.kt) |
| FR-08 Manual admin create | `AdminFlightController.manualCreate(...)` reuses `FlightWriteService.create` | _manual_ |
| FR-09 Edit confirmed flights | `FlightWriteService.update(...)` writes audit log | _manual_ |
| FR-10 Delete + cascade for tow link | `FlightWriteService.delete(id, cascade)` | _manual_ |
| FR-11 Aircraft registry | [repository/entity/Airplane.kt](src/main/kotlin/eu/profinit/flightlog/repository/entity/Airplane.kt) (+ `category`) and existing `ClubAirplane` repos | _manual_ |
| FR-12 ClubDB integration with retry/cache/timeout | [integration/ClubUserService.kt](src/main/kotlin/eu/profinit/flightlog/integration/ClubUserService.kt), [integration/DegradedModeHolder.kt](src/main/kotlin/eu/profinit/flightlog/integration/DegradedModeHolder.kt) | _manual_ |
| FR-13 User management by supplier | seed in [security/AuthService.kt](src/main/kotlin/eu/profinit/flightlog/security/AuthService.kt) (`DefaultUserSeeder`), no self-service UI | n/a |

## Non-functional requirements

| Req | Implementation |
| --- | --- |
| NFR-01 Availability | [OPERATIONS.md](OPERATIONS.md) §1, Spring Boot Actuator `/actuator/health` |
| NFR-02 Response < 2 s | indexes + JPA Specifications in [repository/jpa/JpaRepositories.kt](src/main/kotlin/eu/profinit/flightlog/repository/jpa/JpaRepositories.kt) |
| NFR-03 ClubDB degraded mode | [integration/ClubUserService.kt](src/main/kotlin/eu/profinit/flightlog/integration/ClubUserService.kt) + [integration/DegradedModeHolder.kt](src/main/kotlin/eu/profinit/flightlog/integration/DegradedModeHolder.kt) propagated in `FlightResponse.degraded` |
| NFR-04 Offline + replay <5 s | `service-worker.js` outbox + `pwa-register.js` `online` listener |
| NFR-05 HTTPS only | [OPERATIONS.md](OPERATIONS.md) §3 + cookie `secure` flag in [resources/application.yml](src/main/resources/application.yml) |
| NFR-06 BCrypt + lockout + CAPTCHA gating | `BCryptPasswordEncoder(12)` in [security/SecurityConfig.kt](src/main/kotlin/eu/profinit/flightlog/security/SecurityConfig.kt), [security/LoginAttemptService.kt](src/main/kotlin/eu/profinit/flightlog/security/LoginAttemptService.kt) |
| NFR-07 Backend-side authz | `@PreAuthorize` on writer/admin controllers, `SecurityConfig.requestMatchers(...)` |
| NFR-08 Integrity + transactions | `@Transactional` services, JPA FK/uniqueness via entity annotations |
| NFR-09 Audit log + 12 mo retention | [audit/AuditService.kt](src/main/kotlin/eu/profinit/flightlog/audit/AuditService.kt) + scheduled `purgeOlderThanRetention()` |
| NFR-10 Usability (44 px, contrast, ≤3 interactions) | [resources/static/writer/new-flight.html](src/main/resources/static/writer/new-flight.html), [resources/static/admin/flights.html](src/main/resources/static/admin/flights.html), [resources/static/login.html](src/main/resources/static/login.html) |
| NFR-11 Browser compat | static HTML uses standard ES2017+ APIs |
| NFR-12 Horizontal scaling | stateless API + session cookie (no in-process state apart from import tokens — flagged in [OPERATIONS.md](OPERATIONS.md) §4) |
| NFR-13 Backups | [OPERATIONS.md](OPERATIONS.md) §2 |
| NFR-14 Date / time / decimal formats | [util/Formats.kt](src/main/kotlin/eu/profinit/flightlog/util/Formats.kt) used by CSV + DTO converters |
| NFR-15 Error UX | [web/GlobalExceptionHandler.kt](src/main/kotlin/eu/profinit/flightlog/web/GlobalExceptionHandler.kt) emits structured `{code, message, fieldErrors}`; never a stack trace |

## Known follow-ups

- `[FlightLog]` Replace in-memory `pendingImports` / `ExportJobRegistry` with Redis when running >1 replica (see NFR-12 caveat).
- `[FlightLog]` Send password-reset emails (currently logged at INFO).
- `[FlightLog]` Hook a real CAPTCHA provider (`captchaToken` is currently only checked for non-blankness).
- `[FlightLog]` Add Cypress / Playwright e2e for the offline-replay flow described under NFR-04.
- `[FlightLog]` Replace JPA `ddl-auto: update` with Flyway migrations once schema stabilises.
