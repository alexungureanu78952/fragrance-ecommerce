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

1. Copy `.env.example` to `.env` and enter your local values. The root `.env` is
   ignored by Git, loaded automatically by Docker Compose, and imported by the
   backend when it is started from the `backend` directory.
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

The frontend reads `VITE_API_URL` from the root `.env` when using the default
local URL. If you change it, restart the Vite development server.

Stripe checkout uses test mode only. Put `STRIPE_SECRET_KEY` and
`STRIPE_WEBHOOK_SECRET` from a local Stripe test account in `.env` when
demonstrating payments; never commit them or real card data.

CAPTCHA is enabled for registration and login. Create an hCaptcha site in the
hCaptcha dashboard, then put its public site key in `VITE_CAPTCHA_SITE_KEY` and
its private verification secret in `CAPTCHA_SECRET`. Keep
`CAPTCHA_ENABLED=true`. The site key is sent to the browser; the secret must
remain backend-only.

For Stripe webhooks, run the Stripe CLI in a separate terminal:

```text
stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe
```

Copy the `whsec_...` secret printed by the CLI into `STRIPE_WEBHOOK_SECRET` in
`.env`, then restart the backend.
