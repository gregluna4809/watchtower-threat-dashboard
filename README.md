# Watchtower

Watchtower is a local advisory network threat dashboard for a single Windows machine. This Phase 1 scaffold contains an empty Spring Boot backend, an empty Vite + React + TypeScript frontend, and a Postgres 16 Docker Compose service.

Watchtower advises only. It does not block, kill, modify, redirect, install, schedule, notify, alert, quarantine, or touch anything outside its own project files and database.

## Prerequisites

- Windows 11
- Docker Desktop
- Java 21
- Maven
- Node.js 20+

## Start Postgres

From the repository root:

```powershell
docker compose up -d
```

Postgres runs in container `watchtower-postgres` on host port `5435`, with database `watchtower`, user `watchtower`, and development password `watchtower_dev`.

## Start the Backend

From `backend/`:

```powershell
mvn spring-boot:run
```

The backend binds to `127.0.0.1:8088`.

Health check:

```powershell
Invoke-RestMethod http://127.0.0.1:8088/actuator/health
```

Expected status:

```json
{"status":"UP"}
```

## Optional GeoIP Data

GeoIP enrichment uses local MaxMind GeoLite2 databases. Watchtower does not download these files.

1. Create a free MaxMind account and download the GeoLite2 City and GeoLite2 ASN `.mmdb` files:
   `https://dev.maxmind.com/geoip/geolite2-free-geolocation-data`
2. Place the files here:

```text
%USERPROFILE%\.watchtower\geoip\GeoLite2-City.mmdb
%USERPROFILE%\.watchtower\geoip\GeoLite2-ASN.mmdb
```

If either file is missing, Watchtower logs a warning and continues with GeoIP disabled.

## Start the Frontend

From `frontend/`:

```powershell
npm install
npm run dev
```

The frontend dev server binds to `127.0.0.1:5173`.

Open:

```text
http://127.0.0.1:5173
```
