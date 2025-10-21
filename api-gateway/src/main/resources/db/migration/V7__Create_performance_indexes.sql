-- V7__Create_performance_indexes.sql

-- This migration is intentionally empty as CREATE INDEX CONCURRENTLY and ANALYZE
-- are non-transactional operations that should be run manually after migration.
-- 
-- To create performance indexes manually, run:
-- CREATE INDEX CONCURRENTLY idx_iban_risk_lookup_composite ON risk.iban_risk_lookup(risk_level, country_code);
-- ANALYZE risk.iban_risk_lookup;
