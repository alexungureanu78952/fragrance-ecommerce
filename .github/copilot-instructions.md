# Fragrance Ecommerce Project Guidelines

## Source of truth

- Follow [docs/IMPLEMENTATION-PLAN.md](../docs/IMPLEMENTATION-PLAN.md) and the
  rubric PDF in the repository.
- Treat the project as an educational simulated shop. Use Stripe test mode only.
- Do not add real credentials, payment card data, or personal data to source,
  fixtures, screenshots, logs, or documentation.

## Working method

- Respect the waterfall phases and exit gates. Do not implement later-phase
  features against an unapproved design.
- Before changing code, inspect related files, existing patterns, API contracts,
  migrations, and tests.
- Make small, coherent changes. Update directly related documentation and the
  rubric traceability matrix.
- Preserve unrelated worktree changes. Never reset or overwrite them.
- Surface errors explicitly; do not silently fall back or swallow exceptions.

## Architecture

- Keep backend responsibilities separated into controller, service, repository,
  domain, security, and configuration layers.
- Keep frontend concerns separated into routes/pages, reusable components, API
  clients, state, and styles.
- Prefer existing helpers and typed contracts over duplicated logic or unsafe casts.
- Validate input at the API boundary and enforce authorization in backend services,
  not only in the UI.

## Quality gates

- Add or update focused tests for every behavior change.
- Run the smallest relevant formatter, linter, type check, and test command.
- Do not call a feature complete until success, loading, empty, error, and
  unauthorized states are addressed where relevant.
- Record security, deployment, logging, and SEO evidence when those rubric items
  are affected.

## Security essentials

- Use parameterized persistence access and server-side validation.
- Preserve CSRF, XSS, CORS, upload restrictions, rate limiting, CAPTCHA, and
  security-header requirements from the implementation plan.
- Never log passwords, tokens, card data, or secrets.
