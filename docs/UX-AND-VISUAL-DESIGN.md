# UX and visual design

This document is the Phase 3 design baseline. It defines the frontend routes,
navigation, responsive behavior, visual system, and interaction requirements before
frontend feature implementation begins.

## Product direction

The interface presents a calm, editorial fragrance shop: warm neutral surfaces,
high-contrast typography, restrained accent color, and generous product imagery.
The design should feel premium without imitating a specific commercial brand.

## Navigation map

```text
Catalogue (/)
├── Product details (/products/:productId)
│   └── Add to cart
├── Cart (/cart)
│   └── Checkout (/checkout)
│       ├── Payment pending (/checkout/pending)
│       ├── Payment success (/checkout/success)
│       └── Payment failure (/checkout/failure)
├── Login (/login)
├── Register (/register)
└── Account (/account/orders)

Administrator (/admin)
├── Products (/admin/products)
├── New product (/admin/products/new)
├── Edit product (/admin/products/:productId/edit)
└── Orders (/admin/orders)
```

The primary navigation contains Catalogue, search, cart, and the account action.
Administrator links appear only for an administrator and remain protected by the
backend regardless of frontend visibility.

## Screen specifications

| Route | Primary action | Success state | Loading state | Empty state | Error/permission state |
|---|---|---|---|---|---|
| `/` | Search or open a product | Paginated product grid | Skeleton cards | “No fragrances match” with clear filters action | Inline retry message |
| `/products/:productId` | Add product to cart | Product details and confirmation feedback | Image and detail skeleton | Not applicable | Product-not-found page or retry |
| `/cart` | Continue to checkout | Items, quantities, totals, checkout CTA | Cart skeleton | “Your cart is empty” with catalogue link | Stock conflict with corrective quantity |
| `/checkout` | Start test payment | Redirect/pending payment state | Order summary and disabled CTA | Not applicable | Validation, stock, duplicate, or payment error |
| `/checkout/success` | View order history | Payment status and order link | Payment verification pending | Not applicable | Explicit unresolved payment message |
| `/login` | Authenticate | Redirect to intended route | Disabled submit and progress text | Not applicable | Invalid credentials, locked/rate-limited state |
| `/register` | Create account | Redirect to catalogue/account | Disabled submit and progress text | Not applicable | Field-level validation and duplicate email |
| `/account/orders` | Open an order | Customer-owned order list/detail | Order list skeleton | “No orders yet” with catalogue link | Unauthorized redirect or retry |
| `/admin/products` | Create or edit product | Product table with confirmation | Table skeleton | “No products” with create CTA | 403 permission-denied page |
| `/admin/products/new` | Save product | Return to product list | Disabled form while saving | Not applicable | Field, upload, or server error |
| `/admin/products/:id/edit` | Update product | Updated product summary | Form skeleton | Not applicable | 404, 403, or retry state |
| `/admin/orders` | Update order status | Filterable order table | Table skeleton | “No orders” | 403 permission-denied page |

## Wireframe-level layouts

### Catalogue

```text
+----------------------------------------------------------+
| Logo | Catalogue | Search field | Cart | Account          |
+----------------------------------------------------------+
| Editorial heading                         Filter / Sort  |
| Short supporting description                              |
+----------------------------------------------------------+
| Product card | Product card | Product card | Product card |
| image       | image        | image        | image         |
| name / brand| name / brand | name / brand | name / brand  |
+----------------------------------------------------------+
| Pagination                                             |
+----------------------------------------------------------+
```

### Product details

```text
+-----------------------------+----------------------------+
| Image carousel              | Brand                       |
| [previous] image [next]     | Product name                |
| thumbnails / position       | Price / stock status        |
|                             | Description                 |
|                             | Quantity / Add to cart      |
+-----------------------------+----------------------------+
| Reviews and product information                            |
+----------------------------------------------------------+
```

### Cart and checkout

```text
+--------------------------------+-------------------------+
| Cart items, quantity controls   | Order summary           |
| remove action and stock notice  | subtotal / total       |
|                                | Continue to checkout   |
+--------------------------------+-------------------------+
```

Checkout keeps the order summary visible and makes the test-mode payment context
explicit. It never asks this application for card details.

### Administrator product form

```text
+----------------------------------------------------------+
| Admin navigation: Products | Orders                       |
+----------------------------------------------------------+
| Product name | brand | category | price | stock           |
| Description textarea                                      |
| Image dropzone / selected image list                      |
| Validation messages                       [Save] [Cancel] |
+----------------------------------------------------------+
```

## Responsive behavior

| Viewport | Layout behavior |
|---|---|
| Desktop, 1200px and above | Four-column catalogue, two-column details, persistent order summary |
| Tablet, 768–1199px | Two-column catalogue, stacked details when needed, condensed navigation |
| Mobile, below 768px | One-column catalogue, single-column forms, sticky bottom cart action, collapsed navigation |

Responsive requirements:

- Content remains usable at 320px width without horizontal scrolling.
- Touch targets are at least 44 by 44 CSS pixels.
- Images preserve aspect ratio and use responsive sizing.
- Tables on admin pages become stacked cards or a horizontally scrollable region
  with an accessible label.
- Focus order follows the visual reading order at every breakpoint.
- Motion is reduced when `prefers-reduced-motion: reduce` is enabled.

## Visual system

| Token | Value | Use |
|---|---|---|
| `--color-surface` | `#fbf8f3` | Page background |
| `--color-panel` | `#ffffff` | Cards and forms |
| `--color-ink` | `#1f1b18` | Primary text |
| `--color-muted` | `#6d625a` | Supporting text |
| `--color-accent` | `#8b5e3c` | Primary actions and links |
| `--color-accent-strong` | `#654127` | Hover and focus |
| `--color-border` | `#e5ddd4` | Dividers and inputs |
| `--space-unit` | `0.25rem` | Spacing scale base |
| `--radius-card` | `0.75rem` | Card and panel corners |

Typography uses a distinctive display face for headings and a highly legible
system/sans-serif face for controls and body copy. Color contrast must meet WCAG AA
for body text and interactive controls.

## Interaction requirements

### Product image carousel

- Shows one main image, accessible previous/next buttons, and selectable thumbnails.
- Buttons have visible labels and keyboard focus styles.
- The current image has an `aria-current` indicator.
- The carousel does not auto-advance; users control movement.
- Missing or failed images show an explicit placeholder with useful alt text.

### Purposeful animation

Product cards use a short opacity/translate transition when entering the grid to
help users understand content loading. The transition is disabled for users who
prefer reduced motion. Button feedback uses a brief color/state transition, not a
decorative infinite animation.

### Forms and feedback

- Every input has a visible label and an associated error message.
- Errors are announced through an appropriate live region without stealing focus
  unexpectedly.
- Success messages identify the completed action and remain visible long enough to
  be understood.
- Destructive actions require confirmation or an undo path.

## Phase 3 exit-gate checklist

- [x] Every planned screen has a route and primary action.
- [x] Success, loading, empty, error, and permission-denied states are specified.
- [x] Responsive behavior is defined for desktop, tablet, and mobile.
- [x] Typography, colors, spacing, focus states, and error presentation are defined.
- [x] Carousel behavior and purposeful animation are defined.
- [ ] UX review approval is recorded before frontend implementation.
