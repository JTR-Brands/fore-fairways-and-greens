# FORE: Fairways & Greens

An open-source, enterprise-grade golf-themed property trading game.

## Quick Start
```bash
# Prerequisites: Java 21, Node 20+, Docker

# Start infrastructure
make infra-up

# Run backend
make backend-run

# Run frontend (separate terminal)
make frontend-dev
```

## Project Structure

- `backend/` - Java 21 + Spring Boot 3.x (Gradle multi-module)
- `frontend/` - React + TypeScript + Vite
- `docker/` - Docker Compose for local development
- `docs/` - Architecture documentation

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18, TypeScript, Vite, Zustand |
| Backend | Java 21, Spring Boot 3.2, Gradle |
| Database | PostgreSQL 16 |
| Real-time | WebSockets (STOMP) |
| AI | LLM Agent Service |
