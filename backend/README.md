# Rural EdTech Platform - Group 4 Backend

## Overview
This is the FastAPI backend for the Group 4 components of the Rural EdTech Platform. It serves as an optional synchronization, analytics, and content management layer.

## Architecture
- **Framework**: FastAPI (Python 3.11)
- **Database**: PostgreSQL 15 (SQLAlchemy ORM + Alembic Migrations)
- **Object Storage**: MinIO (S3 compatible)
- **Deployment**: Docker & Docker Compose

## Quick Start (Docker)
1. Ensure Docker Desktop is running.
2. In the `backend` directory, run:
   ```bash
   docker compose up -d --build
   ```
3. To run database migrations inside the API container:
   ```bash
   docker exec -it edtech-fastapi alembic upgrade head
   ```
4. To seed the database with demo data:
   ```bash
   docker exec -it edtech-fastapi python scripts/seed.py
   ```
5. Access the API documentation at `http://localhost:8000/docs`

## Core Concepts
- **Offline First**: The API relies on idempotent, event-based synchronization. Devices can push hundreds of cached `LearningEvents` when internet becomes available via `POST /api/v1/sync/events`.
- **Teacher Analytics**: Aggregates event data into actionable insights (`GET /api/v1/teacher/dashboard`).
- **Content Distribution**: Teachers can upload content packages to the backend, which stores the metadata in PostgreSQL and the binary zip in MinIO.

## Default Credentials (if seeded)
- Email: `teacher@demo.com`
- Password: `password123`
