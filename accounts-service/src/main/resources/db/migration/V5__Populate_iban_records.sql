-- V5__Populate_iban_records.sql
-- Populate 1M IBAN records for risk lookup
-- This will take some time to execute

-- Insert 1,000,000 IBAN records in batches for better performance
DO $$
DECLARE
    batch_size INTEGER := 10000;
    total_records INTEGER := 1000000;
    current_batch INTEGER := 0;
    records_inserted INTEGER := 0;
BEGIN
    RAISE NOTICE 'Starting IBAN population...';
    
    WHILE records_inserted < total_records LOOP
        -- Insert batch
        INSERT INTO risk.iban_risk_lookup (iban, bank_code, account_number, risk_level, country_code)
        SELECT 
            generate_bg_iban() as iban,
            substr(iban, 5, 4) as bank_code,
            substr(iban, 9) as account_number,
            generate_risk_level() as risk_level,
            'BG' as country_code
        FROM generate_series(1, LEAST(batch_size, total_records - records_inserted));
        
        records_inserted := records_inserted + LEAST(batch_size, total_records - records_inserted);
        current_batch := current_batch + 1;
        
        -- Log progress every 100k records
        IF records_inserted % 100000 = 0 THEN
            RAISE NOTICE 'Inserted % records (%.1f%%)', records_inserted, (records_inserted::FLOAT / total_records * 100);
        END IF;
        
        -- Commit every batch
        COMMIT;
    END LOOP;
    
    RAISE NOTICE 'IBAN population completed! Total records: %', records_inserted;
END $$;

-- Note: Performance indexes are created in V7__Create_performance_indexes.sql
