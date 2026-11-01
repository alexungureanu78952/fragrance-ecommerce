---
description: "Use when writing or reviewing Spring Boot, Java, JPA, PostgreSQL, Flyway, REST API, authentication, authorization, payment, upload, or backend test code."
applyTo:
  - "backend/**/*.java"
  - "backend/**/*.sql"
  - "backend/**/*.yml"
  - "backend/**/*.yaml"
  - "backend/**/*.properties"
---
# Backend Guidelines

- Keep the controller, service, repository, and domain boundaries clear.
- Use typed request/response DTOs; do not expose persistence entities as an
  accidental public API.
- Validate input at the boundary and enforce authorization in the service layer.
- Use Flyway for schema changes. Keep migrations ordered, reviewable, and
  compatible with the 3NF design.
- Use parameterized JPA or query APIs. Never construct SQL from user input.
- Use explicit domain exceptions and a consistent error response. Do not catch
  broad exceptions without logging and a deliberate response.
- Hash passwords with the configured Spring Security encoder. Keep secrets in
  environment variables.
- Treat Stripe as test-only and store provider identifiers/status, never card data.
- Add tests for business rules, authorization failures, validation, and external
  service failures.
