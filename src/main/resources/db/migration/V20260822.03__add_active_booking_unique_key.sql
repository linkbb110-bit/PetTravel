ALTER TABLE appointment_order
    ADD UNIQUE KEY uk_order_user_route_active (user_id, route_id, active_booking_key);
