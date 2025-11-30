.PHONY: help infra-up infra-down backend-build backend-run backend-test frontend-init frontend-dev frontend-build clean

# Default target
help:
	@echo "FORE: Fairways & Greens - Development Commands"
	@echo ""
	@echo "Infrastructure:"
	@echo "  make infra-up       Start PostgreSQL via Docker"
	@echo "  make infra-down     Stop PostgreSQL"
	@echo ""
	@echo "Backend:"
	@echo "  make backend-build  Build all backend modules"
	@echo "  make backend-run    Run game service (requires infra-up)"
	@echo "  make backend-test   Run backend tests"
	@echo "  make backend-ai     Run AI agent service"
	@echo ""
	@echo "Frontend:"
	@echo "  make frontend-init  Initialize frontend (first time only)"
	@echo "  make frontend-dev   Start frontend dev server"
	@echo "  make frontend-build Build frontend for production"
	@echo ""
	@echo "Utilities:"
	@echo "  make clean          Clean all build artifacts"
	@echo "  make db-reset       Reset database (destroys data)"

# Infrastructure
infra-up:
	docker compose -f docker/docker-compose.yml up -d
	@echo "Waiting for PostgreSQL to be ready..."
	@sleep 3
	@echo "PostgreSQL is running on localhost:5432"

infra-down:
	docker compose -f docker/docker-compose.yml down

# Backend
backend-build:
	cd backend && ./gradlew build -x test

backend-run:
	cd backend && ./gradlew :fore-game-service:bootRun --args='--spring.profiles.active=local'

backend-ai:
	cd backend && ./gradlew :fore-ai-agent:bootRun

backend-test:
	cd backend && ./gradlew test

# Frontend
frontend-init:
	cd frontend && npm install

frontend-dev:
	cd frontend && npm run dev

frontend-build:
	cd frontend && npm run build

# Utilities
clean:
	cd backend && ./gradlew clean
	rm -rf frontend/node_modules frontend/dist

db-reset:
	docker compose -f docker/docker-compose.yml down -v
	docker compose -f docker/docker-compose.yml up -d
	@echo "Database reset complete"
