# Pairr Frontend

A modern, real-time React SPA for **Pairr** — a skill-based partner matching platform. Pairr allows developers to discover collaborators based on skill proficiency, overlap in availability, and verified ratings.

## 🚀 Tech Stack

| Concern | Technology |
|---|---|
| **Core** | React 19 (Functional Components, Hooks) |
| **Build Tool** | Vite 7 |
| **Styling** | Tailwind CSS v4 (with fluid utility classes) |
| **Routing** | React Router Dom v7 |
| **Real-time** | STOMP.js (@stomp/stompjs) over WebSockets |
| **API Client** | Native Fetch API with custom middleware wrapper |
| **State Management** | React Context API (Auth & Chat) |

---

## 🏗️ Architecture

### 1. Context-Driven State
- **AuthContext**: Manages JWT lifecycle, user profile synchronization, and persistent storage in `localStorage`. Includes automatic token decoding for immediate UI responsiveness.
- **ChatContext**: A centralized "live" hub managing WebSocket connections, active pairing sessions, paginated message histories, and global notification counts.

### 2. Real-time Communication (WebSockets)
- Uses STOMP protocol to handle asynchronous events.
- **Topics**: 
  - `/user/queue/messages`: Real-time 1:1 chat delivery.
  - `/user/queue/pairing`: Instant updates for collaboration requests and status changes.
- **Implementation Note**: In development, the client connects directly to `localhost:8080` to bypass Vite proxy limitations with WebSocket upgrades.

### 3. Resilience & UX
- **Optimistic Updates**: Messages are rendered instantly with a "sending" state before backend confirmation.
- **Polling Fallback**: Includes a redundant 30s polling mechanism for pairing sessions to ensure state integrity if WebSocket heartbeats fail.
- **Smart Scrolling**: Custom logic maintains exact scroll position when loading older messages in a paginated conversation.

---

## ✨ Key Features

### 🤝 Pairing Session Lifecycle
A formal framework for collaboration:
- **Request**: Initiate requests from the recommendations engine.
- **Manage**: Accept, Cancel, or Complete sessions via a dedicated Chat banner or the global "Pending Requests" list.
- **Locking**: Chat is strictly restricted to users with an `ACCEPTED` pairing session to prevent spam and ensure verified interactions.

### 📊 Verified Rating System
- Ratings are only permitted for `ACCEPTED` or `COMPLETED` sessions.
- Prevents duplicate feedback by tracking `ratedByCurrentUser` flag from the backend.
- Feedback is tied to specific skills, directly influencing the recommendation algorithm.

### 🧩 Smart Recommendations
- Ranked matching based on:
  - Time overlap (45% weight)
  - Proficiency similarity (20% weight)
  - Skill/User ratings (25% combined weight)
  - **Experience**: Users with more "Verified Completed Sessions" receive a ranking boost.

### 📝 Progressive Onboarding
- Guided flow for new users: **Skill Selection** → **Availability Setup** → **Dashboard**.
- Availability supports up to 4 discrete slots for both Weekdays and Weekends.

---

## 🛠️ Getting Started

### Prerequisites
- Node.js 18+
- Pairr Backend (Spring Boot) running on `localhost:8080`.

### Installation
```bash
npm install
npm run dev
```
The app runs at `http://localhost:5173`. 

### Environment Variables
| Variable | Description |
|---|---|
| `VITE_API_URL` | Backend API base URL (Leave empty in dev to use Vite Proxy). |

---

## 📂 Project Structure
```
src/
  ├── api/client.js          # Fetch wrapper with auto-401 handling & error parsing
  ├── components/
  │   ├── Chat/              # ChatWidget, Window, List, and RatingModal
  │   ├── UI/                # Sleek, reusable components like ErrorMessage
  │   └── ...                # Onboarding & Profile Modals
  ├── context/
  │   ├── AuthContext.jsx    # Auth Provider & lifecycle
  │   └── ChatContext.jsx    # WebSocket hub & Session management
  └── pages/
      ├── Dashboard.jsx      # Core app shell & Profile overview
      └── OAuthCallback.jsx  # Google OAuth2 success handler
```
