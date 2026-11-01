---
description: "Use when adding tests, CI workflows, security controls, logging, deployment configuration, uploads, rate limits, CAPTCHA, headers, or rubric verification."
applyTo:
  - "**/*Test.java"
  - "**/*.test.ts"
  - "**/*.test.tsx"
  - "**/*.spec.ts"
  - ".github/workflows/**"
  - "infra/**"
  - "docker-compose*.yml"
  - "Dockerfile*"
---
# Testing and Security Guidelines

- Test measurable requirements directly, including the 12 MB upload limit and the
  100-requests-per-minute threshold.
- Prefer deterministic tests with isolated data and no real external credentials.
- Keep at least five meaningful unit tests covering business rules.
- CI must install, lint, test, and build the affected applications from a clean
  checkout.
- Verify authentication, authorization, CSRF, XSS, SQL injection resistance,
  upload extension/MIME checks, rate limiting, CAPTCHA integration, and security
  headers.
- Log user and administrative actions without logging secrets, passwords, tokens,
  or payment data.
- Deployment configuration must fail clearly when required environment variables
  are missing; do not silently use insecure defaults.
- Update the rubric traceability matrix and evidence list when a requirement is
  tested or verified.
