# HealthConnect — Healthcare & Yoga Video Support Platform

A full-stack video support application where **patients/learners** start instant or scheduled
video calls, **support specialists** answer them from a live queue, and **admins** configure
video providers and watch analytics. Built with **Java Spring Boot + PostgreSQL + RabbitMQ**
on the backend, **open-source WebRTC** for peer-to-peer video (no paid SDK), a **React** frontend,
and an **optional Gemini AI** assistant.

> **What this is:** a genuinely runnable, end-to-end **foundation** that covers the core flows of
> every user story in the requirements. It is structured so the harder production concerns
> (full HIPAA controls, recording storage, 2FA, provider failover at runtime) can be layered on.
> The "Story coverage" table below is honest about what is wired end-to-end vs. scaffolded.

---

## Architecture

```
                 ┌──────────────┐      WebRTC media (P2P)      ┌──────────────┐
                 │   Patient    │◄────────────────────────────►│   Support    │
                 │  (browser)   │                              │  (browser)   │
                 └──────┬───────┘                              └──────┬───────┘
                        │  REST + WebSocket(STOMP/SockJS) signaling   │
                        ▼                                             ▼
                 ┌─────────────────────────────────────────────────────────┐
                 │                React frontend (nginx)                    │
                 │   /api  → backend REST     /ws → backend WebSocket       │
                 └───────────────────────────┬─────────────────────────────┘
                                             │
                 ┌───────────────────────────▼─────────────────────────────┐
                 │             Spring Boot backend (port 8080)              │
                 │  auth · users · calls · scheduling · providers · ai      │
                 │  notifications · analytics · WebRTC signaling relay      │
                 └───────┬──────────────────────────┬──────────────────────┘
                         │                          │
                 ┌───────▼────────┐         ┌───────▼────────┐      ┌──────────────┐
                 │   PostgreSQL   │         │    RabbitMQ    │      │  Gemini API  │
                 │  (JPA/Hibernate)│        │ event bus →    │      │  (optional)  │
                 │                │         │ notifications  │      │              │
                 └────────────────┘         └────────────────┘      └──────────────┘
```

**Why a modular monolith instead of separate microservice repos?** The backend is organized into
bounded-context packages (`auth`, `user`, `call`, `schedule`, `provider`, `notification`, `ai`)
that communicate through an event bus (RabbitMQ). This keeps it runnable and reviewable in one pass
while preserving clean seams: each package could be extracted into its own service later with
minimal change, because cross-context communication already goes through events.

**Video is genuinely open-source / peer-to-peer.** The browser uses the native `RTCPeerConnection`
WebRTC API with public Google STUN. The backend only **relays signaling** (offer/answer/ICE) over
WebSocket — it never sees the media stream. No Twilio/Agora/Zoom SDK is required for the internal
provider. The admin "providers" feature lets you register Zoom/Teams as alternative backends.

---

## Tech stack

| Layer        | Technology |
|--------------|-----------|
| Backend      | Java 17, Spring Boot 3.2, Spring Security (JWT), Spring Data JPA / Hibernate, Spring WebSocket (STOMP), Spring AMQP |
| Database     | PostgreSQL 16 |
| Messaging    | RabbitMQ 3.13 (topic exchange → notification consumer) |
| Video        | WebRTC (browser-native, P2P) + STUN; signaling relayed via STOMP/SockJS |
| AI           | Google Gemini REST (optional; graceful fallback when no key) |
| Frontend     | React 18, Vite, React Router, @stomp/stompjs, SockJS |
| Packaging    | Docker + Docker Compose (Postgres, RabbitMQ, backend, frontend) |

---

## Run it

### Option A — Docker Compose (recommended, one command)

Requires Docker Desktop (or Docker Engine + Compose v2).

```bash
cd healthcare-video
docker compose up --build
```

Then open:

| Service            | URL |
|--------------------|-----|
| **Web app**        | http://localhost:8088 |
| Backend API        | http://localhost:8080 |
| RabbitMQ dashboard | http://localhost:15672  (guest / guest) |

The database is seeded automatically on first start with demo users and providers.

> **Camera/mic note:** browsers only allow `getUserMedia` on `localhost` or HTTPS. `localhost:8088`
> works out of the box. If you serve it on a LAN IP, put it behind HTTPS.

To enable the AI assistant, set a key before starting:

```bash
cp .env.example .env
# edit .env and set GEMINI_API_KEY=...
docker compose up --build
```

### Option B — run backend & frontend separately (dev mode)

You still need Postgres and RabbitMQ running (the compose file can provide just those:
`docker compose up postgres rabbitmq`).

```bash
# backend
cd backend
./mvnw spring-boot:run        # or: mvn spring-boot:run

# frontend (new terminal)
cd frontend
npm install
npm run dev                    # Vite dev server on http://localhost:5173 (proxies /api and /ws)
```

---

## Demo logins

| Role     | Email                  | Password    | Notes |
|----------|------------------------|-------------|-------|
| Admin    | admin@health.dev       | admin123    | Providers + analytics |
| Support  | dr.smith@health.dev    | support123  | General Medicine, senior (gets escalations) |
| Support  | yoga.ravi@health.dev   | support123  | Yoga specialist |
| Patient  | patient@health.dev     | patient123  | Start/schedule calls |

**Try the full flow:** log in as the patient in one browser and as `dr.smith` in another
(use a private/incognito window for the second). Patient clicks **Start Video Support** →
support sees it in **Queue** → **Accept & join** → both land in the same WebRTC room with
chat, screen-share, notes and AI assist.

---

## Story coverage

| # | User story | Status | Where |
|---|-----------|--------|-------|
| 1 | Patient starts instant / scheduled video support call | ✅ End-to-end | Dashboard → CallRoom; Schedule |
| 2 | Support receives, accepts/rejects, queue, notes, availability | ✅ End-to-end | SupportQueue; availability toggle; in-call Notes |
| 3 | Admin configures providers, default, test connection, API keys | ✅ End-to-end (test simulated) | AdminProviders |
| 4 | Schedule call with slots, reminders, timezone | ✅ End-to-end | Schedule; `@Scheduled` reminder sweep → 24h/1h events |
| 5 | Call quality controls: mute, camera, screen-share, chat, recording consent | ✅ Mostly | CallRoom controls + consent flag captured |
| 6 | Emergency escalation to senior staff | ✅ Backend wired | `POST /api/calls/{id}/escalate` → notifies senior staff |
| 7 | Admin analytics: calls, duration, ratings | ✅ End-to-end | Dashboard (admin) → `/api/analytics/overview` |
| 8 | Secure auth, encryption, audit, waiting room, HIPAA/GDPR | ◑ Partial | JWT + BCrypt + role guards + consent; see extension points |
| 9 | Multi-language + translation | ◑ Partial | Language on profile + support filtering + Gemini translate endpoint |

✅ = working end-to-end &nbsp; ◑ = core present, hardening needed

---

## Honest extension points (what a production build still needs)

These are deliberately left as clean seams rather than half-built:

- **Recording storage** — consent is captured per call; actual media recording + encrypted
  storage (e.g. S3 with server-side encryption) is not implemented (media is P2P).
- **2FA** — auth is JWT + BCrypt; TOTP/SMS second factor is not yet added.
- **End-to-end & at-rest encryption** — WebRTC media is encrypted in transit (DTLS-SRTP) by the
  browser; database-column encryption and full E2E key management are not implemented.
- **Audit logging** — domain events flow through RabbitMQ already; a dedicated immutable audit
  log/table is a natural next consumer.
- **Provider failover at runtime** — providers carry `priority` and enable/default flags; the
  client currently uses the internal WebRTC path. Runtime selection/failover across Zoom/Teams
  would consume the provider config.
- **TURN server** — only STUN is configured; add TURN for users behind symmetric NATs.
- **Waiting room** — the support queue acts as a basic waiting room; a per-patient "admit" gate
  could be added.
- **Analytics depth** — overview metrics are live; charts/time-series would build on the same data.

---

## Project layout

```
healthcare-video/
├── docker-compose.yml
├── .env.example
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/healthconnect/
│       ├── auth/ user/ call/ schedule/ provider/ notification/ ai/
│       ├── config/   (security, JWT, RabbitMQ, WebSocket, seeding)
│       └── common/   (roles, statuses, error handling)
└── frontend/
    ├── Dockerfile  nginx.conf
    └── src/
        ├── lib/      (api client, auth context, websocket)
        ├── pages/    (Login, Register, Dashboard, CallRoom, Schedule, History, SupportQueue, AdminProviders)
        └── components/ (NotificationBell)
```

---

## Key API endpoints (quick reference)

```
POST /api/auth/register | /login
GET/PUT /api/users/me ·  PATCH /api/users/me/availability ·  GET /api/users/support
POST /api/calls/instant ·  GET /api/calls/queue ·  POST /api/calls/{id}/accept|reject|active|end|escalate|rate|notes
GET  /api/calls/history ·  GET /api/calls/{id} ·  GET /api/calls/room/{roomId}
POST /api/schedule ·  GET /api/schedule ·  POST /api/schedule/{id}/cancel|reschedule
GET  /api/providers/active ·  (admin) GET/POST /api/providers ·  PUT/DELETE /api/providers/{id} ·  POST {id}/default|toggle|test
GET  /api/analytics/overview   (admin)
GET  /api/ai/status ·  POST /api/ai/assist|summary/{callId}|translate
GET  /api/notifications ·  /unread-count ·  POST /api/notifications/{id}/read

WebSocket (SockJS /ws, STOMP):
  send  /app/signal/{roomId}   subscribe /topic/room/{roomId}     (WebRTC offer/answer/ICE)
  send  /app/chat/{roomId}     subscribe /topic/chat/{roomId}     (in-call chat)
  subscribe /topic/notifications/{userId}                         (live notifications)
```
