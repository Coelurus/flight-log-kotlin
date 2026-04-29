# Flight Log (Kotlin / Spring Boot)

Kotlin port of the original ASP.NET project located in [`../flight-log-dotnet/`](../flight-log-dotnet/).

## Stack

- Kotlin 2.1, JVM 21
- Spring Boot 3.4 (Web, Data JPA, Validation)
- PostgreSQL 14+ (production), H2 in PostgreSQL-compat mode (tests)
- JUnit 5 + Mockito-Kotlin
- Gradle (Kotlin DSL)

## Build & run

```bash
# From this directory
gradle wrapper            # one-off; generates ./gradlew + wrapper jar
./gradlew build           # compile + tests
./gradlew bootRun --args='--spring.profiles.active=dev'
```

The dev profile uses `ddl-auto: create-drop` and seeds test data on startup.
Set `DB_URL`, `DB_USER`, `DB_PASSWORD` env vars to point at your PostgreSQL instance
(default `jdbc:postgresql://localhost:5432/flightlog`, user/password `flightlog`/`flightlog`).

## Endpoints

Same routes as the .NET version:

- `GET /airplane` — club airplanes
- `GET /user` — club members
- `GET /flight/InAir` — airplanes currently in air *(stub — TODO 2.5)*
- `POST /flight/Land` — land a flight
- `POST /flight/Takeoff` — take off
- `GET /flight/Report` — report
- `GET /flight/Export` — CSV export *(stub — TODO 5.1)*

## PWA

The backend serves PWA assets:

- `GET /manifest.webmanifest`
- `GET /service-worker.js` (served with `Cache-Control: no-store`)
- `GET /pwa-register.js`

To enable PWA in the existing Aurelia frontend (kept untouched in
`../frontend/`), add these two lines to `frontend/index.ejs` inside `<head>`:

```html
<link rel="manifest" href="/manifest.webmanifest">
<script src="/pwa-register.js" defer></script>
```

Then build the frontend and copy its dist contents into
`src/main/resources/static/` (replacing the placeholder `index.html`). You also need
`/icons/icon-192.png` and `/icons/icon-512.png` for the manifest to validate.

## Layout (mirrors the C# project)

| C# namespace | Kotlin package |
| --- | --- |
| `FlightLogNet.Controllers` | `eu.profinit.flightlog.controller` |
| `FlightLogNet.Facades` | `eu.profinit.flightlog.facade` |
| `FlightLogNet.Operation` | `eu.profinit.flightlog.operation` |
| `FlightLogNet.Models` | `eu.profinit.flightlog.model` |
| `FlightLogNet.Integration` | `eu.profinit.flightlog.integration` |
| `FlightLogNet.Repositories` | `eu.profinit.flightlog.repository` (+ `.impl`, `.entity`, `.jpa`) |

## Mapping notes

- `IConfiguration` → Spring `@Value` / `application.yml`
- EF Core `DbContext` → Spring Data JPA repositories under `repository/jpa/`
- `bool TryGet(..., out long id)` → returns `TryGetResult(found, id)`
- `IServiceCollection.AddScoped(...)` → `@Component` / `@Repository` (Spring scans
  the `eu.profinit.flightlog` package automatically).
- `RestSharp` → Spring `RestClient`
- `log4net` → Logback (`logback-spring.xml`)
- `Microsoft.EntityFrameworkCore.Sqlite` is dropped per project decision (PostgreSQL only).

The original `// TODO X.Y` teaching markers are preserved verbatim in the corresponding
Kotlin files so the tutorial flow still works.
