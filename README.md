# Flight Log (Kotlin / Spring Boot)

Flight log management system for glider clubs. Tracks takeoffs, landings, pilots, and aircraft, with CSV import/export, an admin dashboard, and a PWA-capable writer interface.

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

- `GET /airplane` — club airplanes
- `GET /user` — club members
- `GET /flight/InAir` — airplanes currently in air
- `POST /flight/Land` — land a flight
- `POST /flight/Takeoff` — take off
- `GET /flight/Report` — report
- `GET /flight/Export` — CSV export

## PWA

The backend serves PWA assets:

- `GET /manifest.webmanifest`
- `GET /service-worker.js` (served with `Cache-Control: no-store`)
- `GET /pwa-register.js`

To enable PWA in the Aurelia frontend (`../frontend/`), add these two lines to
`frontend/index.ejs` inside `<head>`:

```html
<link rel="manifest" href="/manifest.webmanifest">
<script src="/pwa-register.js" defer></script>
```

Then build the frontend and copy its dist contents into
`src/main/resources/static/` (replacing the placeholder `index.html`). You also need
`/icons/icon-192.png` and `/icons/icon-512.png` for the manifest to validate.

## Package layout

| Package | Responsibility |
| --- | --- |
| `eu.profinit.flightlog.controller` | HTTP controllers (REST + MVC) |
| `eu.profinit.flightlog.facade` | Orchestration / use-case facades |
| `eu.profinit.flightlog.operation` | Business logic operations |
| `eu.profinit.flightlog.model` | Request / response models |
| `eu.profinit.flightlog.integration` | External service clients (ClubDB) |
| `eu.profinit.flightlog.repository` | Data access (+ `.impl`, `.entity`, `.jpa`) |
