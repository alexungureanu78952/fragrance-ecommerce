# Fragrance Ecommerce Platform

Educational fragrance ecommerce platform built in phases according to
[`docs/IMPLEMENTATION-PLAN.md`](docs/IMPLEMENTATION-PLAN.md).

This project is for educational use only. It simulates ecommerce workflows and
must not be used to process real payments or store real payment card data.

## Prerequisites

- Java 21
- Node.js 22 and npm
- Docker Desktop with Docker Compose

## Local setup

1. Copy `.env.example` to `.env` and replace the local-only password if desired.
2. Start PostgreSQL:

   ```text
   docker compose up -d postgres
   ```

3. Start the backend:

   ```text
   cd backend
   .\mvnw.cmd spring-boot:run
   ```

4. Install and start the frontend in a second terminal:

   ```text
   cd frontend
   npm install
   npm run dev
   ```

The backend and frontend are intentionally empty scaffolds at the end of Phase 0.
Feature implementation starts only after the requirements and architecture phases
are approved.
