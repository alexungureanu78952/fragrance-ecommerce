-- Administrators are customers with extra rights (Administrator extends Customer), so
-- they can keep a cart and place orders. Numbered after the demo seed (V100) so that
-- existing databases apply it in order; later schema migrations continue from V102.

-- Every administrator gets a customer record and the CUSTOMER role.
INSERT INTO customers (user_id)
SELECT a.user_id FROM administrators a
WHERE NOT EXISTS (SELECT 1 FROM customers c WHERE c.user_id = a.user_id);

INSERT INTO user_roles (user_id, role_id)
SELECT a.user_id, r.id FROM administrators a JOIN roles r ON r.code = 'CUSTOMER'
WHERE NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = a.user_id AND ur.role_id = r.id);

-- The administrators table now specialises customers rather than users.
ALTER TABLE administrators DROP CONSTRAINT administrators_user_id_fkey;
ALTER TABLE administrators ADD CONSTRAINT administrators_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES customers(user_id) ON DELETE CASCADE;
