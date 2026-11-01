# Fragrance Ecommerce Platform — Implementation Plan

## 1. Project objective

Build an educational fragrance ecommerce platform that satisfies every criterion in
`Barem_evaluare_proiect_web.pdf` and can be demonstrated end to end.

The application is a simulated shop. It must not process real payments, store real
payment card data, or be presented as a production commercial service.

## 2. Baseline technology decisions

| Area | Decision |
|---|---|
| Backend | Java 21, Spring Boot 3, Spring MVC, Spring Data JPA, Spring Security |
| Database | PostgreSQL with Flyway migrations |
| Frontend | React, TypeScript, Vite |
| API style | JSON REST API |
| Payments | Stripe test mode only; no card data stored by this application |
| Testing | JUnit 5, Mockito, Spring Boot Test, Vitest, React Testing Library |
| Delivery | Docker Compose for local development and GitHub Actions for CI |
| Deployment | HTTPS reverse proxy, hosted frontend, hosted backend, hosted PostgreSQL |

Any change to these decisions requires recording the reason in the relevant phase
decision log before implementation continues.

## 3. Waterfall rules

1. Complete each phase's deliverables before starting the next phase.
2. Do not mark a phase complete until its exit criteria are verified.
3. Keep the rubric traceability matrix up to date whenever a feature is added.
4. Every user-visible feature needs success, loading, empty, error, and unauthorized
   behavior where applicable.
5. Every backend feature needs validation, authorization analysis, tests, and logging
   analysis before it is considered complete.
6. Never use real credentials, payment data, or personal data in source code,
   fixtures, screenshots, or demonstrations.
7. Prefer small, reviewable commits that correspond to one coherent change.

## 4. Phase plan and gates

### Phase 0 — Project initialization

**Tasks**

- Create the repository structure for `backend`, `frontend`, `infra`, `docs`, and
  `.github`.
- Add README, environment variable documentation, `.gitignore`, and license or
  educational-use statement.
- Create local Docker Compose services for PostgreSQL and any required supporting
  service.
- Add the initial GitHub Actions skeleton.

**Deliverables**

- Runnable empty backend
- Runnable empty frontend
- Local database startup instructions
- Initial CI workflow

**Exit gate**

- Backend and frontend start locally.
- PostgreSQL is reachable.
- CI can install dependencies and run the initial checks.

### Phase 1 — Requirements and traceability

**Tasks**

- Define actors: guest, customer, and administrator.
- Define user stories and acceptance criteria.
- Define the four minimum complete scenarios:
  1. Admin creates, edits, views, and deletes a product.
  2. Customer registers, logs in, and browses products.
  3. Customer adds products to a cart and creates an order.
  4. Customer completes a Stripe test payment and views order history.
- Maintain the rubric matrix below with implementation and evidence links.

**Exit gate**

- All functional and rubric requirements have an owner, planned phase, and
  measurable acceptance test.

### Phase 2 — Architecture and database design

**Tasks**

- Produce an entity-relationship diagram in `docs/`.
- Design a 3NF schema for users, roles, products, brands, categories, images, carts,
  orders, order items, payments, reviews, and audit logs.
- Define REST endpoints, request/response shapes, error format, and pagination.
- Define authentication, authorization, upload, logging, and deployment boundaries.
- Define inheritance and interface usage intentionally:
  `User` with `Customer` and `Administrator`, plus a payment service interface.

**Exit gate**

- Schema passes a 3NF review.
- API contract and security design are approved.
- No implementation begins against an undocumented endpoint or table.

### Phase 3 — UX and visual design

**Tasks**

- Produce wireframes and a navigation map for catalogue, product details, cart,
  checkout, authentication, customer account, and admin pages.
- Define responsive behavior for desktop, tablet, and mobile.
- Define typography, colors, spacing, focus states, and error presentation.
- Plan the product image carousel and at least one purposeful animation.

**Exit gate**

- Every planned screen has a route, primary action, empty state, error state, and
  responsive behavior.

### Phase 4 — Backend foundation and access control

**Tasks**

- Implement configuration, migrations, entities, repositories, services, and
  controllers.
- Implement registration, login, password hashing, session/token handling, and
  role-based authorization.
- Add validation, structured error responses, and global exception handling.
- Add audit logging for authentication and administrative actions.

**Exit gate**

- Authentication and authorization tests pass.
- Customer endpoints cannot be used to perform admin actions.
- Secrets are externalized and no sensitive values are committed.

### Phase 5 — Catalogue and administration

**Tasks**

- Implement product, brand, category, stock, price, and image management.
- Implement public catalogue, product details, search, filters, sorting, and
  pagination.
- Implement upload limits: maximum 12 MB and approved extensions/MIME types.
- Add server-side validation and safe file handling.

**Exit gate**

- Admin product CRUD and customer catalogue browsing are complete scenarios.
- SQL injection, authorization, and upload validation tests pass.

### Phase 6 — Cart, orders, and payment

**Tasks**

- Implement cart creation, item changes, stock checks, and total calculation.
- Implement immutable order snapshots and order status transitions.
- Integrate Stripe test mode using server-side payment creation and webhook
  verification where applicable.
- Store only payment-provider identifiers and payment status.
- Implement customer order history and admin order management.

**Exit gate**

- The complete checkout scenario works with Stripe test credentials.
- Failed payment, insufficient stock, duplicate submission, and unauthorized
  access are handled explicitly.

### Phase 7 — Frontend implementation and responsive design

**Tasks**

- Implement routes, API client, authentication state, catalogue, product details,
  carousel, cart, checkout, payment result, account, and admin screens.
- Add loading, empty, error, and permission-denied states.
- Implement at least two organized CSS files and responsive layouts.
- Add semantic HTML, keyboard navigation, accessible labels, and image alt text.

**Exit gate**

- All four minimum scenarios work through the frontend on desktop, tablet, and
  mobile viewport sizes.
- Browser console has no avoidable errors.

### Phase 8 — Security hardening

**Tasks**

- Verify CSRF protection, XSS-safe rendering, parameterized persistence access, and
  strict CORS.
- Add request rate limiting: block additional requests after 100 consecutive
  requests in under one minute.
- Add CAPTCHA to abuse-prone flows such as registration, login, or checkout.
- Inspect uploaded file content and metadata to reduce steganography risk.
- Configure security headers and verify an A result with Security Headers Inspector.
- Protect configuration, logs, build files, and private resources at the proxy/server
  boundary. Include `.htaccess` rules if required by the evaluator.

**Exit gate**

- Each of the ten security criteria has a test or captured verification evidence.
- Security failures produce explicit errors and useful audit logs without leaking
  secrets.

### Phase 9 — Testing and automation

**Tasks**

- Add at least five meaningful unit tests; target business rules rather than trivial
  getters and setters.
- Add integration tests for persistence and authentication.
- Add frontend component tests and an end-to-end checkout smoke test.
- Configure CI to install, lint, test, and build backend and frontend.
- Add test data that is deterministic and contains no real personal data.

**Exit gate**

- CI passes from a clean checkout.
- The test report demonstrates the minimum five unit tests and critical scenario
  coverage.

### Phase 10 — SEO, deployment, and final acceptance

**Tasks**

- Add titles, descriptions, canonical URLs, sitemap, robots file, Open Graph data,
  semantic headings, alt text, and structured product metadata where appropriate.
- Deploy frontend, backend, database, and HTTPS reverse proxy.
- Configure production-like environment variables and database migrations.
- Configure application logging to a retrievable file.
- Run Lighthouse and Security Headers Inspector.
- Capture rubric evidence and rehearse the demonstration.

**Exit gate**

- Public URL is reachable.
- Lighthouse SEO score is at least 90/100.
- Logs can be consulted.
- CI is green.
- All rubric criteria have evidence.

## 5. Rubric traceability matrix

| Rubric area | Planned evidence |
|---|---|
| 3NF database | [Requirements and traceability](REQUIREMENTS-AND-TRACEABILITY.md), [architecture and database design](ARCHITECTURE-AND-DATABASE.md), migrations, schema review |
| Four main scenarios | [Requirements and traceability](REQUIREMENTS-AND-TRACEABILITY.md), end-to-end test report, demonstration script |
| MVC/Repository architecture | [Requirements and traceability](REQUIREMENTS-AND-TRACEABILITY.md), [architecture and database design](ARCHITECTURE-AND-DATABASE.md), source structure |
| Images, text, tables, links | [Requirements and traceability](REQUIREMENTS-AND-TRACEABILITY.md), frontend screenshots, component tests |
| Three classes and one interface | [Requirements and traceability](REQUIREMENTS-AND-TRACEABILITY.md), backend source, design document |
| Authentication and authorization | [Requirements and traceability](REQUIREMENTS-AND-TRACEABILITY.md), security tests, role demonstration |
| Inheritance | [Requirements and traceability](REQUIREMENTS-AND-TRACEABILITY.md), `User` hierarchy, tests |
| Two exception types | [Requirements and traceability](REQUIREMENTS-AND-TRACEABILITY.md), exception classes, handlers, tests |
| Carousel and animation | [Requirements and traceability](REQUIREMENTS-AND-TRACEABILITY.md), [UX and visual design](UX-AND-VISUAL-DESIGN.md), browser test, screenshots |
| CSS and responsive design | [Requirements and traceability](REQUIREMENTS-AND-TRACEABILITY.md), [UX and visual design](UX-AND-VISUAL-DESIGN.md), CSS files, viewport test evidence |
| Five unit tests | [Requirements and traceability](REQUIREMENTS-AND-TRACEABILITY.md), CI test report |
| Git and GitHub Actions | [Requirements and traceability](REQUIREMENTS-AND-TRACEABILITY.md), repository history, workflow run |
| Logging | [Requirements and traceability](REQUIREMENTS-AND-TRACEABILITY.md), log file, documented event examples |
| Hosting | [Requirements and traceability](REQUIREMENTS-AND-TRACEABILITY.md), public URL |
| SEO 90+ | [Requirements and traceability](REQUIREMENTS-AND-TRACEABILITY.md), Lighthouse report |
| Stripe test payment | [Requirements and traceability](REQUIREMENTS-AND-TRACEABILITY.md), test-mode checkout evidence |
| Security levels I and II | [Requirements and traceability](REQUIREMENTS-AND-TRACEABILITY.md), security test report, tool reports |

## 6. Definition of done

A feature is done only when:

- Its acceptance criteria are met.
- Its API and data changes are documented.
- Validation and authorization are implemented.
- Relevant unit/integration/frontend tests pass.
- Errors and audit logging are handled.
- Responsive and accessible UI behavior is checked when applicable.
- CI passes.
- The rubric matrix and evidence list are updated.

## 7. First implementation batch

The first coding batch should be limited to project initialization:

1. Create the directory structure.
2. Generate the Spring Boot and React applications.
3. Add Docker Compose PostgreSQL.
4. Add environment variable templates.
5. Add initial build/test CI.
6. Verify a clean local start and CI run.

No product or payment feature should be implemented until Phase 1 and Phase 2
deliverables are approved.
