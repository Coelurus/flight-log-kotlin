-- Flyway baseline placeholder.
--
-- The schema for V1 was created by Hibernate's ddl-auto=update mode in
-- early development. Future schema changes should live in V2__..., V3__...
-- files in this directory and use plain SQL DDL (no Hibernate-specific syntax).
--
-- Useful indexes added on top of the JPA-generated schema:
CREATE INDEX IF NOT EXISTS idx_flights_takeoff_time ON flights (takeoff_time);
CREATE INDEX IF NOT EXISTS idx_flights_landing_time ON flights (landing_time);
