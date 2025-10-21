-- Create additional indexes after population for better performance
-- These indexes are created CONCURRENTLY to avoid blocking the table during creation
CREATE INDEX CONCURRENTLY idx_iban_risk_lookup_composite ON risk.iban_risk_lookup(risk_level, country_code);

-- Update table statistics
ANALYZE risk.iban_risk_lookup;
