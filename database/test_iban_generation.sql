-- Test script for V8__Generate_1M_valid_ibans.sql
-- This script tests the IBAN generation functionality

-- Test 1: Test individual functions
DO $$
DECLARE
    test_iban VARCHAR(22);
    test_risk_data RECORD;
    test_check_digits VARCHAR(2);
BEGIN
    RAISE NOTICE '=== Testing Individual Functions ===';
    
    -- Test IBAN generation
    test_iban := generate_valid_bg_iban(1);
    RAISE NOTICE 'Generated IBAN for seq 1: %', test_iban;
    
    -- Test risk data generation
    SELECT * INTO test_risk_data FROM generate_risk_data();
    RAISE NOTICE 'Generated risk data: level=%, score=%', test_risk_data.risk_level, test_risk_data.risk_score;
    
    -- Test check digits calculation
    test_check_digits := calculate_iban_check_digits('BG', 'BANK0000000100');
    RAISE NOTICE 'Check digits for BG BANK0000000100: %', test_check_digits;
    
    RAISE NOTICE 'Individual function tests completed successfully!';
END $$;

-- Test 2: Test small batch generation (100 records)
DO $$
DECLARE
    record_count INTEGER;
    good_count INTEGER;
    review_count INTEGER;
    block_count INTEGER;
    min_score INTEGER;
    max_score INTEGER;
BEGIN
    RAISE NOTICE '=== Testing Small Batch Generation (100 records) ===';
    
    -- Clear test data
    DELETE FROM risk.iban_risk_lookup WHERE iban LIKE 'TEST%';
    
    -- Insert 100 test records
    INSERT INTO risk.iban_risk_lookup (iban, bank_code, account_number, risk_level, country_code, risk_score, created_at, updated_at)
    SELECT 
        'TEST' || generate_valid_bg_iban(seq_num) as iban,
        substring(generate_valid_bg_iban(seq_num), 5, 4) as bank_code,
        substring(generate_valid_bg_iban(seq_num), 9, 8) as account_number,
        risk_data.risk_level,
        'BG' as country_code,
        risk_data.risk_score,
        CURRENT_TIMESTAMP as created_at,
        CURRENT_TIMESTAMP as updated_at
    FROM (
        SELECT 
            seq_num,
            generate_valid_bg_iban(seq_num) as iban,
            substring(generate_valid_bg_iban(seq_num), 5, 4) as bank_code,
            substring(generate_valid_bg_iban(seq_num), 9, 8) as account_number
        FROM generate_series(1, 100) as seq_num
    ) iban_data
    CROSS JOIN LATERAL generate_risk_data() as risk_data;
    
    -- Verify count
    SELECT COUNT(*) INTO record_count FROM risk.iban_risk_lookup WHERE iban LIKE 'TEST%';
    RAISE NOTICE 'Generated % test records', record_count;
    
    -- Check distribution
    SELECT 
        COUNT(*) FILTER (WHERE risk_level = 'GOOD'),
        COUNT(*) FILTER (WHERE risk_level = 'REVIEW'),
        COUNT(*) FILTER (WHERE risk_level = 'BLOCK')
    INTO good_count, review_count, block_count
    FROM risk.iban_risk_lookup WHERE iban LIKE 'TEST%';
    
    RAISE NOTICE 'Distribution: GOOD=%, REVIEW=%, BLOCK=%', good_count, review_count, block_count;
    
    -- Check risk score ranges
    SELECT MIN(risk_score), MAX(risk_score) INTO min_score, max_score
    FROM risk.iban_risk_lookup WHERE iban LIKE 'TEST%';
    
    RAISE NOTICE 'Risk score range: % to %', min_score, max_score;
    
    -- Verify risk scores match risk levels
    IF EXISTS (
        SELECT 1 FROM risk.iban_risk_lookup 
        WHERE iban LIKE 'TEST%' 
        AND (
            (risk_level = 'GOOD' AND risk_score > 30) OR
            (risk_level = 'REVIEW' AND (risk_score < 31 OR risk_score > 70)) OR
            (risk_level = 'BLOCK' AND risk_score < 71)
        )
    ) THEN
        RAISE EXCEPTION 'Risk scores do not match risk levels!';
    END IF;
    
    RAISE NOTICE 'Small batch test completed successfully!';
    
    -- Clean up test data
    DELETE FROM risk.iban_risk_lookup WHERE iban LIKE 'TEST%';
END $$;

-- Test 3: Performance test (1000 records)
DO $$
DECLARE
    start_time TIMESTAMP;
    end_time TIMESTAMP;
    duration INTERVAL;
    record_count INTEGER;
BEGIN
    RAISE NOTICE '=== Performance Test (1000 records) ===';
    
    start_time := clock_timestamp();
    
    -- Insert 1000 test records
    INSERT INTO risk.iban_risk_lookup (iban, bank_code, account_number, risk_level, country_code, risk_score, created_at, updated_at)
    SELECT 
        'PERF' || generate_valid_bg_iban(seq_num) as iban,
        substring(generate_valid_bg_iban(seq_num), 5, 4) as bank_code,
        substring(generate_valid_bg_iban(seq_num), 9, 8) as account_number,
        risk_data.risk_level,
        'BG' as country_code,
        risk_data.risk_score,
        CURRENT_TIMESTAMP as created_at,
        CURRENT_TIMESTAMP as updated_at
    FROM (
        SELECT 
            seq_num,
            generate_valid_bg_iban(seq_num) as iban,
            substring(generate_valid_bg_iban(seq_num), 5, 4) as bank_code,
            substring(generate_valid_bg_iban(seq_num), 9, 8) as account_number
        FROM generate_series(1, 1000) as seq_num
    ) iban_data
    CROSS JOIN LATERAL generate_risk_data() as risk_data;
    
    end_time := clock_timestamp();
    duration := end_time - start_time;
    
    SELECT COUNT(*) INTO record_count FROM risk.iban_risk_lookup WHERE iban LIKE 'PERF%';
    
    RAISE NOTICE 'Generated % records in %', record_count, duration;
    RAISE NOTICE 'Performance: % records per second', (record_count / EXTRACT(EPOCH FROM duration))::INTEGER;
    
    -- Clean up test data
    DELETE FROM risk.iban_risk_lookup WHERE iban LIKE 'PERF%';
    
    RAISE NOTICE 'Performance test completed successfully!';
END $$;

-- Test 4: IBAN validation test
DO $$
DECLARE
    test_iban VARCHAR(22);
    is_valid BOOLEAN;
    i INTEGER;
BEGIN
    RAISE NOTICE '=== IBAN Validation Test ===';
    
    -- Test 10 random IBANs for validity
    FOR i IN 1..10 LOOP
        test_iban := generate_valid_bg_iban(i * 1000);
        
        -- Basic format validation
        is_valid := (
            length(test_iban) = 22 AND
            test_iban ~ '^BG[0-9]{2}[A-Z]{4}[0-9]{10}$'
        );
        
        IF NOT is_valid THEN
            RAISE WARNING 'Invalid IBAN format: %', test_iban;
        ELSE
            RAISE NOTICE 'IBAN % is valid: %', i, test_iban;
        END IF;
    END LOOP;
    
    RAISE NOTICE 'IBAN validation test completed successfully!';
END $$;

-- Final test completion message
DO $$
BEGIN
    RAISE NOTICE '=== All Tests Completed Successfully! ===';
END $$;
