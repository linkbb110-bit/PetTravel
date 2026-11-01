ALTER TABLE appointment_order
    ADD KEY idx_order_timeout (order_status, is_deleted, create_time);
