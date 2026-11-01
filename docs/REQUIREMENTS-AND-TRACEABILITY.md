# Requirements and traceability

This document is the Phase 1 requirements baseline for the educational simulated
fragrance shop. It translates the rubric and implementation plan into measurable
acceptance criteria. No real credentials, payment card data, or personal data may
be used in implementation or evidence.

## Actors

| Actor | Description | Allowed capabilities |
|---|---|---|
| Guest | Unauthenticated visitor | Browse the public catalogue and product details; register or log in |
| Customer | Authenticated shopper | Manage their cart, create orders, complete test payments, review their own order history, and submit reviews |
| Administrator | Authenticated staff role | Manage products, brands, categories, stock, images, and orders; view relevant audit events |

Authorization must be enforced by backend services and controllers, not only by
frontend visibility.

## Minimum complete scenarios

### S1 — Administrator manages a product

**Owner:** Catalogue and administration  
**Planned phase:** Phase 5, using access control from Phase 4

1. An administrator authenticates and opens the product management screen.
2. The administrator creates a valid product with brand, category, price, stock,
   description, and approved image data.
3. The product appears in the administrator's list and the public catalogue.
4. The administrator edits product fields and the updated values are shown.
5. The administrator views product details and deletes the product.
6. The deleted product is no longer available through the public catalogue.

**Acceptance test:** An integration test proves that an administrator can complete
the CRUD sequence, while a customer receives an authorization error for the
administrative endpoints. Validation rejects malformed prices, negative stock,
missing required fields, oversized files, and disallowed file types.

### S2 — Customer registers, logs in, and browses products

**Owner:** Authentication and catalogue  
**Planned phase:** Phase 4 and Phase 5

1. A guest registers with valid credentials and receives an explicit success response.
2. Invalid or duplicate registration data receives a structured validation error.
3. The customer logs in and receives the configured authenticated session/token.
4. The customer browses paginated products, opens product details, searches,
   filters, and sorts results.
5. Loading, empty, error, and unauthorized states are visible and actionable.

**Acceptance test:** Backend integration tests cover registration, password
hashing, login, duplicate credentials, invalid credentials, and role enforcement.
Frontend tests cover catalogue success, loading, empty, and error states.

### S3 — Customer creates an order

**Owner:** Cart and orders  
**Planned phase:** Phase 6

1. An authenticated customer adds an in-stock product to a cart.
2. The customer changes quantities or removes items.
3. The server validates stock and calculates totals from trusted product data.
4. The customer submits the cart and receives an immutable order snapshot.
5. Insufficient stock, duplicate submission, invalid quantities, and unauthorized
   access receive explicit errors.

**Acceptance test:** Service tests prove quantity validation, stock enforcement,
server-side total calculation, idempotency protection, and immutable order item
snapshots.

### S4 — Customer completes a Stripe test payment

**Owner:** Payments and order history  
**Planned phase:** Phase 6

1. The customer starts checkout for an existing order.
2. The backend creates a Stripe test-mode payment using server-side credentials.
3. The application stores only provider identifiers and payment status.
4. A verified payment result changes the order to the appropriate status.
5. Failed or canceled payment is shown explicitly without exposing secrets.
6. The customer can view the resulting order and payment status in order history.

**Acceptance test:** Integration tests cover successful, failed, canceled, duplicate,
and unauthorized payment flows. A demonstration uses Stripe test mode only and no
card data is stored by this application.

## Rubric traceability matrix

| Rubric requirement | Owner | Planned phase | Measurable acceptance test | Evidence |
|---|---|---:|---|---|
| Database is in 3NF | Data design | 2 | ERD and schema review show atomic attributes, no repeating groups, and dependencies on the complete key | [Architecture and database design](ARCHITECTURE-AND-DATABASE.md), migrations, review record |
| Four complete scenarios | Product team | 4–7 | S1–S4 pass their scenario acceptance tests end to end | Test report and demonstration script |
| MVC/Repository architecture | Backend foundation | 2, 4 | Controllers, services, repositories, domain, security, and configuration have separated responsibilities | [Architecture and database design](ARCHITECTURE-AND-DATABASE.md), source tree |
| Images, text, tables, and links | Frontend | 7 | Catalogue and admin screens render each required content type with semantic markup | Component tests and screenshots |
| At least three classes | Backend foundation | 4 | Backend contains at least three meaningful domain/service classes used by the application | Source and tests |
| At least one interface | Payments | 2, 6 | Payment service interface is defined and invoked by the checkout service | [Architecture and database design](ARCHITECTURE-AND-DATABASE.md), API design and unit tests |
| Authentication | Authentication | 4 | Registration and login tests pass for valid and invalid credentials | [Architecture and database design](ARCHITECTURE-AND-DATABASE.md), `AuthServiceTest`, `AuthControllerIntegrationTest` |
| Authorization | Access control | 4 | Customer access to administrator operations returns 403; ownership checks protect customer data | [Architecture and database design](ARCHITECTURE-AND-DATABASE.md), `AuthControllerIntegrationTest`, authorization tests |
| Inheritance | Domain model | 2, 4 | `User` hierarchy intentionally models `Customer` and `Administrator` and is tested | [Architecture and database design](ARCHITECTURE-AND-DATABASE.md), source, tests |
| Two exception types | Error handling | 4 | At least two domain/application exception types map to structured HTTP errors | Exception tests |
| Image carousel | Frontend UX | 3, 7 | Product detail view provides button-controlled, keyboard-accessible image states | [UX and visual design](UX-AND-VISUAL-DESIGN.md), `ProductVisual`, browser evidence pending |
| Purposeful animation | Frontend UX | 3, 7 | Loading pulse and carousel transitions communicate state and respect reduced motion | [UX and visual design](UX-AND-VISUAL-DESIGN.md), CSS, browser evidence pending |
| Two CSS files and 10 properties | Frontend styling | 7 | Organized `index.css` and `App.css` provide the responsive visual system | [UX and visual design](UX-AND-VISUAL-DESIGN.md), source inspection and build |
| Responsive design | Frontend styling | 3, 7 | Catalogue, details, cart, and auth layouts include mobile/tablet breakpoints | [UX and visual design](UX-AND-VISUAL-DESIGN.md), viewport evidence pending |
| Five unit tests | Testing | 9 | CI reports at least five meaningful business-rule unit tests | CI test report |
| Git versioning | Delivery | 0 | Repository has history with coherent commits | GitHub repository |
| GitHub Actions CI/CD | Delivery | 0, 9 | Push and pull request workflow installs, lints/tests, and builds successfully | Workflow run |
| User activity logging | Operations | 4, 8, 10 | Authentication and product administration actions are persisted and written to a structured application log without secrets | `AuditLogService`, log configuration and sample |
| Hosting | Deployment | 10 | Public URL serves the application | Deployment URL and smoke test |
| SEO | SEO | 10 | Lighthouse SEO score is at least 90/100 and required metadata is present | Lighthouse report |
| Stripe payments | Payments | 6 | Stripe test-mode Checkout Session and signed webhook update order status while recording only provider ID/status | `PaymentServiceTest`, Stripe test-mode demonstration |
| CSRF protection | Security I | 4, 8 | State-changing requests without valid CSRF protection are rejected | Security test |
| XSS protection | Security I | 4, 8 | User-controlled content is safely rendered and XSS payload tests do not execute | Security test |
| SQL injection protection | Security I | 5, 8 | Search/filter input cannot alter query semantics and sorting accepts only allow-listed fields | Parameterized repository query and catalogue service tests |
| Upload size limit | Security I | 5, 8 | Files over 12 MB are rejected before persistence | `ProductImageServiceTest`, upload test |
| Approved upload extensions/MIME | Security I | 5, 8 | Non-approved extensions or MIME types are rejected | `ProductImageServiceTest`, upload test |
| Rate limiting | Security II | 8 | More than 100 consecutive requests in under one minute are temporarily blocked | Rate-limit test and log |
| CAPTCHA | Security II | 8 | Abuse-prone flow requires a valid CAPTCHA result | Authentication/checkout test |
| Steganography risk reduction | Security II | 8 | Uploaded files undergo content decoding, metadata capture, and content hashing beyond filename validation | `ProductImageService`, upload security test |
| Security headers | Security II | 8, 10 | Security Headers Inspector reports grade A | Captured inspector report |
| `.htaccess` resource protection | Security II | 8, 10 | Private/configuration resources are inaccessible through the deployed web boundary | Deployment smoke test and configuration |

## Phase 1 exit-gate checklist

- [x] Actors are defined.
- [x] Four minimum scenarios have owners and measurable acceptance tests.
- [x] Functional and rubric requirements have an owner and planned phase.
- [x] Evidence types are identified for every rubric requirement.
- [ ] Phase 2 API, schema, and security design approval is complete.

The final item intentionally remains open. Feature implementation must not begin
against undocumented tables or endpoints.
