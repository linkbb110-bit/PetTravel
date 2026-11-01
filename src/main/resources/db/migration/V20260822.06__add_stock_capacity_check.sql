ALTER TABLE camp_route
    ADD CONSTRAINT chk_route_stock_capacity CHECK (available_stock <= total_stock);
