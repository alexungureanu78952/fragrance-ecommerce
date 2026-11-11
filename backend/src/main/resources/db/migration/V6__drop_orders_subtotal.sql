-- orders.subtotal was always set to the same value as orders.total (no tax,
-- discount, or shipping columns exist to explain a difference), and was never
-- read by the application. Dropping it removes a redundant column that
-- violates 3NF without a valid reason.
--
-- order_items.line_total is intentionally stored (unit_price_snapshot * quantity)
-- rather than recomputed: financial records must be immutable at the row level
-- to guard against rounding-mode changes and future price/tax logic.

ALTER TABLE orders DROP COLUMN subtotal;
