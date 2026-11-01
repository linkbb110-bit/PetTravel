ALTER TABLE appointment_order
    ADD COLUMN active_booking_key TINYINT
        GENERATED ALWAYS AS (
            CASE WHEN order_status IN (0, 1) AND is_deleted = 0 THEN 1 ELSE NULL END
        ) STORED;
