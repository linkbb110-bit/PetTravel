-- This script is read-only. Resolve every returned row before Flyway applies V20260822.01-.07.

-- Active duplicate bookings would block creation of the unique generated-column index.
SELECT user_id, route_id, COUNT(*) AS active_count
FROM appointment_order
WHERE order_status IN (0, 1) AND is_deleted = 0
GROUP BY user_id, route_id
HAVING COUNT(*) > 1;

-- Invalid inventory must be corrected before adding CHECK constraints.
SELECT id, total_stock, available_stock
FROM camp_route
WHERE available_stock < 0 OR available_stock > total_stock;

-- Legacy plaintext-looking passwords require a migration decision. Production defaults
-- to rejecting them; temporarily enable LEGACY_PLAINTEXT_UPGRADE_ENABLED only if approved.
SELECT COUNT(*) AS non_bcrypt_password_count
FROM sys_user
WHERE password NOT LIKE '$2a$%'
  AND password NOT LIKE '$2b$%'
  AND password NOT LIKE '$2y$%'
  AND is_deleted = 0;

-- A non-zero result means part of this remediation may already have been applied
-- manually. Do not let Flyway run until its baseline/history matches the real schema.
SELECT 'active_booking_key' AS existing_remediation_object, COUNT(*) AS object_count
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'appointment_order'
  AND column_name = 'active_booking_key'
UNION ALL
SELECT 'uk_order_user_route_active', COUNT(*)
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'appointment_order'
  AND index_name = 'uk_order_user_route_active'
UNION ALL
SELECT 'inventory_compensation_task', COUNT(*)
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'inventory_compensation_task';

