# Fragrance Ecommerce Platform

Educational fragrance ecommerce platform built in phases according to
[`docs/IMPLEMENTATION-PLAN.md`](docs/IMPLEMENTATION-PLAN.md).

This project is for educational use only. It simulates ecommerce workflows and
must not be used to process real payments or store real payment card data.

## Prerequisites

- Docker Desktop with Docker Compose (enough to run the whole stack)
- Java 21 and Node.js 22 with npm (only for development outside Docker)

## Run everything in Docker

1. Copy `.env.example` to `.env` and enter your local values.
2. Build and start PostgreSQL, the backend, and Apache:

   ```text
   docker compose up -d --build
   ```

3. Open <http://localhost:8081>. The first start creates the schema and demo data.

| Service | Container | Reachable from the host |
| --- | --- | --- |
| Apache (frontend + `/api` proxy) | `web` | <http://localhost:8081> |
| Spring Boot API | `backend` | Only through Apache at `/api` |
| PostgreSQL 16 | `postgres` | `localhost:5433` (DBeaver) |

- Logs: `docker compose logs -f backend`, or the log file `backend/logs/fragrance-backend.log`.
- After code changes: `docker compose up -d --build`.
- Stop: `docker compose down` (keeps the data).

The backend reads secrets from `.env` at runtime; they are not built into the
images. The hCaptcha *site key* is public and is built into the frontend bundle,
so rebuild with `--build` after changing it.

## Local development setup

Use this to run the backend from the IDE and the frontend with hot reload.

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

`VITE_SITE_URL` is the public origin of the storefront (default
`http://localhost:5173`). The build uses it for the canonical and Open Graph URLs
and generates `robots.txt` and `sitemap.xml` from it. The `build:demo` and
`build:pages` scripts take their values from the committed, non-secret
`.env.demo` and `.env.pages` files.

## Database

- PostgreSQL 16 runs in Docker and is published on **host port 5433**
  (`POSTGRES_PORT`), so it does not clash with a locally installed PostgreSQL on
  5432.
- Flyway creates the schema from `backend/src/main/resources/db/migration` the
  first time the backend starts. It then applies the demo seed in
  `backend/src/main/resources/db/demo`: the accounts below, three brands, three
  categories, and six products. Set `FLYWAY_LOCATIONS=classpath:db/migration` to
  skip the seed.
- Data lives in the named Docker volume `fragrance-ecommerce_postgres-data` and
  survives restarts and `docker compose down`. Flyway records applied migrations,
  so nothing is re-run or overwritten. `docker compose down -v` deletes the volume;
  the next backend start recreates the schema and demo data from scratch.
- The database password is `POSTGRES_PASSWORD` in `.env`. PostgreSQL reads it
  only when the volume is first created, so after changing it run
  `docker compose down -v` (this deletes the data).

To inspect the data model in DBeaver: **New connection → PostgreSQL**, host
`localhost`, port `5433`, database `fragrance_db`, user `fragrance_app`, password
from `POSTGRES_PASSWORD`. Then right-click **fragrance_db → Schemas → public →
View Diagram** to show the tables and their foreign keys.

## Local demo accounts

These fake accounts are created by the demo seed for local testing only:

| Role | Email | Password |
| --- | --- | --- |
| Customer | `demo.customer@example.test` | `DemoCustomer!2026` |
| Administrator | `demo.admin@example.test` | `DemoAdmin!2026` |

Do not reuse these credentials outside the local educational environment.

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
stripe listen `
  --events checkout.session.completed,checkout.session.async_payment_failed,checkout.session.expired `
  --forward-to http://localhost:8081/api/v1/webhooks/stripe
```

Use `http://localhost:8080/...` instead when the backend runs from the IDE.
Copy the `whsec_...` secret printed by the CLI into `STRIPE_WEBHOOK_SECRET` in
`.env`, then restart the backend (`docker compose up -d backend`).

## Hosting and demonstration

The project uses two free setups:

- **Online (GitHub Pages):** `.github/workflows/pages.yml` publishes the static
  frontend to <https://alexungureanu78952.github.io/fragrance-ecommerce/> on every
  push to `main` that changes the frontend. The backend is not hosted, so the
  catalogue shows its error state there. Use this URL for the hosting criterion
  and the Lighthouse SEO audit. One-time setup: in the repository, set
  **Settings → Pages → Source** to **GitHub Actions**, and add the public hCaptcha
  site key as the repository variable `VITE_CAPTCHA_SITE_KEY`.
- **Live demo (Docker on your machine):** Apache serves the built frontend, applies
  `frontend/public/.htaccess`, and proxies `/api` to the backend container, so the
  browser sees a single origin at <http://localhost:8081>.

### Demo-day checklist

1. For checkout, make sure `.env` contains:

   ```text
   STRIPE_SUCCESS_URL=http://localhost:8081/#/checkout/success
   STRIPE_CANCEL_URL=http://localhost:8081/#/checkout/cancel
   ```

2. Start the stack: `docker compose up -d --build`.
3. Start `stripe listen` (see above, forwarding to port 8081).
4. Open <http://localhost:8081>, and DBeaver on `localhost:5433` for the data model.

| Rubric criterion | How to show it |
| --- | --- |
| Hosting | Open the GitHub Pages URL |
| SEO ≥ 90 | Chrome DevTools → Lighthouse → SEO on the GitHub Pages URL |
| Security headers (A) | Security Headers Inspector on <http://localhost:8081> |
| `.htaccess` | <http://localhost:8081/.env>, `/.htaccess`, `/assets/` all return 403 |
| Logging | `backend/logs/fragrance-backend.log` — `Audit event actor=… action=…` lines for register, login, logout, orders, checkout, and admin changes |
| Stripe | Checkout with card `4242 4242 4242 4242`; the order turns `PAID` after the webhook |
| Unit tests and CI | GitHub Actions run of `CI`, with test-report artifacts |
