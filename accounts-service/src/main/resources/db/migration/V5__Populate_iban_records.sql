-- V5__Populate_iban_records.sql
-- Populate 1K IBAN records for risk lookup using predictable pattern

-- Insert 1,000 IBAN records with predictable pattern
DO $$
DECLARE
    i INTEGER;
    bank_codes VARCHAR[] := ARRAY['BANK', 'BNBG', 'CITI', 'UNCR', 'UBBS'];
    bank_code VARCHAR(4);
    account_num VARCHAR(8);
    iban_value VARCHAR(22);
    risk_level VARCHAR(10);
BEGIN
    RAISE NOTICE 'Starting IBAN population...';
    
    -- Generate exactly 1000 IBANs
    FOR i IN 1..1000 LOOP
        -- Select bank code based on sequence (cycles through 5 banks)
        bank_code := bank_codes[1 + ((i - 1) % array_length(bank_codes, 1))];
        
        -- Generate account number based on sequence
        account_num := lpad(i::TEXT, 8, '0');
        
        -- Generate IBAN with predictable pattern
        iban_value := 'BG' || lpad(((i - 1) % 100)::TEXT, 2, '0') || bank_code || account_num;
        
        -- Assign risk level based on sequence (distributed pattern)
        risk_level := CASE (i % 4)
            WHEN 0 THEN 'LOW'
            WHEN 1 THEN 'MEDIUM' 
            WHEN 2 THEN 'HIGH'
            ELSE 'BLOCKED'
        END;
        
        -- Insert the record
        INSERT INTO risk.iban_risk_lookup (iban, bank_code, account_number, risk_level, country_code)
        VALUES (iban_value, bank_code, account_num, risk_level, 'BG');
        
        -- Log progress every 100 records
        IF i % 100 = 0 THEN
            RAISE NOTICE 'Inserted % records (%.1f%%)', i, (i::FLOAT / 1000 * 100);
        END IF;
    END LOOP;
    
    RAISE NOTICE 'IBAN population completed! Total records: 1000';
END $$;

-- Note: Performance indexes are created in V7__Create_performance_indexes.sql
