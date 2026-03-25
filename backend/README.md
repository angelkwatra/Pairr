# Pairr

A skill-based partner matching API. Discover and collaborate with like-minded people based on skills, proficiency levels, ratings, and availability.

**Target users:** Students, professionals, hobbyists, mentors, and learners.

## Core User Flows

1. **Register & set up profile** — sign up with email, declare skills with proficiency levels, set availability windows (weekday/weekend)
2. **Discover partners** — search by skill, proficiency range, and availability overlap; system returns ranked recommendations
3. **Communicate** — start a 1:1 chat (REST or real-time WebSocket) with stored message history
4. **Rate & feedback** — rate users on a skill (1-5 + optional text); ratings aggregate into per-skill and overall scores, feeding back into recommendation ranking

## Tech Stack

| Component | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.5.10 |
| Database | PostgreSQL 16 |
| Auth | JWT (stateless, role-based) |
| Real-time | WebSocket + STOMP |
| Migrations | Liquibase |
| Caching | Caffeine |
| Build | Maven (wrapper included) |
| Containers | Docker Compose (PostgreSQL) |

## Getting Started

### Prerequisites

- Java 17+
- Docker & Docker Compose

### Run

```bash
# Start PostgreSQL
docker-compose up -d

# Run the application
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`. A default admin account is created on first startup:
- **Email:** `admin@pairr.com`
- **Password:** `admin123`

API docs are available at **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)** — use the "Authorize" button to paste a JWT and test authenticated endpoints interactively.

### Build & Test

```bash
./mvnw clean package              # Build
./mvnw test                       # Run all tests
./mvnw test -Dtest=ClassName      # Run a single test class
./mvnw test -Dtest=ClassName#methodName  # Run a single test method
```

### Unit Tests

The project includes unit tests for all critical service flows using Mockito (`@ExtendWith(MockitoExtension.class)`) — no Spring context or database needed, so they run fast (~0.5s total).

| Test Class | Tests | What It Covers |
|---|---|---|
| `TimeMatcherTest` | 9 | Sweep-line overlap/distance algorithm: full, partial, no overlap, multiple slots, null/empty, unsorted input |
| `ScoreCalculatorTest` | 18 | Proficiency/skill/user rating scoring, time score (overlap cap, inverse distance), final score weights and 2-decimal rounding |
| `RatingServiceTest` | 10 | Submit rating with recalculation, all 6 validation guards (self-rating, not found, duplicate), query methods |
| `ChatServiceTest` | 10 | Message sending, conversation creation/reuse, UUID ordering, self-message guard, participant authorization |
| `AuthServiceTest` | 5 | Register (happy path + duplicate email), login (happy path + wrong email + wrong password) |
| `UserSkillServiceTest` | 4 | Bulk skill add, user not found, duplicate skill, skill not in DB |
| `RecommendationServiceTest` | 3 | Happy path, missing availability, missing skill preconditions |

Test files live under `src/test/java/com/connect/pairr/` mirroring the main source structure (`core/recommendation/`, `service/`, `auth/`).

## API Overview

### Public

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a new user (returns JWT) |
| POST | `/api/auth/login` | Login (returns JWT) |

### Authenticated (Bearer token required)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/user/me` | Get current user profile (ID, email, etc.) |
| GET | `/api/users/{id}` | Get a user's public profile |
| GET | `/api/users/{id}/skills` | Get a user's skills |
| GET | `/api/users/{id}/availability` | Get a user's availability |
| GET | `/api/categories` | List all skill categories |
| GET | `/api/skills` | List all skills |
| POST | `/api/user/skills` | Add skills to your profile (full replace) |
| GET | `/api/user/skills` | Get your skills |
| POST | `/api/user/availability` | Set availability windows (full replace) |
| GET | `/api/user/availability` | Get your availability |
| GET | `/api/recommendations?skillId=&dayType=&numberOfRecommendations=` | Get partner recommendations |
| POST | `/api/ratings` | Rate a user on a skill (1-5 + optional feedback) |
| GET | `/api/ratings?userId=` | Get all ratings for a user |
| GET | `/api/ratings?userId=&skillId=` | Get ratings for a user on a specific skill |
| POST | `/api/chat/messages` | Send a chat message |
| GET | `/api/chat/conversations` | List your conversations (paginated, with unread count) |
| GET | `/api/chat/conversations/{id}/messages` | Get message history (paginated, auto-marks as read) |
| POST | `/api/chat/conversations/{id}/read` | Mark conversation as read |

### Pairing Sessions (Collaboration Lifecycle)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/pairing/request` | Request a pairing session with another user |
| PATCH | `/api/pairing/{id}/status?status=` | Accept, Complete, or Cancel a session |
| GET | `/api/pairing/sessions` | List your pairing sessions (paginated) |

### WebSocket (STOMP over WebSocket)

| Action | Destination | Description |
|---|---|---|
| Connect | `ws://localhost:8080/ws?token=<JWT>` | Open WebSocket connection with JWT auth |
| Send | `/app/chat.send` | Send a message (payload: `{"recipientId":"...","content":"..."}`) |
| Subscribe | `/user/queue/messages` | Receive incoming messages in real time |

### Admin (ADMIN role required)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/admin/categories` | Create a skill category |
| POST | `/api/admin/skills` | Create a skill under a category |
| GET | `/api/admin/users` | List all users |
| GET | `/api/admin/dashboard` | Admin dashboard |

## How It Works

### Authentication Flow

1. Client calls `POST /api/auth/register` (or `/login`) with email + password
2. `AuthService` validates credentials, hashes the password (BCrypt for registration), and generates a JWT via `JwtService`
3. The JWT contains the user's UUID (subject), email, and role; expires after 24 hours
4. For all subsequent requests, the client sends `Authorization: Bearer <token>`
5. `JwtAuthenticationFilter` intercepts each HTTP request, validates the JWT signature and expiry, checks the user still exists (cached via Caffeine, 60min TTL), and sets the `SecurityContext` with the user's UUID as principal

### Recommendation Engine

The recommendation engine uses rule-based weighted scoring (no ML):

| Factor | Weight | Description |
|---|---|---|
| Availability overlap | 45% | Sweep-line algorithm computes time overlap between windows |
| Proficiency similarity | 20% | Closer proficiency levels score higher |
| Skill rating | 15% | Peer-rated skill score similarity (from ratings system) |
| Overall user rating | 10% | Aggregate rating similarity (from ratings system) |
| Completed sessions | 10% | Verified collaborations (experience bonus) |

**How a recommendation request flows:**

1. Client calls `GET /api/recommendations?skillId=X&dayType=WEEKDAY&numberOfRecommendations=10`
2. `RecommendationService` validates the requester has the requested skill and has availability for the given day type
3. A single JPQL query fetches all candidate users who share the skill and have matching day-type availability (excluding the requester)
4. Candidates are grouped by user ID (since one user can have multiple time slots)
5. `RecommendationEngine` scores each candidate using `ScoreCalculator`:
   - **Time score:** `TimeMatcher` uses a sweep-line algorithm to find the best overlap (or closest distance) between the requester's and candidate's time windows. Overlap is capped at a configurable max (default 4 hours). If no overlap, an inverse-distance decay function still gives some credit for being close
   - **Proficiency score:** `1.0 - (|requester_level - candidate_level| / max_diff)` where levels are BEGINNER(0), AMATEUR(1), INTERMEDIATE(2), EXPERT(3)
   - **Skill rating score:** `1.0 - |normalize(candidate) - normalize(requester)|` where ratings are normalized to 0-1 scale (divided by 5). Returns 0 if either user has no ratings yet
   - **User rating score:** Same formula as skill rating, applied to overall user ratings
   - **Final score:** `time*0.45 + proficiency*0.20 + skillRating*0.15 + userRating*0.10 + sessions*0.10`
6. A min-heap (`PriorityQueue`) of size N efficiently selects the top-N candidates — O(n log k) where n = candidates, k = requested count
7. Results are returned sorted by score descending

### Ratings and Score Recalculation

1. Client calls `POST /api/ratings` with `{toUserId, skillId, rating (1-5), feedback (optional, max 500 chars)}`
2. `RatingService` validates: not self-rating, both users have the skill, no duplicate rating for this (rater, rated, skill) triple
3. The rating is saved to the `ratings` table
4. **Eager recalculation:** the system immediately recomputes:
   - `UserSkill.rating` = AVG of all ratings this user received for this specific skill
   - `User.overallRating` = AVG of all ratings this user received across all skills
5. These denormalized averages are what the recommendation engine reads — no on-the-fly aggregation needed at query time

### Real-Time Chat (WebSocket + STOMP)

Chat has two layers: REST endpoints for history/conversations and WebSocket for real-time message delivery.

**WebSocket connection and authentication:**

1. Client opens a WebSocket connection to `ws://localhost:8080/ws?token=<JWT>`
2. The `/ws` endpoint is excluded from Spring Security's HTTP filter chain (permitted in `SecurityConfig`)
3. `JwtHandshakeInterceptor` runs during the HTTP upgrade handshake:
   - Extracts the `token` query parameter
   - Validates the JWT via `JwtService` (signature + expiry)
   - Checks the user still exists via `UserService`
   - Stores `userId` and `role` in the WebSocket session attributes
   - Returns `false` (rejects handshake) if any check fails
4. After the WebSocket connection is established, the client sends a STOMP CONNECT frame
5. `WebSocketAuthChannelInterceptor` intercepts the CONNECT frame, reads `userId`/`role` from session attributes, and sets a `UsernamePasswordAuthenticationToken` as the STOMP session's `Principal`

**Sending a message in real time:**

1. Client sends a STOMP SEND frame to `/app/chat.send` with payload `{"recipientId":"<UUID>","content":"Hello!"}`
2. `WebSocketChatController.sendMessage()` handles it:
   - Extracts `senderId` from the `Principal` (set during CONNECT)
   - Delegates to `ChatService.sendMessage()` which persists the message to the database (same logic as the REST endpoint)
3. The persisted `MessageResponse` is pushed to both users via `SimpMessagingTemplate.convertAndSendToUser()`:
   - Recipient receives it at `/user/queue/messages`
   - Sender also receives it at `/user/queue/messages` (so their UI updates)

**Subscribing to receive messages:**

- Clients subscribe to `/user/queue/messages` after connecting
- Spring resolves `/user/{userId}/queue/messages` internally based on the `Principal`, so each user only receives their own messages

**REST endpoints for history (Phase 1):**

- `POST /api/chat/messages` — send a message (alternative to WebSocket, same persistence logic)
- `GET /api/chat/conversations` — list all conversations for the current user, with last message preview (truncated to 100 chars)
- `GET /api/chat/conversations/{id}/messages` — full message history for a conversation (verifies the user is a participant)

**Conversation deduplication:** conversations are stored with sorted UUID pairs (`participant_1_id < participant_2_id`) so the same two users always map to one conversation row. `ChatService.findOrCreateConversation()` sorts the UUIDs before looking up or creating.

### Idempotent Profile Updates (Full-Replace Pattern)

Setting skills and availability is idempotent — `POST /api/user/skills` and `POST /api/user/availability` always replace the user's entire set:

1. All existing records (skills or availability) for the user are deleted
2. The new set from the request is saved
3. Sending an empty list clears all entries

This avoids complex merge logic and conflicts.

## Testing Chat

REST endpoints can be tested with Postman/curl. For WebSocket real-time chat, a built-in test page is available:

1. Start the app (`./mvnw spring-boot:run`)
2. Open **two browser tabs** to `http://localhost:8080/chat-test.html`
3. In **Tab 1**: fill in email/password/display name and click **Register** (or **Login** if already registered)
4. In **Tab 2**: register/login as a different user
5. In both tabs: click **Connect** — status should turn green
6. Copy the other user's ID from the debug log, paste it as **Recipient User ID**
7. Type a message and hit **Send** — both tabs see the message in real time

On connect, the page auto-fetches all conversation history via REST so you can see messages that were sent while you were offline. New real-time messages appear after the history separator.

The page has zero external dependencies — it implements a minimal STOMP client inline.

## Configuration

Key environment variables (with defaults for local dev):

| Variable | Default | Purpose |
|---|---|---|
| `JWT_SECRET` | Dev key (change in prod) | JWT signing key |
| `ADMIN_EMAIL` | `admin@pairr.com` | Default admin email |
| `ADMIN_PASSWORD` | `admin123` | Default admin password |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Allowed frontend origin(s) for CORS |
| `GOOGLE_CLIENT_ID` | - | Google OAuth Client ID |
| `GOOGLE_CLIENT_SECRET` | - | Google OAuth Client Secret |
| `PORT` | `8080` | Server port (auto-set by Railway) |

## Deployment

The backend is deployed on **Railway** with a **PostgreSQL** database. The frontend (React) is deployed on **Vercel**.

### Production Profile

Railway activates the production profile via `SPRING_PROFILES_ACTIVE=prod`, which overrides:
- DB connection → uses Railway's PostgreSQL env vars (`PGHOST`, `PGPORT`, etc.)
- SQL logging → disabled
- Swagger UI → disabled
- Actuator → exposes only `/actuator/health`

### Railway Environment Variables

| Variable | Value | Notes |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | Activates production config |
| `JWT_SECRET` | Random 32+ byte string | **Required** — no insecure default in prod |
| `ADMIN_EMAIL` | Your admin email | **Required** |
| `ADMIN_PASSWORD` | Strong password | **Required** |
| `CORS_ALLOWED_ORIGINS` | `https://pairr.vercel.app` | Your Vercel frontend URL |
| `NIXPACKS_JDK_VERSION` | `17` | Ensures correct Java version |
| `PGHOST`, `PGPORT`, `PGUSER`, `PGPASSWORD`, `PGDATABASE` | Auto-provided | From Railway PostgreSQL service |
| `PORT` | Auto-provided | Railway assigns the port |

### CI/CD

GitHub Actions runs `./mvnw clean test` on every push to `main` and on pull requests. Deployment is handled automatically by Railway on git push (connect your GitHub repo in Railway's dashboard).

## MVP Scope

### Built
- User authentication (register/login with JWT)
- Profile with skills and proficiency levels (BEGINNER, AMATEUR, INTERMEDIATE, EXPERT)
- Availability windows (weekday/weekend with time ranges)
- Recommendation engine with weighted scoring
- Ratings and feedback system (1-5 per skill, aggregated into per-skill and overall scores)
- 1:1 chat — REST endpoints for history + WebSocket (STOMP) for real-time delivery
- Admin-managed skill categories (prevents user-generated chaos)
- Role-based access control (USER / ADMIN)

### Planned (not yet built)
- Timezone support
- Meeting link sharing (Google Meet / Zoom — paste only, no generation)

### Post-MVP
- Group chats, video calls
- Redis for online status
- ElasticSearch for skill search
- AI/ML-powered recommendations
- Notifications, payments
