# FlightLog — REST API Reference

**Základní URL:** `https://<host>`  
**Content-Type:** `application/json` (není-li uvedeno jinak)  
**Kódování:** UTF-8  
**Formát data:** `DD-MM-YYYY`  
**Formát času:** `HH:MM` (24 h)

---

## Obsah

1. [Autentizace](#1-autentizace)
2. [Zapisovatel — lety](#2-zapisovatel--lety)
3. [Administrátor — lety](#3-administrátor--lety)
4. [Administrátor — export CSV](#4-administrátor--export-csv)
5. [Administrátor — import CSV](#5-administrátor--import-csv)
6. [Číselníkové endpointy](#6-číselníkové-endpointy)
7. [Stránkování a filtrování](#7-stránkování-a-filtrování)
8. [Chybové odpovědi](#8-chybové-odpovědi)
9. [Matice oprávnění](#9-matice-oprávnění)

---

## 1 Autentizace

Všechny API požadavky (kromě endpointů pro přihlášení a obnovu hesla uvedených níže) vyžadují aktivní session. Session cookie `FLSESSION` je nastavena serverem při úspěšném přihlášení a musí být přiložena ke každému dalšímu požadavku (`credentials: 'include'` ve `fetch`, `-b` / `-c` v curl).

Platnost session: **30 minut nečinnosti**. Po vypršení server vrací `401`.

---

### GET /api/auth/me

Vrátí identitu aktuálně přihlášeného uživatele.

**Autentizace:** vyžadována  
**Role:** libovolný přihlášený uživatel

**Odpověď `200 OK`**

```json
{
  "email": "admin@flightlog.local",
  "displayName": "Jan Novák",
  "role": "ADMIN"
}
```

`role` nabývá hodnot: `WRITER`, `ADMIN`.

**Odpověď `401 Unauthorized`** — žádná aktivní session.

---

### POST /api/auth/login

Přihlásí uživatele a vytvoří session.

**Autentizace:** nevyžadována

**Tělo požadavku**

```json
{
  "email": "writer@flightlog.local",
  "password": "writer1234",
  "captchaToken": null
}
```

| Pole | Typ | Povinné | Omezení |
|---|---|:---:|---|
| `email` | string | ano | Platný e-mail, max. 254 znaků |
| `password` | string | ano | 8–128 znaků |
| `captchaToken` | string | ne | Vyžadováno po zablokování účtu (viz níže) |

**Odpověď `200 OK`**

```json
{
  "email": "writer@flightlog.local",
  "displayName": "Anna Kmentová",
  "role": "WRITER"
}
```

Odpověď obsahuje hlavičku `Set-Cookie: FLSESSION=<value>; HttpOnly; SameSite=Lax`.

**Chyby**

| Status | Podmínka |
|---|---|
| `401` | Neplatný e-mail nebo heslo — tělo: `{"message":"Neplatný e-mail nebo heslo."}` |
| `423` | Účet dočasně zablokován po 3 neúspěšných pokusech — tělo: `{"message":"Account locked."}` |
| `403` | Chybí CAPTCHA token po 3 neúspěšných pokusech — tělo: `{"message":"Captcha required."}` |

> Po 3 neúspěšných pokusech za sebou je účet zablokován na **15 minut** a každý další požadavek musí obsahovat `captchaToken`.

---

### POST /api/auth/logout

Zneplatní aktuální session.

**Autentizace:** vyžadována (libovolná role)

**Tělo požadavku:** žádné

**Odpověď `204 No Content`**

---

### POST /api/auth/password/change

Změní heslo aktuálně přihlášeného uživatele.

**Autentizace:** vyžadována (libovolná role)

**Tělo požadavku**

```json
{
  "oldPassword": "writer1234",
  "newPassword": "NewSecurePass99!"
}
```

| Pole | Typ | Povinné | Omezení |
|---|---|:---:|---|
| `oldPassword` | string | ano | Musí odpovídat stávajícímu heslu |
| `newPassword` | string | ano | 8–128 znaků |

**Odpověď `204 No Content`**

**Chyby**

| Status | Podmínka |
|---|---|
| `400` | `oldPassword` neodpovídá stávajícímu heslu |
| `401` | Uživatel není přihlášen |

---

### POST /api/auth/password/reset/request

Vyžádá e-mail pro obnovu hesla. Vždy vrací `202` bez ohledu na to, zda účet existuje (prevence výčtu uživatelů).

**Autentizace:** nevyžadována

**Tělo požadavku**

```json
{
  "email": "writer@flightlog.local"
}
```

**Odpověď `202 Accepted`** — tělo je prázdné. E-mail s jednorázovým tokenem je odeslán, pokud účet existuje.

---

### POST /api/auth/password/reset/confirm

Nastaví nové heslo pomocí tokenu přijatého e-mailem. Tokeny expirují po **24 hodinách** a jsou jednorázové.

**Autentizace:** nevyžadována

**Tělo požadavku**

```json
{
  "token": "eyJhbGc...",
  "newPassword": "FreshPass2026!"
}
```

| Pole | Typ | Povinné | Omezení |
|---|---|:---:|---|
| `token` | string | ano | Jednorázový token z e-mailu |
| `newPassword` | string | ano | 8–128 znaků |

**Odpověď `204 No Content`**

**Chyby**

| Status | Podmínka |
|---|---|
| `400` | Token je neplatný, vypršel nebo byl již použit |

---

## 2 Zapisovatel — lety

Základní cesta: `/api/writer/flights`  
**Vyžadovaná role:** `WRITER` nebo `ADMIN`

---

### POST /api/writer/flights

Zaznamená vzlet nového letu. Pokud je `startType` rovno `TOW`, jsou pole vlečného letadla povinná.

> Zapisovatel může mít nejvýše jeden otevřený (rozletěný) let. Pokus o vytvoření dalšího před přistáním vrátí `409`.

**Tělo požadavku**

```json
{
  "date": "06-05-2026",
  "pilotName": "Novák",
  "gliderImmatriculation": "OK-3456",
  "gliderType": null,
  "task": "Prostor",
  "takeoffTime": "10:30",
  "startType": "TOW",
  "towPilotName": "Houdek",
  "towImmatriculation": "OK-ABC12",
  "towType": null,
  "landingTime": null
}
```

| Pole | Typ | Povinné | Omezení |
|---|---|:---:|---|
| `date` | string | ano | `DD-MM-YYYY` |
| `pilotName` | string | ano | Max. 50 znaků — příjmení vyhledáno v ClubDB |
| `gliderImmatriculation` | string | ano | Max. 20 znaků, velká písmena |
| `gliderType` | string | ne | Max. 100 znaků; pouze pro neznámé hostující letadlo |
| `task` | string | ano | Max. 200 znaků |
| `takeoffTime` | string | ano | `HH:MM` |
| `startType` | enum | ano | `WINCH` (naviák) nebo `TOW` (vlek) |
| `towPilotName` | string | podmíněně | Povinné při `startType = TOW`; max. 50 znaků |
| `towImmatriculation` | string | podmíněně | Povinné při `startType = TOW`; max. 20 znaků |
| `towType` | string | ne | Max. 100 znaků; pouze pro neznámé vlečné letadlo |
| `landingTime` | string | ne | `HH:MM`; pole pouze pro administrátory, u zapisovatelů ignorováno |

**Odpověď `200 OK`**

```json
{
  "flights": [
    {
      "id": 101,
      "date": "06-05-2026",
      "takeoffTime": "10:30",
      "landingTime": null,
      "durationHours": null,
      "pilotName": "Novák",
      "pilotClubMember": true,
      "airplaneImmatriculation": "OK-3456",
      "airplaneType": "L-13 Blaník",
      "category": "GLIDER",
      "task": "Prostor",
      "startType": "TOW",
      "linkedFlightId": 102
    },
    {
      "id": 102,
      "date": "06-05-2026",
      "takeoffTime": "10:30",
      "landingTime": null,
      "durationHours": null,
      "pilotName": "Houdek",
      "pilotClubMember": true,
      "airplaneImmatriculation": "OK-ABC12",
      "airplaneType": "WT9",
      "category": "POWERED",
      "task": "Prostor",
      "startType": "TOW",
      "linkedFlightId": 101
    }
  ],
  "degraded": false,
  "degradedMessage": null
}
```

Pokud je ClubDB nedostupné, odpověď obsahuje:

```json
{
  "flights": [...],
  "degraded": true,
  "degradedMessage": "Systém ClubDB je nedostupný. Data o pilotech mohou být zastaralá."
}
```

**Chyby**

| Status | Podmínka |
|---|---|
| `400` | Chyba validace — viz [Chybové odpovědi](#8-chybové-odpovědi) |
| `409` | Zapisovatel již má otevřený (rozletěný) let |
| `422` | Porušení business pravidla (např. přistání před vzletem) |

---

### POST /api/writer/flights/{id}/land

Zaznamená čas přistání existujícího otevřeného letu.

**Parametry cesty**

| Parametr | Typ | Popis |
|---|---|---|
| `id` | long | ID letu, který přistává |

**Tělo požadavku**

```json
{
  "landingTime": "11:45"
}
```

| Pole | Typ | Povinné | Omezení |
|---|---|:---:|---|
| `landingTime` | string | ano | `HH:MM`; musí být pozdější než `takeoffTime` |

**Odpověď `200 OK`** — vrátí aktualizovaný objekt `FlightResponse` (stejná struktura jako položka v seznamu výše).

**Chyby**

| Status | Podmínka |
|---|---|
| `400` | Neplatný formát času |
| `404` | Let nenalezen |
| `422` | Čas přistání není pozdější než čas vzletu |

---

### GET /api/writer/flights/my-open

Vrátí aktuálně otevřený (rozletěný) let přihlášeného zapisovatele, pokud existuje.

**Odpověď `200 OK`** — objekt `FlightResponse` nebo `null`, pokud žádný otevřený let neexistuje.

```json
{
  "id": 101,
  "date": "06-05-2026",
  "takeoffTime": "10:30",
  "landingTime": null,
  "durationHours": null,
  "pilotName": "Novák",
  "pilotClubMember": true,
  "airplaneImmatriculation": "OK-3456",
  "airplaneType": "L-13 Blaník",
  "category": "GLIDER",
  "task": "Prostor",
  "startType": "TOW",
  "linkedFlightId": 102
}
```

---

## 3 Administrátor — lety

Základní cesta: `/api/admin/flights`  
**Vyžadovaná role:** `ADMIN`

---

### GET /api/admin/flights

Vrátí stránkovaný, filtrovaný seznam letů.

**Query parametry** — všechny volitelné:

| Parametr | Typ | Popis |
|---|---|---|
| `dateFrom` | string | `DD-MM-YYYY` — počáteční datum filtru (včetně) |
| `dateTo` | string | `DD-MM-YYYY` — koncové datum filtru (včetně) |
| `category` | enum | `GLIDER` (kluzák) nebo `POWERED` (motorové) |
| `immatriculation` | string | Částečná nebo přesná shoda (bez ohledu na velikost písmen) |
| `takeoffFrom` | string | `HH:MM` — nejdřívější čas vzletu v daný den |
| `takeoffTo` | string | `HH:MM` — nejpozdější čas vzletu v daný den |
| `durationMin` | double | Minimální doba letu v hodinách |
| `durationMax` | double | Maximální doba letu v hodinách |
| `inAirOnly` | boolean | `true` — pouze lety bez zaznamenaného přistání |
| `landedOnly` | boolean | `true` — pouze dokončené lety |
| `page` | int | Index stránky (číslováno od 0, výchozí: `0`) |
| `size` | int | Počet záznamů na stránku: `10`, `25` nebo `50` (výchozí: `25`) |

**Odpověď `200 OK`**

```json
{
  "content": [
    {
      "id": 101,
      "date": "06-05-2026",
      "takeoffTime": "10:30",
      "landingTime": "11:45",
      "durationHours": 1.25,
      "pilotName": "Novák",
      "pilotClubMember": true,
      "airplaneImmatriculation": "OK-3456",
      "airplaneType": "L-13 Blaník",
      "category": "GLIDER",
      "task": "Prostor",
      "startType": "TOW",
      "linkedFlightId": 102
    }
  ],
  "page": 0,
  "size": 25,
  "totalElements": 1,
  "totalPages": 1
}
```

---

### POST /api/admin/flights

Ručně vytvoří záznam o letu (zpětné doplnění). Přijímá stejné tělo jako `POST /api/writer/flights`, navíc s volitelným polem `landingTime`.

**Odpověď `200 OK`** — `CreateFlightResponse` (stejná struktura jako u endpointu zapisovatele).

---

### PUT /api/admin/flights/{id}

Aktualizuje existující záznam letu.

**Parametry cesty**

| Parametr | Typ | Popis |
|---|---|---|
| `id` | long | ID letu k aktualizaci |

**Tělo požadavku** — stejná struktura jako `POST /api/admin/flights`.

**Odpověď `200 OK`** — aktualizovaný objekt `FlightResponse`.

**Chyby**

| Status | Podmínka |
|---|---|
| `400` | Chyba validace |
| `404` | Let nenalezen |
| `422` | Porušení business pravidla |

---

### DELETE /api/admin/flights/{id}

Smaže záznam letu.

**Parametry cesty**

| Parametr | Typ | Popis |
|---|---|---|
| `id` | long | ID letu ke smazání |

**Query parametry**

| Parametr | Typ | Výchozí | Popis |
|---|---|---|---|
| `cascade` | boolean | `false` | Při `true` smaže také provázaný let (pár vlek–kluzák) |

**Odpověď `204 No Content`**

**Chyby**

| Status | Podmínka |
|---|---|
| `404` | Let nenalezen |

> Smazání je nevratné a je zaznamenáno v auditním logu. Pokud je let provázán s jiným letem (kluzák ↔ vlečné letadlo) a `cascade=false`, vazba je přerušena, ale provázaný záznam zůstane zachován.

---

## 4 Administrátor — export CSV

### GET /api/admin/flights/export

Exportuje lety odpovídající aktuálním filtrům do souboru CSV. Přijímá stejné query parametry jako `GET /api/admin/flights` (bez `page` a `size`).

**Vyžadovaná role:** `ADMIN`

**Odpověď — synchronní (do 1 000 záznamů)**

```
HTTP/1.1 200 OK
Content-Type: text/csv;charset=UTF-8
Content-Disposition: attachment; filename="flightlog_export_06-05-2026.csv"
```

Struktura souboru (kódování UTF-8 s BOM, oddělovač středník):

```
id;datum;typ;imatrikulace;pilot;ukol;start;pristani;doba;pripojeno
101;06-05-2026;L-13 Blaník;OK-3456;Novák;Prostor;10:30;11:45;1.25;
102;06-05-2026;WT9;OK-ABC12;Houdek;Prostor;10:30;10:47;0.28;101
```

**Odpověď — asynchronní (více než 1 000 záznamů)**

```
HTTP/1.1 202 Accepted
Content-Type: application/json
```

```json
{
  "jobId": "a3f9c821-1b2e-4d5f-8c3a-0e7d6f1b4c9d"
}
```

ID jobu použijte k dotazování na stav dokončení (viz níže).

---

### GET /api/admin/flights/exports/{jobId}

Dotáže se na stav asynchronního export jobu.

**Parametry cesty**

| Parametr | Typ | Popis |
|---|---|---|
| `jobId` | string | UUID vrácené asynchronní odpovědí exportu |

**Odpověď — zpracovává se**

```
HTTP/1.1 200 OK
```

```json
{ "status": "pending" }
```

**Odpověď — hotovo**

```
HTTP/1.1 200 OK
Content-Type: text/csv;charset=UTF-8
Content-Disposition: attachment; filename="flightlog_export_06-05-2026.csv"
```

Tělo: bajty CSV souboru.

**Odpověď — selhalo**

```
HTTP/1.1 500 Internal Server Error
```

```json
{ "status": "failed", "error": "Unexpected error during export." }
```

**Chyby**

| Status | Podmínka |
|---|---|
| `404` | Job ID nenalezeno (expirováno nebo nikdy neexistovalo) |

---

## 5 Administrátor — import CSV

Import probíhá ve dvou krocích: **náhled** (validace bez uložení) a **potvrzení** (uložení do databáze).

---

### POST /api/admin/flights/import

Nahraje a zvaliduje CSV soubor bez uložení záznamů. Při úspěchu vrátí potvrzovací token.

**Vyžadovaná role:** `ADMIN`  
**Content-Type:** `multipart/form-data`

**Požadavek**

```
POST /api/admin/flights/import
Content-Type: multipart/form-data; boundary=----FormBoundary

------FormBoundary
Content-Disposition: form-data; name="file"; filename="history.csv"
Content-Type: text/csv

id;datum;typ;imatrikulace;pilot;ukol;start;pristani;doba;pripojeno
1;09-05-2025;WT9;PUS19;Houdek;Prostor;16:57;17:12;0.25;
```

Povinná hlavička CSV (přesné názvy sloupců oddělené středníkem):

```
id;datum;typ;imatrikulace;pilot;ukol;start;pristani;doba;pripojeno
```

**Odpověď `200 OK`** — validace prošla, import je připraven k potvrzení:

```json
{
  "toImport": 12,
  "errors": [],
  "confirmationToken": "X7kPq2mNvLsWbRdT"
}
```

**Odpověď `400 Bad Request`** — validace selhala; `confirmationToken` je `null`:

```json
{
  "toImport": 10,
  "errors": [
    {
      "rowNumber": 3,
      "column": "pristani",
      "message": "Přistání musí být po startu."
    },
    {
      "rowNumber": 7,
      "column": null,
      "message": "Očekáváno 10 sloupců, nalezeno 9."
    }
  ],
  "confirmationToken": null
}
```

Objekt `ImportError`:

| Pole | Typ | Popis |
|---|---|---|
| `rowNumber` | int | Číslo řádku v souboru (číslováno od 1, řádek 1 = hlavička) |
| `column` | string \| null | Název chybného sloupce, nebo `null` pro chyby na úrovni celého řádku |
| `message` | string | Lidsky čitelný popis chyby (česky) |

---

### POST /api/admin/flights/import/confirm

Uloží dříve zvalidovaný import pomocí potvrzovacího tokenu. Token je jednorázový a uchováván v paměti serveru — musí být použit před restartem serveru.

**Tělo požadavku**

```json
{
  "confirmationToken": "X7kPq2mNvLsWbRdT"
}
```

**Odpověď `200 OK`**

```json
{
  "imported": 12
}
```

**Chyby**

| Status | Podmínka |
|---|---|
| `400` | Token je neplatný, byl již použit nebo server byl restartován po náhledu |

---

## 6 Číselníkové endpointy

Tyto endpointy vrací referenční data pro naplnění rozbalovacích nabídek. Vyžadují přihlášení, ale nejsou omezeny na konkrétní roli.

---

### GET /airplane

Vrátí seznam klubových letadel.

**Autentizace:** vyžadována (libovolná role)

**Odpověď `200 OK`**

```json
[
  {
    "id": 5,
    "immatriculation": "OK-3456",
    "type": "L-13 Blaník"
  },
  {
    "id": 8,
    "immatriculation": "OK-ABC12",
    "type": "WT9"
  }
]
```

---

### GET /user

Vrátí seznam členů klubu z ClubDB (pilotů). Při nedostupnosti ClubDB přejde na dlouhodobý PostgreSQL snapshot.

**Autentizace:** vyžadována (libovolná role)

**Response `200 OK`**

```json
[
  {
    "memberId": 42,
    "firstName": "Jan",
    "lastName": "Novák"
  },
  {
    "memberId": 17,
    "firstName": "Pavel",
    "lastName": "Houdek"
  }
]
```

---

## 7 Stránkování a filtrování

### Stránkování

Endpoint `GET /api/admin/flights` čísluje stránky od nuly.

| Parametr | Výchozí | Povolené hodnoty |
|---|---|---|
| `page` | `0` | Libovolné nezáporné celé číslo |
| `size` | `25` | `10`, `25`, `50` |

Stránkované odpovědi vždy obsahují:

```json
{
  "content": [...],
  "page": 0,
  "size": 25,
  "totalElements": 342,
  "totalPages": 14
}
```

### Filtrování

Filtry jsou kombinovány logikou AND — všechny zadané parametry musí být splněny zároveň. Vynechání parametru znamená žádné omezení v dané dimenzi.

`inAirOnly` a `landedOnly` se vzájemně vylučují. Pokud jsou oba nastaveny na `true`, výsledek je prázdný.

**Příklad — všechny lety kluzáků v květnu 2026, strana 2:**

```
GET /api/admin/flights?dateFrom=01-05-2026&dateTo=31-05-2026&category=GLIDER&page=1&size=25
```

**Příklad — lety aktuálně ve vzduchu:**

```
GET /api/admin/flights?inAirOnly=true&size=50
```

**Příklad — export letů kluzáků za konkrétní den:**

```
GET /api/admin/flights/export?dateFrom=06-05-2026&dateTo=06-05-2026&category=GLIDER
```

---

## 8 Chybové odpovědi

Všechny chybové odpovědi mají jednotnou JSON strukturu:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Čas přistání musí být pozdější než čas startu.",
  "field": "landingTime",
  "timestamp": "2026-05-06T10:32:00Z"
}
```

| Pole | Typ | Popis |
|---|---|---|
| `status` | int | HTTP stavový kód |
| `error` | string | Textový popis HTTP stavu |
| `message` | string | Lidsky čitelný popis (doménové chyby v češtině) |
| `field` | string \| null | Název pole formuláře, které chybu způsobilo (je-li relevantní) |
| `timestamp` | string | Časová razítka ve formátu ISO-8601 |

### Přehled HTTP stavových kódů

| Kód | Význam | Kdy se vrací |
|---|---|---|
| `200 OK` | Úspěch | GET, PUT, POST vracející data |
| `202 Accepted` | Asynchronní operace zahájena | Velký CSV export, odeslání e-mailu pro reset hesla |
| `204 No Content` | Úspěch, žádné tělo | DELETE, odhlášení, změna hesla |
| `400 Bad Request` | Chyba vstupní validace | Chybějící povinné pole, špatný formát, neplatný CSV soubor |
| `401 Unauthorized` | Žádná platná session | Přístup k chráněnému endpointu bez přihlášení nebo po vypršení session |
| `403 Forbidden` | Nedostatečná role | WRITER volá endpoint pouze pro ADMIN |
| `404 Not Found` | Zdroj nenalezen | Neznámé ID letu, neznámé ID export jobu |
| `409 Conflict` | Konflikt business pravidla | Vytvoření nového letu, když jeden je stále ve vzduchu |
| `422 Unprocessable Entity` | Porušení business pravidla | Přistání před vzletem, pilot bez role PILOT |
| `423 Locked` | Účet dočasně zablokován | Příliš mnoho neúspěšných pokusů o přihlášení |
| `500 Internal Server Error` | Neočekávaná chyba serveru | Zalogováno; klientovi není odhalen stack trace |

### Chyby validace (400)

Pokud selže validace více polí, odpověď všechna vyjmenuje:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    { "field": "date", "message": "Povinné pole." },
    { "field": "pilotName", "message": "Povinné pole." }
  ],
  "timestamp": "2026-05-06T10:32:00Z"
}
```

---

## 9 Matice oprávnění

| Endpoint | Veřejný | WRITER | ADMIN |
|---|:---:|:---:|:---:|
| `GET /api/auth/me` | | ano | ano |
| `POST /api/auth/login` | ano | | |
| `POST /api/auth/logout` | | ano | ano |
| `POST /api/auth/password/change` | | ano | ano |
| `POST /api/auth/password/reset/request` | ano | | |
| `POST /api/auth/password/reset/confirm` | ano | | |
| `POST /api/writer/flights` | | ano | ano |
| `POST /api/writer/flights/{id}/land` | | ano | ano |
| `GET /api/writer/flights/my-open` | | ano | ano |
| `GET /api/admin/flights` | | | ano |
| `POST /api/admin/flights` | | | ano |
| `PUT /api/admin/flights/{id}` | | | ano |
| `DELETE /api/admin/flights/{id}` | | | ano |
| `GET /api/admin/flights/export` | | | ano |
| `GET /api/admin/flights/exports/{jobId}` | | | ano |
| `POST /api/admin/flights/import` | | | ano |
| `POST /api/admin/flights/import/confirm` | | | ano |
| `GET /airplane` | | ano | ano |
| `GET /user` | | ano | ano |
| `GET /actuator/health` | ano | | |
| `GET /actuator/**` (ostatní) | | | ano |

> Oprávnění jsou vynucována výhradně na backendu. Kontroly rolí na frontendu jsou pouze kosmetické.

---

## Příloha A — Ukázky požadavků (curl)

### Přihlášení

```bash
curl -c cookies.txt -X POST https://flightlog.example.com/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"writer@flightlog.local","password":"writer1234"}'
```

### Záznam vzletu naviákem

```bash
curl -b cookies.txt -X POST https://flightlog.example.com/api/writer/flights \
  -H 'Content-Type: application/json' \
  -d '{
    "date": "06-05-2026",
    "pilotName": "Novák",
    "gliderImmatriculation": "OK-3456",
    "task": "Výcvik",
    "takeoffTime": "09:15",
    "startType": "WINCH"
  }'
```

### Záznam přistání

```bash
curl -b cookies.txt -X POST https://flightlog.example.com/api/writer/flights/101/land \
  -H 'Content-Type: application/json' \
  -d '{"landingTime":"10:05"}'
```

### Výpis letů za dnešní den (admin)

```bash
curl -b cookies.txt \
  "https://flightlog.example.com/api/admin/flights?dateFrom=06-05-2026&dateTo=06-05-2026&size=50"
```

### Export filtrovaného CSV do souboru

```bash
curl -b cookies.txt -OJ \
  "https://flightlog.example.com/api/admin/flights/export?dateFrom=01-05-2026&dateTo=31-05-2026"
```

### Nahrání CSV pro náhled importu

```bash
curl -b cookies.txt -X POST https://flightlog.example.com/api/admin/flights/import \
  -F "file=@history.csv"
```

### Potvrzení importu

```bash
curl -b cookies.txt -X POST https://flightlog.example.com/api/admin/flights/import/confirm \
  -H 'Content-Type: application/json' \
  -d '{"confirmationToken":"X7kPq2mNvLsWbRdT"}'
```

---

## Příloha B — Poznámky k integraci ClubDB

Endpoint `/user` a ověřování pilotů vůči ClubDB fungují v jednom ze dvou režimů:

| Režim | Popis |
|---|---|
| **Normální** | Data načtena živě z `GET http://vyuka.profinit.eu:8080/club/user`; cachována v paměti po dobu 60 s (Caffeine). |
| **Degradovaný** | ClubDB nedostupné po timeoutu 15 s a 2 opakovaných pokusech (za 2 s a 5 s). Systém přejde na nejnovější PostgreSQL snapshot. `FlightResponse.degraded` je `true` a `degradedMessage` je vyplněno. |

Pilot je považován za platného člena klubu s pilotním oprávněním pouze tehdy, pokud jeho pole `roles` v ClubDB obsahuje hodnotu `"PILOT"`. Členové klubu bez této role jsou evidováni jako hosté. Piloti, kteří v ClubDB vůbec neexistují, jsou rovněž evidováni jako hosté.
