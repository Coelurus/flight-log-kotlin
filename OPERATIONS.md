# Operations runbook

## 1. Deployment

- Stateless Spring Boot app + PostgreSQL 14+. Run multiple instances behind a
  load balancer that performs HTTPS termination (TLS 1.2+).
- Env vars: `DB_URL`, `DB_USER`, `DB_PASSWORD`, optional `ALLOWED_ORIGINS`.
- Health probe: `GET /actuator/health` (open). Metrics: `GET /actuator/metrics`
  (admin-only).

## 2. Backups (NFR-13)

```bash
0 2 * * *  pg_dump -Fc flightlog | gzip > /backups/flightlog-$(date +\%F).dump.gz
```

- Replicate `/backups/` to a geographically separate bucket.
- RPO ≤ 24 h, RTO ≤ 24 h. Restore: `pg_restore -d flightlog backup.dump`.

## 3. HTTPS / security

- Reverse proxy enforces HSTS + redirects HTTP→HTTPS.
- Session cookie `FLSESSION` is `HttpOnly`, `SameSite=Lax`. Mark `secure: true`
  on the cookie when running behind HTTPS-only ingress.

## 4. Scaling caveats

The default build keeps a few small caches in-process:

- CSV import confirmation tokens (`FlightImportService.pendingImports`)
- Async export jobs (`ExportJobRegistry.jobs`)
- ClubDB short-term Caffeine cache

When scaling beyond a single replica, either pin requests to one node (sticky
sessions) or move these caches to Redis / equivalent.

## 5. Default seeded accounts

| Email | Password | Role |
| --- | --- | --- |
| `admin@flightlog.local` | `admin1234` | ADMIN |
| `writer@flightlog.local` | `writer1234` | WRITER |

Change passwords immediately after first deploy via `POST /api/auth/password/change`.

## 6. Audit log retention

The scheduled job `AuditService.purgeOlderThanRetention()` runs daily at
03:30 UTC and deletes audit_log rows older than 13 months.
