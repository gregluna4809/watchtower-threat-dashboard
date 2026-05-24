# Watchtower — Project Specification

> **This document is the single source of truth.** Every phase prompt references this file. Do not deviate from these decisions without an explicit instruction from the user.

---

## 0. The Prime Directive: Watchtower Advises, It Never Acts

**Watchtower is an advisory tool. It observes, scores, and reports. It does not — and must never be able to — take action on the host system or on the network.**

This is the single most important property of the system. It is more important than any feature, any user request, any apparent convenience, and any "obvious" improvement. Every other rule in this document exists to serve this one.

### What "advisory only" means in practice

Watchtower is allowed to:
- **Read** information the OS already exposes to a normal user (running processes, network connections, file paths, code signatures)
- **Read** public threat intelligence over HTTPS
- **Write** to its own Postgres database
- **Write** to its own log files inside the project directory
- **Display** information to the user via its own UI

Watchtower is forbidden to:
- Kill, suspend, or otherwise interfere with any process
- Block, drop, redirect, or modify any network connection
- Add, modify, or remove any firewall rule (Windows Firewall, third-party firewalls, hosts file, routing table, DNS settings)
- Write, modify, or delete any registry key
- Install, modify, or remove any Windows service, scheduled task, driver, or startup entry
- Modify, move, rename, quarantine, or delete any file outside its own project directory and database volume
- Inject code, hook APIs, or load anything into another process's address space
- Make outbound calls to anywhere other than the configured threat intel APIs (AbuseIPDB, optionally OTX)
- Send notifications, emails, SMS, push messages, or any other form of alert beyond rendering data in its own UI
- Auto-run on system startup unless the user manually creates that arrangement themselves (and Watchtower's installer / setup never offers to)

### Why this matters

An advisory tool that gains the power to act becomes a tool that can be tricked, exploited, misconfigured, or buggy in ways that *damage the system it's supposed to protect*. The history of endpoint security is full of products that bricked machines by acting on false positives. By architectural choice, Watchtower cannot do that — not because we trust ourselves to use the power responsibly, but because the power doesn't exist in the codebase.

This also means Watchtower can be wrong, sometimes very wrong, without consequence beyond a misleading number on a dashboard. The user is always the actor. Watchtower hands the user information; the user decides what, if anything, to do about it.

### How this constrains design

- The threat score is a **number on a screen.** It triggers nothing.
- The rules engine emits **reasons**, not actions.
- The ML layer (post-v1) emits **anomaly scores**, not interventions.
- The UI shows **what was observed**, never offers a "block this" or "kill this" button.
- The API has **no mutating endpoints** beyond toggling a rule's enabled flag — and even rule toggles only affect future scores, never the host.
- There is **no agent, no service, no daemon** other than the user-launched Java process.
- There is **no privileged operation** the code path can perform. If a feature would require elevation to do its job, that feature does not get built.

### How to resolve ambiguity

If a future feature, request, or "improvement" could plausibly be interpreted as crossing the advisory/action line, the answer is **no**, and the burden of proof to override is on the requester. The default is inaction.

If a phase prompt, a user message, or your own reasoning suggests adding a capability that touches the system, **stop and ask**. Reference this section by name in the response. The phrase to use: *"This appears to violate the Prime Directive (Spec §0). Confirm before proceeding."*

---

## 1. What Watchtower Is

A **local, advisory** network threat dashboard for a single Windows machine. It polls network connections, enriches them with metadata, scores them against a rules engine, persists the data, and exposes a React dashboard.

Watchtower's relationship to the host is strictly read-only and observational. See §0 for the full statement of this property.

---

## 2. Hard Constraints

These constraints exist because of the deployment environment (personal Windows machine with Windows Defender active). Violating any of them is a failed implementation.

1. **No packet capture libraries.** No Npcap, no WinPcap, no libpcap, no pcap4j, no JNetPcap. These install kernel drivers and trigger Defender.
2. **No raw sockets, no promiscuous mode, no WFP (Windows Filtering Platform) hooks.**
3. **No code that modifies the system.** No registry writes, no firewall rule changes, no service installation, no scheduled task creation, no file writes outside the project directory and the Postgres data volume.
4. **No process termination, no DLL injection, no kernel-mode anything.**
5. **Data collection is restricted to:**
   - `netstat -ano` output (parsed via `ProcessBuilder`)
   - `tasklist /v /fo csv` output (parsed via `ProcessBuilder`)
   - Optional: `Get-AuthenticodeSignature` via PowerShell for signature status
   - Local GeoIP lookups against a downloaded MaxMind GeoLite2 file
   - Outbound HTTPS calls to threat intel APIs (AbuseIPDB, optionally AlienVault OTX) for IPs we've already seen
6. **No admin-required APIs unless explicitly approved by the user.** The app must function (with reduced data) when run as a normal user.

---

## 3. Locked Technical Decisions

| Concern | Decision |
|---|---|
| Project name | `watchtower` |
| Backend language | Java 21 |
| Backend framework | Spring Boot 3.4.5 |
| Build tool | Maven |
| Database | PostgreSQL 16 in Docker |
| Postgres host port | **5435** (5432 default, 5433 RetireSmart, 5434 commonly taken) |
| Postgres container name | `watchtower-postgres` |
| DB name / user / password | `watchtower` / `watchtower` / `watchtower_dev` (dev only; never commit prod secrets) |
| Schema migrations | Flyway |
| ORM | Spring Data JPA / Hibernate |
| Auth | **None for v1.** Localhost-only. Bind backend to `127.0.0.1` only. |
| Frontend tooling | Vite |
| Frontend language | TypeScript (strict mode) |
| Frontend framework | React 18 |
| Routing | React Router v6 |
| Data fetching | TanStack Query v5 |
| Styling | Tailwind CSS + shadcn/ui |
| Charts | Recharts |
| Real-time updates | WebSocket (STOMP over SockJS via Spring) — phase 7+ only |
| Capture method | `netstat -ano` polling every 3 seconds |
| Process enrichment | `tasklist /v /fo csv` |
| GeoIP | MaxMind GeoLite2 City (local `.mmdb` file) |
| Threat intel | AbuseIPDB free tier (1000 lookups/day), aggressively cached |
| OS target | Windows 10 / 11 only for v1 |

---

## 4. Repository Layout

```
watchtower/
├── README.md
├── docker-compose.yml              # Postgres only
├── .gitignore
├── backend/
│   ├── pom.xml
│   ├── src/main/java/com/gluna/watchtower/
│   │   ├── WatchtowerApplication.java
│   │   ├── config/                 # CORS, scheduling, async, web
│   │   ├── capture/                # NetstatPoller, NetstatParser, ConnectionSnapshot
│   │   ├── process/                # TasklistService, ProcessInfo
│   │   ├── enrichment/             # GeoIpService, ThreatIntelService, ReverseDnsService
│   │   ├── scoring/                # RulesEngine, Rule (interface), rules/*.java
│   │   ├── model/                  # JPA entities
│   │   ├── repo/                   # Spring Data repositories
│   │   ├── service/                # Application services (orchestration)
│   │   ├── api/                    # REST controllers, DTOs
│   │   └── ws/                     # WebSocket config + handlers (phase 7+)
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-dev.yml
│       └── db/migration/           # V1__init.sql, V2__..., etc.
└── frontend/
    ├── package.json
    ├── vite.config.ts
    ├── tsconfig.json
    ├── tailwind.config.js
    ├── index.html
    └── src/
        ├── main.tsx
        ├── App.tsx
        ├── lib/                    # apiClient.ts, types.ts, utils.ts
        ├── hooks/                  # useConnections, useProcesses, useScores
        ├── components/             # ui/ (shadcn), domain components
        └── pages/                  # Dashboard, Connections, Processes, Rules
```

---

## 5. Data Model

Six tables. All timestamps are `TIMESTAMPTZ`. All IDs are `BIGSERIAL` unless noted.

### `processes`
| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL PK | |
| pid | INTEGER | Not unique — PIDs are reused |
| name | TEXT | e.g. `chrome.exe` |
| path | TEXT NULL | Full executable path if resolvable |
| signed | BOOLEAN NULL | NULL = not yet checked |
| signer | TEXT NULL | e.g. `Microsoft Corporation` |
| first_seen | TIMESTAMPTZ NOT NULL | |
| last_seen | TIMESTAMPTZ NOT NULL | |

Index: `(pid, name, first_seen)` for dedupe lookups.

### `remote_endpoints`
| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL PK | |
| ip | INET UNIQUE NOT NULL | |
| asn | INTEGER NULL | |
| asn_org | TEXT NULL | |
| country_iso | CHAR(2) NULL | |
| country_name | TEXT NULL | |
| city | TEXT NULL | |
| reverse_dns | TEXT NULL | |
| first_seen | TIMESTAMPTZ NOT NULL | |
| last_seen | TIMESTAMPTZ NOT NULL | |

### `connections`
| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL PK | |
| process_id | BIGINT FK -> processes(id) NULL | NULL when PID can't be resolved |
| endpoint_id | BIGINT FK -> remote_endpoints(id) NULL | NULL for LISTENING rows |
| local_ip | INET NOT NULL | |
| local_port | INTEGER NOT NULL | |
| remote_port | INTEGER NULL | |
| protocol | VARCHAR(8) NOT NULL | `TCP` / `UDP` |
| state | VARCHAR(16) NULL | `ESTABLISHED`, `LISTENING`, etc. |
| first_seen | TIMESTAMPTZ NOT NULL | |
| last_seen | TIMESTAMPTZ NOT NULL | |
| observation_count | INTEGER NOT NULL DEFAULT 1 | Incremented per poll the row is still present |

Unique constraint: `(process_id, local_port, endpoint_id, remote_port, protocol)` — this is how we dedupe across polls.

### `threat_scores`
| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL PK | |
| target_type | VARCHAR(16) NOT NULL | `ENDPOINT` or `PROCESS` or `CONNECTION` |
| target_id | BIGINT NOT NULL | |
| score | INTEGER NOT NULL | 0–100 |
| reasons | JSONB NOT NULL | `[{ "rule": "...", "points": 25, "detail": "..." }]` |
| computed_at | TIMESTAMPTZ NOT NULL | |

Index: `(target_type, target_id, computed_at DESC)`.

### `intel_cache`
| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL PK | |
| ip | INET NOT NULL | |
| source | VARCHAR(32) NOT NULL | `ABUSEIPDB`, `OTX`, `GEOIP` |
| payload | JSONB NOT NULL | Raw response from the provider |
| fetched_at | TIMESTAMPTZ NOT NULL | |
| ttl_seconds | INTEGER NOT NULL | |

Unique constraint: `(ip, source)`.

### `rule_definitions`
| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL PK | |
| code | VARCHAR(64) UNIQUE NOT NULL | e.g. `UNSIGNED_OUTBOUND` |
| display_name | TEXT NOT NULL | |
| description | TEXT NOT NULL | |
| default_points | INTEGER NOT NULL | |
| enabled | BOOLEAN NOT NULL DEFAULT true | |

Seeded by Flyway on first run.

---

## 6. Scoring Model

- Each rule returns 0 or more points with a reason string.
- Total score per target is the **sum** of all rule points, **clamped to 100**.
- Score is recomputed whenever a connection's enrichment data changes or a poll observes the connection again.
- Scores are written as new rows (append-only) so we have a history; the UI shows the latest per `(target_type, target_id)`.

### V1 Rules (implement in this order)

| Code | Points | Trigger |
|---|---|---|
| `UNSIGNED_OUTBOUND` | 20 | Process executable is unsigned AND has an established outbound connection |
| `TEMP_PATH_EXEC` | 30 | Process path is under `%TEMP%`, `%APPDATA%`, `%LOCALAPPDATA%\Temp`, or similar |
| `UNCOMMON_PORT` | 5 | Remote port is not in [80, 443, 53, 22, 25, 110, 143, 465, 587, 993, 995, 3389, 5222, 5223, 8080, 8443] |
| `HIGH_RISK_COUNTRY` | 15 | Remote IP geolocates to a configurable high-risk country list |
| `ABUSEIPDB_FLAG` | scaled 10–40 | `abuseConfidenceScore` from AbuseIPDB; scale: `points = clamp(confidence * 0.4, 0, 40)`. Only fires when `>= 25`. |
| `PRIVATE_RANGE_BROWSER` | 10 | Process is a known browser and remote IP is in RFC1918 private range (excluding the local subnet) |
| `BEACON_CADENCE` | 20 | Same process→endpoint pair observed at regular intervals (stddev/mean of interpoll deltas < 0.2 across ≥ 5 observations) — implement in phase 9+ |

---

## 7. API Surface (REST)

All routes prefixed `/api/v1`. Backend binds to `127.0.0.1:8088`.

| Method | Path | Description |
|---|---|---|
| GET | `/health` | `{ "status": "UP", "version": "..." }` |
| GET | `/connections` | Latest connection rows; supports `?state=`, `?minScore=`, `?processName=`, `?country=`, `?limit=`, `?offset=` |
| GET | `/connections/{id}` | Single connection with joined process + endpoint + latest score |
| GET | `/processes` | Distinct processes seen, with aggregate connection counts and max score |
| GET | `/processes/{id}` | Single process with its connections |
| GET | `/endpoints` | Distinct remote endpoints |
| GET | `/endpoints/{id}` | Single endpoint with enrichment data |
| GET | `/rules` | Rule definitions and enabled state |
| PATCH | `/rules/{code}` | Toggle `enabled` only — no other field is mutable from the UI in v1 |
| GET | `/stats/summary` | Counts: total connections, distinct processes, distinct endpoints, mean score, top-5 scoring processes |

DTOs are explicit Java records under `api/dto/`. Never expose JPA entities directly.

---

## 8. Non-Negotiable Coding Rules

These exist to keep Claude Code's output consistent and reviewable.

### Backend (Java)
1. Java 21. Use records for DTOs and immutable value objects.
2. Constructor injection only. **No `@Autowired` on fields.**
3. No `Lombok`. Write the boilerplate; it's a teaching project.
4. Every service class has an interface only when there is more than one implementation OR a clear test seam. Otherwise concrete class is fine.
5. All scheduled methods are in a single `@Configuration`-annotated `ScheduleConfig` class or annotated with `@Scheduled` on a `@Service` method whose only job is scheduling. Keep cron expressions and intervals in `application.yml`.
6. Every external command execution (`netstat`, `tasklist`, `powershell`) goes through a single `WindowsCommandRunner` service with a 5-second timeout and proper stream draining. **No inline `Runtime.exec` calls anywhere else.**
7. SQL via Flyway migrations only. **Never** `spring.jpa.hibernate.ddl-auto=update` or `create`. Set it to `validate`.
8. Use `INET` and `JSONB` Postgres types — register Hibernate types or use Hypersistence Utils if needed; do not store IPs as strings.
9. Exceptions: throw a small set of project-defined runtime exceptions (`CaptureException`, `EnrichmentException`, `NotFoundException`). Map them to HTTP via a single `@RestControllerAdvice`.
10. Logging: SLF4J with parameterized messages (`log.info("Polled {} rows", count)`), never string concatenation.
11. **No premature interfaces, no premature abstractions, no premature generics.** YAGNI.

### Frontend (TypeScript / React)
1. TypeScript strict mode. No `any` without an inline comment explaining why.
2. **No `Form` HTML elements in artifacts** — does not apply to this real app, but in this real app prefer `<form onSubmit>` only when actually submitting.
3. Functional components only. Hooks only.
4. TanStack Query for all server state. **No `useEffect` + `fetch` patterns.**
5. One API client module (`lib/apiClient.ts`) using `fetch` with typed wrappers; do not sprinkle `fetch` calls across components.
6. All API response types live in `lib/types.ts` and mirror the backend DTOs exactly.
7. Tailwind utility classes. shadcn/ui components for primitives (Button, Card, Table, Dialog, etc.).
8. **Polished, professional appearance.** No emoji-driven UI, no playful copy, no rainbow gradients. Dark mode by default with a serious security-tool aesthetic: slate/zinc neutrals, a single accent color (default: amber for warnings, red for critical, green for clean), monospaced font for IPs and ports.
9. Numbers, IPs, ports, and timestamps rendered with `font-mono` and right-aligned in tables.
10. Loading states are skeletons, not spinners. Errors are inline alerts, not toasts (toasts are for ephemeral confirmations only).

### Both
1. **No code outside the agreed scope of the current phase.** If a phase says "implement netstat polling and persist snapshots," do not also add a WebSocket endpoint, do not also add an unrelated rule, do not also refactor an unrelated file.
2. **Complete files only.** Every file produced is the full, runnable file — never a diff, never a snippet, never "...(rest unchanged)".
3. **No invented dependencies.** Only add a library to `pom.xml` or `package.json` when its use is required by the current phase, and call it out explicitly in the response.
4. **No code that contacts the network at startup.** Threat intel calls happen on demand, after a connection is observed.

---

## 9. How Claude Code Should Behave on This Project

When starting any phase:
1. Read this entire spec doc first. Confirm at the top of the response that the relevant constraints are understood.
2. State the phase's exact scope in one sentence before writing code.
3. List the files that will be created or modified.
4. Generate the files in full.
5. End with: (a) the exact commands to run to verify the phase works, (b) the expected output, (c) a one-line summary of what was delivered.

When asked to do something outside the current phase, refuse and reference the spec.

When the user says "go off script," ask which constraint they want relaxed and why before proceeding.

---

## 10. Out of Scope for v1 (do not implement)

- Multi-host / agent + server architecture
- Linux or macOS support
- Authentication, user accounts, multi-tenancy
- Cloud deployment
- Automated response actions (blocking, killing processes, firewall changes) — **forbidden, not just out of scope**
- Email / SMS alerting
- Encrypted at-rest storage of the database beyond Postgres defaults
- Mobile UI

---

End of spec.
