ALTER TABLE camp_route
    ADD CONSTRAINT chk_route_stock_non_negative CHECK (available_stock >= 0);
