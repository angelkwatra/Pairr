# Pairr

A skill-based partner matching platform that helps students, professionals, and hobbyists discover and collaborate with like-minded people based on skills, proficiency levels, ratings, and availability.

> **Live Demo:** Backend on [Railway](https://railway.app) · Frontend on [Vercel](https://pairr.vercel.app)

---

## ✨ Features

- **Smart Recommendations** — Weighted scoring engine (availability overlap 45%, proficiency 20%, ratings 25%, experience 10%) ranks the best collaborators for you
- **Real-Time Chat** — WebSocket (STOMP) powered 1:1 messaging with REST fallback for history
- **Pairing Sessions** — Formal request → accept → complete lifecycle for verified collaborations
- **Verified Ratings** — Rate partners on specific skills (1–5); scores feed back into recommendations
- **Progressive Onboarding** — Guided setup: select skills → set availability → start discovering
- **Google OAuth** — Sign in with Google alongside traditional email/password + JWT auth
- **Admin Dashboard** — Manage skill categories, users, and platform data

---

## 🏗️ Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Java 17 · Spring Boot 3.5 · Spring Security · Spring WebSocket |
| **Frontend** | React 19 · Vite 7 · Tailwind CSS v4 · React Router v7 |
| **Database** | PostgreSQL 16 · Liquibase migrations |
| **Auth** | JWT (stateless) · Google OAuth 2.0 · Role-based (USER / ADMIN) |
| **Real-Time** | STOMP over WebSocket · @stomp/stompjs |
| **Caching** | Caffeine |
| **API Docs** | Swagger UI (SpringDoc OpenAPI) |
| **CI/CD** | GitHub Actions · Railway (backend) · Vercel (frontend) |

---

## 📂 Project Structure

```
Pairr/
├── backend/          # Spring Boot API (Java 17, Maven)
│   ├── src/
│   ├── docker-compose.yml   # Local PostgreSQL
│   ├── pom.xml
│   └── mvnw
├── frontend/         # React SPA (Vite)
│   ├── src/
│   ├── package.json
│   └── vite.config.js
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 17+**
- **Node.js 18+**
- **Docker & Docker Compose**

### 1. Start the Database

```bash
cd backend
docker-compose up -d
```

### 2. Run the Backend

```bash
cd backend
./mvnw spring-boot:run
```

The API starts on **http://localhost:8080**.  
Swagger docs available at **http://localhost:8080/swagger-ui.html**.

A default admin account is created on first startup:
- **Email:** `admin@pairr.com`
- **Password:** `admin123`

### 3. Run the Frontend

```bash
cd frontend
npm install
npm run dev
```

The app runs on **http://localhost:5173**.

### Environment Variables

#### Backend

| Variable | Default | Purpose |
|---|---|---|
| `JWT_SECRET` | Dev key | JWT signing key (change in production) |
| `ADMIN_EMAIL` | `admin@pairr.com` | Default admin email |
| `ADMIN_PASSWORD` | `admin123` | Default admin password |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Allowed frontend origin(s) |
| `GOOGLE_CLIENT_ID` | — | Google OAuth Client ID |
| `GOOGLE_CLIENT_SECRET` | — | Google OAuth Client Secret |

#### Frontend

| Variable | Description |
|---|---|
| `VITE_API_URL` | Backend API URL (leave empty in dev for Vite proxy) |

---

## 🔧 How It Works

### Recommendation Engine

The engine uses **rule-based weighted scoring** to rank potential partners:

| Factor | Weight | Method |
|---|---|---|
| Availability overlap | 45% | Sweep-line algorithm on time windows |
| Proficiency similarity | 20% | Distance between skill levels (Beginner → Expert) |
| Skill rating | 15% | Peer-rated score comparison |
| Overall user rating | 10% | Aggregate rating comparison |
| Completed sessions | 10% | Experience bonus from verified collaborations |

### Real-Time Chat

- **WebSocket** connection with JWT auth via `ws://localhost:8080/ws?token=<JWT>`
- STOMP protocol for pub/sub messaging (`/user/queue/messages`)
- REST endpoints for conversation history and message sending
- Optimistic UI updates with sending states

### Pairing Sessions

A formal collaboration lifecycle:
**Request** → **Accept** → **Complete** (or Cancel)

Chat is locked to users with an accepted pairing session, ensuring verified interactions.

---

## 🧪 Testing

```bash
# Run all tests
cd backend
./mvnw test

# Run a specific test class
./mvnw test -Dtest=RecommendationServiceTest
```

The backend has **59 unit tests** covering all critical flows — recommendation scoring, ratings, chat, auth, and time matching — using Mockito with no Spring context required (~0.5s total runtime).

---

## 🚢 Deployment

| Component | Platform |
|---|---|
| Backend | [Railway](https://railway.app) with PostgreSQL service |
| Frontend | [Vercel](https://vercel.app) |

Railway activates the `prod` profile via `SPRING_PROFILES_ACTIVE=prod`, which disables Swagger UI, restricts actuator to `/actuator/health`, and connects to Railway's managed PostgreSQL.

**CI/CD:** GitHub Actions runs `./mvnw clean test` on every push to `main` and on pull requests. Railway auto-deploys on git push.

---

## 📋 API Overview

<details>
<summary><strong>Public Endpoints</strong></summary>

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register (returns JWT) |
| POST | `/api/auth/login` | Login (returns JWT) |

</details>

<details>
<summary><strong>Authenticated Endpoints</strong> (Bearer token required)</summary>

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/user/me` | Current user profile |
| GET | `/api/users/{id}` | Public profile |
| POST | `/api/user/skills` | Set skills (full replace) |
| POST | `/api/user/availability` | Set availability (full replace) |
| GET | `/api/recommendations` | Get partner recommendations |
| POST | `/api/ratings` | Rate a user on a skill |
| POST | `/api/chat/messages` | Send a chat message |
| GET | `/api/chat/conversations` | List conversations |
| POST | `/api/pairing/request` | Request a pairing session |
| PATCH | `/api/pairing/{id}/status` | Accept / Complete / Cancel |

</details>

<details>
<summary><strong>Admin Endpoints</strong> (ADMIN role required)</summary>

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/admin/categories` | Create a skill category |
| POST | `/api/admin/skills` | Create a skill |
| GET | `/api/admin/users` | List all users |
| GET | `/api/admin/dashboard` | Admin dashboard |

</details>

<details>
<summary><strong>WebSocket</strong></summary>

| Action | Destination | Description |
|---|---|---|
| Connect | `ws://localhost:8080/ws?token=<JWT>` | Open connection |
| Send | `/app/chat.send` | Send a message |
| Subscribe | `/user/queue/messages` | Receive messages |

</details>

---

## 📌 Roadmap

- [ ] Timezone support
- [ ] Meeting link sharing (Google Meet / Zoom)
- [ ] Group chats & video calls
- [ ] Redis for online presence
- [ ] ElasticSearch for skill search
- [ ] AI/ML-powered recommendations
- [ ] Notifications & payments

---