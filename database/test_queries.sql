-- Test script to verify IBAN generation and risk lookup
-- Run this after migrations to test the functionality

-- Test IBAN generation
SELECT 'Testing IBAN generation:' as test_type;
SELECT generate_bg_iban() as sample_iban;

-- Test risk level generation
SELECT 'Testing risk level generation:' as test_type;
SELECT generate_risk_level() as sample_risk_level;

-- Test risk check function
SELECT 'Testing risk check function:' as test_type;
SELECT * FROM risk.check_iban_risk('BG11BANK99991234567890');

-- Check table statistics
SELECT 'Table statistics:' as test_type;
SELECT 
    schemaname,
    tablename,
    n_tup_ins as total_rows,
    n_tup_upd as updates,
    n_tup_del as deletes
FROM pg_stat_user_tables 
WHERE schemaname IN ('risk', 'audit', 'public')
ORDER BY schemaname, tablename;

-- Sample risk lookup
SELECT 'Sample risk lookups:' as test_type;
SELECT iban, risk_level, country_code 
FROM risk.iban_risk_lookup 
LIMIT 10;
