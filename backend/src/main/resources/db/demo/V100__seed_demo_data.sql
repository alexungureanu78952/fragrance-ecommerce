-- Demo data for local presentation only. Applied once by Flyway after the schema
-- migrations (see spring.flyway.locations). Accounts match the README:
--   demo.customer@example.test / DemoCustomer!2026
--   demo.admin@example.test    / DemoAdmin!2026
-- Passwords are stored as BCrypt hashes (cost 10), never in plain text.

INSERT INTO users (id, email, password_hash, display_name, enabled, created_at, updated_at) VALUES
    ('10000000-0000-0000-0000-000000000001', 'demo.customer@example.test',
     '$2a$10$cIKnCxsQvabvTYxokKYBHerAQfpu6BUN6akiyZV6fXEtrI1unXosS', 'Demo Customer', TRUE, now(), now()),
    ('10000000-0000-0000-0000-000000000002', 'demo.admin@example.test',
     '$2a$10$yMKiOcgJMsZQBMLKvfcjJeFhG8KgJlT5uVm6JUZgDtXvt8Z3mKDju', 'Demo Administrator', TRUE, now(), now());

INSERT INTO customers (user_id) VALUES ('10000000-0000-0000-0000-000000000001');
INSERT INTO administrators (user_id) VALUES ('10000000-0000-0000-0000-000000000002');

INSERT INTO user_roles (user_id, role_id) VALUES
    ('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001'),
    ('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002');

INSERT INTO brands (id, name, description) VALUES
    ('20000000-0000-0000-0000-000000000001', 'Nocturne', 'House compositions built around evening light.'),
    ('20000000-0000-0000-0000-000000000002', 'Maison Lune', 'Soft, skin-close floral studies.'),
    ('20000000-0000-0000-0000-000000000003', 'Atelier Brume', 'Mineral and resin accords from small-batch distillation.');

INSERT INTO categories (id, name, description) VALUES
    ('30000000-0000-0000-0000-000000000001', 'Woody', 'Cedar, vetiver, and smoked woods.'),
    ('30000000-0000-0000-0000-000000000002', 'Floral', 'Iris, rose, and white florals.'),
    ('30000000-0000-0000-0000-000000000003', 'Amber', 'Warm resins, vanilla, and spice.');

INSERT INTO products (id, brand_id, category_id, name, description, price, stock_quantity, active, created_at, updated_at) VALUES
    ('40000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001',
     'Dusk Cedar', 'Smoked cedar and black pepper over a dry vetiver base.', 89.00, 12, TRUE, now(), now()),
    ('40000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000003',
     'Ember Hour', 'Labdanum and tonka warmed with a trace of cardamom.', 104.00, 8, TRUE, now(), now()),
    ('40000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002',
     'Pale Iris', 'Powdery orris root softened by musk and pear skin.', 96.00, 10, TRUE, now(), now()),
    ('40000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002',
     'Morning Rose', 'Dew-bright rose with green tea and white cedar.', 78.00, 15, TRUE, now(), now()),
    ('40000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000001',
     'Stone Vetiver', 'Wet stone, grapefruit peel, and Haitian vetiver.', 92.00, 6, TRUE, now(), now()),
    ('40000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000003',
     'Salt Amber', 'Ambergris accord with sea salt and benzoin.', 118.00, 3, TRUE, now(), now());
