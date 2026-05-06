# Nasazení a provoz — FlightLog

**Verze dokumentu:** 1.0  
**Datum:** 06-05-2026  
**Autoři:** Provozní tým  
**Stav:** Platný

---

## Obsah

1. [Přehled prostředí](#1-přehled-prostředí)
2. [Požadavky na infrastrukturu](#2-požadavky-na-infrastrukturu)
3. [Proměnné prostředí](#3-proměnné-prostředí)
4. [Build a sestavení aplikace](#4-build-a-sestavení-aplikace)
5. [Lokální provoz (Docker Compose)](#5-lokální-provoz-docker-compose)
6. [Produkční nasazení (Kubernetes)](#6-produkční-nasazení-kubernetes)
7. [Správa databáze](#7-správa-databáze)
8. [Zálohování a obnova](#8-zálohování-a-obnova)
9. [Monitoring a logování](#9-monitoring-a-logování)
10. [Bezpečnost provozu](#10-bezpečnost-provozu)
11. [První spuštění a výchozí účty](#11-první-spuštění-a-výchozí-účty)
12. [Řešení běžných problémů](#12-řešení-běžných-problémů)

---

## 1 Přehled prostředí

```
┌─────────────────────────────────────────────────────┐
│  Cloudová infrastruktura (AWS / Azure / OCI)        │
│                                                     │
│   ┌──────────────┐     ┌──────────────────────┐    │
│   │  Kubernetes  │     │  Managed PostgreSQL  │    │
│   │  cluster     │──►  │  (nebo StatefulSet)  │    │
│   │              │     └──────────────────────┘    │
│   │  flightlog   │                                  │
│   │  namespace   │     ┌──────────────────────┐    │
│   │              │──►  │  ELK stack           │    │
│   └──────────────┘     │  (ES + Logstash +    │    │
│          │             │   Kibana)            │    │
│          ▼             └──────────────────────┘    │
│   ┌──────────────┐                                  │
│   │  Ingress /   │◄── HTTPS (TLS 1.2+)             │
│   │  LB          │    od uživatelů                  │
│   └──────────────┘                                  │
└─────────────────────────────────────────────────────┘
         │
         ▼ HTTP (interní)
  http://vyuka.profinit.eu:8080/  (ClubDB — ext. API)
```

### Technologický zásobník

| Komponenta | Technologie | Verze |
|---|---|---|
| Aplikace | Spring Boot / Kotlin / Java | 3.4 / 2.1 / 21 |
| Databáze | PostgreSQL | 16 |
| Kontejnerizace | Docker | 24+ |
| Orchestrace | Kubernetes + kustomize | 1.29+ |
| Logování | Logstash / Elasticsearch / Kibana | 8.15.3 |
| Base image (JRE) | Eclipse Temurin | 21-jre |

---

## 2 Požadavky na infrastrukturu

### 2.1 Minimální požadavky na produkci

| Komponenta | CPU | RAM | Disk |
|---|---|---|---|
| Aplikace (pod) | 200m–2 | 512 Mi–1 Gi | — (stateless) |
| PostgreSQL | 100m–1 | 256 Mi–1 Gi | 5 Gi PVC |
| Elasticsearch | 500m–2 | 1 Gi–2 Gi | 10 Gi PVC |
| Logstash | 200m–1 | 256 Mi–512 Mi | — |
| Kibana | 200m–1 | 512 Mi–1 Gi | — |

### 2.2 Předpoklady pro lokální vývoj

- Docker Engine 24+ s Docker Compose v2
- JDK 21 (Eclipse Temurin doporučen)
- Gradle 8+ (nebo `./gradlew` wrapper — doporučeno)
- Volné porty: `8080`, `5432`, `5050`, `9200`, `5044`, `5601`

---

## 3 Proměnné prostředí

### 3.1 Aplikace (Spring Boot)

| Proměnná | Povinná | Výchozí hodnota | Popis |
|---|:---:|---|---|
| `DB_URL` | ✓ | `jdbc:postgresql://localhost:5432/flightlog` | JDBC URL PostgreSQL databáze |
| `DB_USER` | ✓ | `flightlog` | Uživatel databáze |
| `DB_PASSWORD` | ✓ | `flightlog` | Heslo databáze — **změnit v produkci!** |
| `ALLOWED_ORIGINS` | — | *(prázdné)* | Čárkami oddělené CORS originy, např. `https://flightlog.example.com` |
| `ENV` | — | `dev` | Označení prostředí (dev / k8s / prod); vkládá se do JSON logů jako `env` |
| `LOGSTASH_HOST` | — | `localhost` | Hostname Logstash TCP appenderu |
| `LOGSTASH_PORT` | — | `5044` | Port Logstash |
| `LOGSTASH_ENABLED` | — | `true` | Vypnutí Logstash shipmentu (`false` pro čisté lokální logy) |
| `JAVA_OPTS` | — | `-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC` | JVM parametry předané při startu kontejneru |

> **Bezpečnostní poznámka:** Hodnoty `DB_PASSWORD` a jakékoliv budoucí API klíče nikdy neukládejte do ConfigMap — použijte Kubernetes `Secret` nebo secret management (HashiCorp Vault, AWS Secrets Manager, Azure Key Vault).

### 3.2 PostgreSQL kontejner

| Proměnná | Hodnota |
|---|---|
| `POSTGRES_DB` | `flightlog` |
| `POSTGRES_USER` | `flightlog` |
| `POSTGRES_PASSWORD` | *(viz Secret `flightlog-db`)* |

### 3.3 Logback (logback-spring.xml)

Logback číst konfiguraci z proměnných Spring prostředí — lze přepsat přes env proměnné:

| Spring property | Env proměnná (ekvivalent) | Popis |
|---|---|---|
| `logging.logstash.host` | `LOGSTASH_HOST` | Host Logstash |
| `logging.logstash.port` | `LOGSTASH_PORT` | Port Logstash |
| `logging.logstash.enabled` | `LOGSTASH_ENABLED` | Zapnout/vypnout TCP appender |

---

## 4 Build a sestavení aplikace

### 4.1 Sestavení JAR (lokálně)

```bash
# Plný build včetně testů
./gradlew clean build

# Pouze produkční JAR, bez testů (rychlejší)
./gradlew clean bootJar -x test

# Výsledek:
# build/libs/flight-log-kotlin-<verze>.jar
```

> Gradle wrapper (`./gradlew`) nevyžaduje lokální instalaci Gradle. JDK 21 musí být dostupné na `PATH`.

### 4.2 Spuštění jednotkových testů

```bash
# Rychlé unit testy (bez Spring kontextu, bez DB)
./gradlew unitTest

# Kompletní test suite (včetně integračních testů — vyžaduje Docker pro Testcontainers)
./gradlew test
```

### 4.3 Sestavení Docker obrazu

```bash
# Sestavení obrazu — multi-stage build (builder: JDK 21, runtime: JRE 21)
docker build -t flightlog/app:latest .

# Označení konkrétní verze
docker build -t flightlog/app:$(./gradlew properties -q | grep "^version:" | awk '{print $2}') .
```

**Jak funguje multi-stage build (`Dockerfile`):**

1. **Builder stage** (`eclipse-temurin:21-jdk`) — závislosti Gradle jsou cachované v samostatné vrstvě, aby opakované buildy byly rychlejší; spustí `bootJar -x test`.
2. **Runtime stage** (`eclipse-temurin:21-jre`) — kopíruje pouze výsledný JAR; aplikace běží jako neprivilegovaný uživatel `flightlog` (UID 1001).

### 4.4 Pushnutí obrazu do registry

```bash
# Příklad pro Docker Hub
docker tag flightlog/app:latest your-registry/flightlog/app:latest
docker push your-registry/flightlog/app:latest

# Aktualizace image v k8s/50-app.yaml:
# image: your-registry/flightlog/app:latest
```

---

## 5 Lokální provoz (Docker Compose)

### 5.1 Spuštění celého zásobníku

```bash
cd flight-log-kotlin

# Spustí: PostgreSQL, pgAdmin, Elasticsearch, Logstash, Kibana
docker compose up -d

# Aplikaci spusťte lokálně (s dev profilem):
./gradlew bootRun --args='--spring.profiles.active=dev'

# Nebo v Dockeru (bez ELK):
docker compose up -d postgres
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 5.2 Dostupné služby po startu

| Služba | URL | Přihlašovací údaje |
|---|---|---|
| FlightLog aplikace | http://localhost:8080 | viz [sekce 11](#11-první-spuštění-a-výchozí-účty) |
| pgAdmin | http://localhost:5050 | `admin@flightlog.dev` / `admin` |
| Kibana | http://localhost:5601 | *(bez hesla — dev)* |
| Elasticsearch | http://localhost:9200 | *(bez hesla — dev)* |
| PostgreSQL | `localhost:5432` | `flightlog` / `flightlog` |

> **Poznámka:** pgAdmin je předkonfigurováno pro prohlížení DB. Po přihlášení přidejte server s host `postgres`, port `5432`, user `flightlog`, heslo `flightlog`.

### 5.3 Zastavení a čistění

```bash
# Zastavit bez smazání dat
docker compose down

# Zastavit a smazat všechna persistentní data (volumes)
docker compose down -v
```

### 5.4 Dev profil (`application-dev.yml`)

Při spuštění s profilem `dev` se aktivuje:
- `ddl-auto: create-drop` — schema je regenerováno při každém startu
- `show-sql: true` — SQL dotazy v konzoli
- `flightlog.seed-test-data: true` — automatické naplnění testovacími daty (`DevDataSeeder`)
- Logování na úrovni `TRACE` pro balíček `eu.profinit.flightlog`

---

## 6 Produkční nasazení (Kubernetes)

### 6.1 Předpoklady

- `kubectl` nakonfigurovaný na cílový cluster
- Container image dostupný v registry přístupném z clusteru
- `kustomize` (součást `kubectl` od verze 1.14)

### 6.2 Nasazení krok za krokem

```bash
cd flight-log-kotlin/k8s

# 1. Ověřte připojení ke clusteru
kubectl cluster-info

# 2. (Volitelné) Zkontrolujte, co bude nasazeno
kubectl kustomize . | less

# 3. Nasaďte celý zásobník
kubectl apply -k .

# 4. Sledujte stav nasazení
kubectl rollout status deployment/flightlog -n flightlog
kubectl get pods -n flightlog -w
```

### 6.3 Pořadí nasazení (kustomization.yaml)

Kustomize nasazuje manifesty v tomto pořadí:

| Krok | Soubor | Obsah |
|---|---|---|
| 1 | `00-namespace.yaml` | Namespace `flightlog` |
| 2 | `05-config.yaml` | Secret s DB credentials + ConfigMap s env |
| 3 | `10-postgres.yaml` | StatefulSet PostgreSQL + PVC 5 Gi + headless Service |
| 4 | `20-elasticsearch.yaml` | StatefulSet Elasticsearch + PVC 10 Gi |
| 5 | `30-logstash.yaml` | Deployment Logstash |
| 6 | `40-kibana.yaml` | Deployment Kibana |
| 7 | `50-app.yaml` | Deployment aplikace + Service + Ingress |

### 6.4 Aktualizace aplikace (rolling update)

```bash
# Aktualizace image bez výpadku (rolling update)
kubectl set image deployment/flightlog \
  flightlog=your-registry/flightlog/app:<nová-verze> \
  -n flightlog

# Sledování průběhu
kubectl rollout status deployment/flightlog -n flightlog

# Rollback při problémech
kubectl rollout undo deployment/flightlog -n flightlog
```

### 6.5 Konfigurace secrets pro produkci

Výchozí `05-config.yaml` obsahuje hesla v plaintextu — **nahraďte** před produkčním nasazením:

```bash
# Způsob 1: kubectl create secret (jednorázový)
kubectl create secret generic flightlog-db \
  --from-literal=POSTGRES_DB=flightlog \
  --from-literal=POSTGRES_USER=flightlog \
  --from-literal=POSTGRES_PASSWORD='<silné-heslo>' \
  -n flightlog

# Způsob 2: SealedSecrets (doporučeno pro GitOps)
# Způsob 3: External Secrets Operator (AWS Secrets Manager / Vault)
```

### 6.6 Konfigurace Ingress

Upravte hostname v `50-app.yaml` dle vaší domény:

```yaml
rules:
  - host: flightlog.vasedomena.cz        # ← změňte
    http:
      paths:
        - path: /
          pathType: Prefix
          backend:
            service:
              name: flightlog
              port: { number: 80 }
  - host: kibana.flightlog.vasedomena.cz  # ← změňte (nebo odstraňte)
```

Pro HTTPS přidejte TLS sekci a cert-manager anotaci:

```yaml
metadata:
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  tls:
    - hosts: [flightlog.vasedomena.cz]
      secretName: flightlog-tls
```

### 6.7 Horizontální škálování (HPA)

> **Upozornění:** Více replik aplikace **vyžaduje** přesunutí HTTP session mimo paměť JVM. Viz níže.

Aplikace v `k8s/50-app.yaml` je konfigurována na `replicas: 1`. Před škálováním:

1. Přidejte závislost `spring-session-data-redis` do `build.gradle.kts`
2. Nasaďte Redis (např. Bitnami Helm chart)
3. Nastavte proměnné `SPRING_SESSION_STORE_TYPE=redis` a `SPRING_REDIS_HOST=<host>`

```bash
# Po splnění výše uvedených podmínek:
kubectl scale deployment/flightlog --replicas=3 -n flightlog
```

In-memory caches, které blokují škálování (musí být přesunuty do Redis):
- `FlightImportService.pendingImports` — potvrzovací tokeny CSV importu
- `ExportJobRegistry.jobs` — záznamy asynchronních export jobů
- `ClubUserService` Caffeine cache — krátkodobá cache ClubDB

---

## 7 Správa databáze

### 7.1 Flyway migrace

Migrace se spouštějí **automaticky při startu aplikace**. Stav migrací:

```bash
# Zobrazit stav migrací (přímo v DB)
kubectl exec -it $(kubectl get pod -l app=postgres -n flightlog -o name) \
  -n flightlog -- psql -U flightlog -d flightlog \
  -c "SELECT version, description, installed_on, success FROM flyway_schema_history ORDER BY installed_rank;"
```

Přidání nové migrace — vytvořte soubor v `src/main/resources/db/migration/`:

```
V3__popis_zmeny.sql
```

> Flyway je nastaven na `baseline-version=0`. Na čistém DB proběhne `V1__baseline.sql` + `V2__indexes.sql`. Na existujících DB s verzí 1 pokračuje od `V2`.

### 7.2 Přímý přístup k databázi

```bash
# kubectl port-forward (lokální přístup k produkční DB — opatrně!)
kubectl port-forward svc/postgres 5432:5432 -n flightlog

# Připojení
psql -h localhost -U flightlog -d flightlog

# Nebo přes pgAdmin (lokální prostředí):
http://localhost:5050
```

### 7.3 Správa uživatelských účtů

Správa účtů je výhradně na straně dodavatele (FR-13). Přidání nového uživatele:

```sql
-- Vložení nového uživatele (bcrypt hash pro heslo 'Heslo1234!')
-- Generování hash: htpasswd -bnBC 12 "" 'Heslo1234!' | tr -d ':\n'
INSERT INTO users (email, password_hash, role, active, created_at)
VALUES ('novy@flightlog.local', '$2a$12$...hash...', 'WRITER', true, now());
```

---

## 8 Zálohování a obnova

### 8.1 Strategie zálohování

| Metriky | Cíl | Poznámka |
|---|---|---|
| RPO (max. ztráta dat) | ≤ 24 hodin | NFR-13 |
| RTO (max. doba obnovy) | ≤ 24 hodin | NFR-13 |
| Frekvence zálohy | 1× denně | Doporučeno: 02:00 UTC |
| Umístění zálohy | Geograficky oddělená lokalita | Jiný region / jiný cloud |
| Retence | Minimálně 30 dní | Doporučeno: 90 dní |

### 8.2 Automatická záloha (cron)

```bash
# /etc/cron.d/flightlog-backup  (na zálohovacím hostu nebo jako Kubernetes CronJob)
0 2 * * * pg_dump -Fc -h <postgres-host> -U flightlog flightlog \
  | gzip > /backups/flightlog-$(date +%F).dump.gz
```

**Kubernetes CronJob** (doporučeno pro produkci):

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: flightlog-backup
  namespace: flightlog
spec:
  schedule: "0 2 * * *"
  jobTemplate:
    spec:
      template:
        spec:
          containers:
            - name: pg-dump
              image: postgres:16-alpine
              command:
                - sh
                - -c
                - |
                  pg_dump -Fc -h postgres -U $POSTGRES_USER $POSTGRES_DB \
                    | gzip > /backup/flightlog-$(date +%F).dump.gz
              envFrom:
                - secretRef:
                    name: flightlog-db
              volumeMounts:
                - name: backup
                  mountPath: /backup
          restartPolicy: OnFailure
          volumes:
            - name: backup
              persistentVolumeClaim:
                claimName: flightlog-backup-pvc  # PVC namontovaný do zálohovacího úložiště
```

### 8.3 Synchronizace zálohy do objektového úložiště

```bash
# AWS S3
aws s3 sync /backups/ s3://flightlog-backups/db/ \
  --storage-class STANDARD_IA \
  --delete

# Azure Blob Storage
azcopy sync /backups/ "https://<account>.blob.core.windows.net/flightlog-backups/db/"
```

### 8.4 Postup obnovy z zálohy

```bash
# 1. Stažení zálohy z úložiště
aws s3 cp s3://flightlog-backups/db/flightlog-2026-05-06.dump.gz /tmp/

# 2. Rozbalení
gunzip /tmp/flightlog-2026-05-06.dump.gz

# 3. Zastavení aplikace (aby nedocházelo k zápisům)
kubectl scale deployment/flightlog --replicas=0 -n flightlog

# 4. Obnova databáze
# POZOR: příkaz smaže a znovu vytvoří databázi!
kubectl exec -it $(kubectl get pod -l app=postgres -n flightlog -o name) \
  -n flightlog -- bash -c \
  "dropdb -U flightlog flightlog && createdb -U flightlog flightlog"

pg_restore -h <postgres-host> -U flightlog -d flightlog \
  /tmp/flightlog-2026-05-06.dump

# 5. Ověření integrity dat
psql -h <postgres-host> -U flightlog -d flightlog \
  -c "SELECT count(*) FROM flights; SELECT count(*) FROM users;"

# 6. Restart aplikace
kubectl scale deployment/flightlog --replicas=1 -n flightlog
kubectl rollout status deployment/flightlog -n flightlog
```

### 8.5 Záloha Elasticsearch (audit logy)

Elasticsearch index `flightlog-*` obsahuje logy a audit záznamy. Pro zálohu použijte Snapshot API:

```bash
# Registrace úložiště snapsnotu (S3)
curl -X PUT "http://elasticsearch:9200/_snapshot/flightlog_backup" \
  -H 'Content-Type: application/json' \
  -d '{"type":"s3","settings":{"bucket":"flightlog-es-backups","region":"eu-central-1"}}'

# Vytvoření snapshotu
curl -X PUT "http://elasticsearch:9200/_snapshot/flightlog_backup/$(date +%F)" \
  -H 'Content-Type: application/json' \
  -d '{"indices":"flightlog-*","include_global_state":false}'
```

---

## 9 Monitoring a logování

### 9.1 Health endpointy

| Endpoint | Přístup | Popis |
|---|---|---|
| `/actuator/health` | Veřejný | Celkový stav aplikace (UP/DOWN) |
| `/actuator/health/readiness` | Veřejný | Připravenost přijímat provoz (DB ping) |
| `/actuator/health/liveness` | Veřejný | JVM běží (Kubernetes liveness probe) |
| `/actuator/info` | Veřejný | Verze aplikace, metadata |
| `/actuator/metrics` | Pouze ADMIN | Metriky (JVM, HTTP, cache, DB pool) |

```bash
# Ruční kontrola stavu
curl -s http://localhost:8080/actuator/health | jq .

# Příklad odpovědi při zdravém stavu:
# {"status":"UP","components":{"db":{"status":"UP"},"diskSpace":{"status":"UP"}}}
```

### 9.2 Kubernetes proby

Aplikace má v `50-app.yaml` nakonfigurované proby:

| Typ | Endpoint | Prodleva | Interval |
|---|---|---|---|
| Readiness | `/actuator/health/readiness` | 20 s | 5 s (24 pokusů) |
| Liveness | `/actuator/health/liveness` | 60 s | 15 s |
| Docker HEALTHCHECK | `/actuator/health` | 40 s | 15 s (10 pokusů) |

### 9.3 ELK stack — logy

```
Aplikace (Logback TCP appender)
    │  JSON lines přes TCP 5044
    ▼
Logstash (flightlog.conf)
    │  Obohacení: event_kind (audit/access/app), konverze typů
    ▼
Elasticsearch (index: flightlog-YYYY.MM.dd)
    │
    ▼
Kibana (http://kibana:5601)
```

**Přihlášení do Kibana (produkce):**

```
URL:      http://kibana.flightlog.vasedomena.cz
Index:    flightlog-*
Časové pole: @timestamp
```

**Doporučené Kibana dashboardy:**

| Dashboard | Filtr | Účel |
|---|---|---|
| Audit trail | `event_kind: audit` | Sledování akcí uživatelů |
| HTTP access | `event_kind: access` | Analýza provozu, chybové kódy |
| Aplikační chyby | `level: ERROR` | Detekce výjimek |
| Degradovaný režim | `message: *degraded*` | Výpadky ClubDB API |

### 9.4 Lokální logy (soubor)

Logback zapisuje rotující JSON soubory do `logs/`:

```
logs/
├── app.json                    ← aktuální log
├── app-2026-05-05.0.json.gz    ← rotovaný (max 20 MB)
└── app-2026-05-04.0.json.gz    ← uchovávány 14 dní, max 500 MB celkem
```

### 9.5 Audit log v databázi

Auditní záznamy jsou uloženy v tabulce `audit_log`:

```sql
-- Přehled akcí za poslední den
SELECT timestamp, user_email, action, entity_type, entity_id, level
FROM audit_log
WHERE timestamp > now() - interval '1 day'
ORDER BY timestamp DESC;

-- Lety smazané za poslední týden
SELECT *
FROM audit_log
WHERE action = 'DELETE_FLIGHT'
  AND timestamp > now() - interval '7 days';
```

**Retence:** Záznamy starší než 13 měsíců jsou automaticky mazány každý den v **03:30 UTC** (`AuditService.purgeOlderThanRetention()`).

### 9.6 Sledování degradovaného režimu (ClubDB)

```bash
# Vyhledání výskytů degradovaného režimu v Elasticsearch
curl -s "http://elasticsearch:9200/flightlog-*/_search" \
  -H 'Content-Type: application/json' \
  -d '{
    "query": {"match": {"message": "degraded"}},
    "sort": [{"@timestamp": "desc"}],
    "size": 10
  }' | jq '.hits.hits[]._source | {timestamp: .["@timestamp"], message}'
```

---

## 10 Bezpečnost provozu

### 10.1 Checklis před prvním nasazením do produkce

- [ ] Změnit výchozí DB heslo (viz [sekce 6.5](#65-konfigurace-secrets-pro-produkci))
- [ ] Změnit hesla výchozích účtů (viz [sekce 11](#11-první-spuštění-a-výchozí-účty))
- [ ] Nastavit proměnnou `ALLOWED_ORIGINS` na produkční doménu
- [ ] Zapnout `secure: true` na session cookie (HTTPS Ingress)
- [ ] Nastavit retenční politiku pro Elasticsearch indexy
- [ ] Ověřit TLS certifikát na Ingressu
- [ ] Zkontrolovat, že `/actuator/metrics` a `/actuator/**` jsou dostupné pouze pro ADMIN
- [ ] Nastavit zálohovací CronJob a otestovat obnovu

### 10.2 Session cookie

Session cookie `FLSESSION` má tyto vlastnosti:

| Vlastnost | Hodnota | Popis |
|---|---|---|
| `HttpOnly` | `true` | Nedostupné pro JavaScript |
| `SameSite` | `Lax` | Ochrana před CSRF |
| `Secure` | Nastavit při HTTPS | Přidat do `application.yml` za Ingress |
| Timeout | 30 minut inaktivity | Konfigurovatelné přes `server.servlet.session.timeout` |

Přidání `Secure` příznaku za HTTPS proxy:

```yaml
# application.yml
server:
  servlet:
    session:
      cookie:
        secure: true
```

### 10.3 Ochrana přihlašovací brány

- Po **3 neúspěšných** pokusech přihlášení je aktivována CAPTCHA ochrana (`LoginAttemptService`)
- Reset hesla funguje přes jednorázový token s platností **max. 24 hodin** zaslaný na e-mail
- Hesla jsou hashována algoritmem **BCrypt s cost factor 12**

### 10.4 Síťová politika (doporučeno)

Pro produkci doporučujeme Kubernetes NetworkPolicy omezující komunikaci:

```yaml
# Příklad: aplikace smí komunikovat pouze s postgres a logstash
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: flightlog-egress
  namespace: flightlog
spec:
  podSelector:
    matchLabels: { app: flightlog }
  policyTypes: [Egress]
  egress:
    - to:
        - podSelector: { matchLabels: { app: postgres } }
      ports: [{ port: 5432 }]
    - to:
        - podSelector: { matchLabels: { app: logstash } }
      ports: [{ port: 5044 }]
    - ports: [{ port: 443 }, { port: 80 }]   # ClubDB externi API + SMTP
```

---

## 11 První spuštění a výchozí účty

### 11.1 Výchozí seeded účty (dev / první nasazení)

> **Kriticky důležité:** Změňte tato hesla **okamžitě po prvním přihlášení** v produkci.

| E-mail | Heslo | Role |
|---|---|---|
| `admin@flightlog.local` | `admin1234` | ADMIN |
| `writer@flightlog.local` | `writer1234` | WRITER |

### 11.2 Změna hesla

```
POST /api/auth/password/change
Content-Type: application/json
Cookie: FLSESSION=<session>

{
  "currentPassword": "admin1234",
  "newPassword": "NoveHeslo2026!"
}
```

### 11.3 Přidání nového uživatele (SQL)

Správa účtů je výhradně na straně dodavatele (max. 5× ročně v rámci záruční podpory):

```bash
# Generování BCrypt hash (cost 12) pro heslo 'Heslo2026!'
docker run --rm alpine sh -c \
  "apk add -q apache2-utils && htpasswd -bnBC 12 '' 'Heslo2026!' | tr -d ':\n'"

# Vložení uživatele do DB
psql -h <host> -U flightlog -d flightlog -c "
  INSERT INTO users (email, password_hash, role, active, created_at)
  VALUES ('zapisovatel@letiste.cz', '\$2a\$12\$<hash>', 'WRITER', true, now());
"
```

---

## 12 Řešení běžných problémů

### 12.1 Aplikace se nespustí — chyba DB připojení

```
com.zaxxer.hikari.pool.HikariPool$PoolInitializationException:
  Failed to initialize pool: Connection refused
```

**Příčina:** PostgreSQL není dostupný nebo `DB_URL` je špatně nastaveno.

```bash
# Ověřte dostupnost DB
kubectl exec -it <flightlog-pod> -n flightlog -- \
  sh -c "nc -zv postgres 5432 && echo OK || echo FAIL"

# Zkontrolujte proměnné prostředí
kubectl exec -it <flightlog-pod> -n flightlog -- env | grep DB_
```

### 12.2 Flyway migrace selhala

```
FlywayException: Validate failed: Migration checksum mismatch for migration version 1
```

**Příčina:** Obsah `V1__baseline.sql` byl změněn po aplikaci na DB. Flyway validuje checksum.

```bash
# Zobrazit stav migrací
psql -U flightlog -d flightlog \
  -c "SELECT version, checksum, success FROM flyway_schema_history;"

# Nikdy neměňte existující migrační soubory — vytvořte nový V<N>__.sql
```

### 12.3 ClubDB API nedostupné

**Příznak:** V logu se opakuje `ClubDB unavailable, falling back to long-term cache`.

**Chování systému:** Aplikace přejde do degradovaného režimu, data pilotů jsou načtena z PostgreSQL snapshot. Záznamy letů lze normálně zapisovat.

```bash
# Ruční test dostupnosti ClubDB
curl -v --max-time 15 http://vyuka.profinit.eu:8080/club/user | head -20

# Zjistit stáří posledního snapshotu
psql -U flightlog -d flightlog \
  -c "SELECT max(fetched_at) FROM club_user_snapshots;"
```

### 12.4 Pod v stavu CrashLoopBackOff

```bash
# Zobrazit poslední logy před pádem
kubectl logs <pod-name> -n flightlog --previous

# Zobrazit události
kubectl describe pod <pod-name> -n flightlog | tail -20
```

### 12.5 CSV import selže na validaci

**Příčina:** Nejčastěji špatné kódování (ne UTF-8), špatný oddělovač (čárka místo středník), nebo chybný formát datumu.

**Kontrola:**

```bash
# Ověření kódování souboru
file -i import.csv
# Výstup musí obsahovat: charset=utf-8

# Ověření oddělovače (první 2 řádky)
head -2 import.csv | cat -A
```

### 12.6 Elasticsearch nevytváří indexy

```bash
# Stav clusteru
curl -s http://elasticsearch:9200/_cluster/health | jq .status

# Ověřit, že Logstash přijímá data
kubectl logs deployment/logstash -n flightlog | tail -30

# Vypsat existující indexy
curl -s "http://elasticsearch:9200/_cat/indices/flightlog-*?v"
```

---

*Dokument byl vytvořen na základě analýzy zdrojového kódu, Dockerfile, docker-compose.yml a Kubernetes manifestů projektu flight-log-kotlin.*
