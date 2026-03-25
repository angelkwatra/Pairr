# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Frontend for **Pairr** — a skill-based partner matching app. React SPA that communicates with a Spring Boot REST API backend (separate repo at `../pairr`). Users register, declare skills with proficiency levels, set availability, get ranked partner recommendations, chat in real time, and rate each other.

## Build & Run Commands

```bash
npm run dev       # Start dev server (http://localhost:5173)
npm run build     # Production build → dist/
npm run lint      # ESLint
npm run preview   # Preview production build locally
```

The backend must be running on `localhost:8080` for API calls to work in dev (Vite proxy handles this).

## Tech Stack

| Concern | Library |
|---|---|
| Framework | React 19 |
| Build | Vite 7 |
| Styling | Tailwind CSS v4 |
| Routing | react-router-dom v7 |
| WebSocket | @stomp/stompjs |
| Deployment | Vercel |

## Architecture

### API Client (`src/api/client.js`)

Thin `fetch` wrapper. Auto-injects JWT from `localStorage` into `Authorization: Bearer` header. On 401, clears token and redirects to `/login`. All API calls go through `api.get()`, `api.post()`, `api.put()`, `api.delete()`.

`VITE_API_URL` env var controls the base URL — empty in dev (Vite proxy forwards `/api/*` and `/ws` to `localhost:8080`), set to the Railway backend URL in production.

### Auth (`src/context/AuthContext.jsx`)

React context providing `{ token, user, login, logout, isAuthenticated }`. Token and user object are persisted in `localStorage`. `useAuth()` hook for access. `ProtectedRoute` component wraps authenticated pages.

### Routing (`src/App.jsx`)

- `/login` → Login page (public)
- `/register` → Register page (public)
- `/` → Dashboard (protected)
- `*` → Redirects to `/`

### Vite Config (`vite.config.js`)

Dev server proxies `/api` and `/ws` to `localhost:8080` so no CORS issues during development.

## Backend API Reference

The backend API docs are at `http://localhost:8080/swagger-ui.html` when running locally. Key endpoints:

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register (returns JWT) |
| POST | `/api/auth/login` | Login (returns JWT) |
| GET | `/api/categories` | List skill categories |
| GET | `/api/skills` | List all skills |
| POST | `/api/user/skills` | Add skills to profile (bulk) |
| GET | `/api/user/skills` | Get your skills |
| POST | `/api/user/availability` | Set availability (full replace) |
| GET | `/api/user/availability` | Get your availability |
| GET | `/api/recommendations` | Get partner recommendations (params: skillId, dayType, numberOfRecommendations) |
| POST | `/api/ratings` | Rate a user on a skill |
| GET | `/api/ratings?userId=` | Get ratings for a user |
| POST | `/api/chat/messages` | Send a chat message |
| GET | `/api/chat/conversations` | List your conversations |
| GET | `/api/chat/conversations/{id}/messages` | Get message history |

WebSocket: connect to `/ws?token=<JWT>`, send to `/app/chat.send`, subscribe to `/user/queue/messages`.

## Deployment

Deployed on **Vercel**. Set `VITE_API_URL` to the Railway backend URL (e.g. `https://pairr-backend.up.railway.app`) in Vercel's environment variables.
