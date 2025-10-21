-- V5__Fix_iban_generation_for_1M_records.sql
-- Fix IBAN generation to create exactly 1M valid Bulgarian IBANs with proper validation

-- Drop the old sequence and function
DROP SEQUENCE IF EXISTS iban_sequence CASCADE;
DROP FUNCTION IF EXISTS generate_bg_iban() CASCADE;
DROP FUNCTION IF EXISTS generate_risk_level() CASCADE;

-- Clear existing data
TRUNCATE TABLE risk.iban_risk_lookup RESTART IDENTITY CASCADE;

-- Create new sequence for 1M records
CREATE SEQUENCE iban_sequence START 1 MAXVALUE 1000000;

-- Function to calculate IBAN check digits (mod 97-10 algorithm)
CREATE OR REPLACE FUNCTION calculate_iban_check_digits(country_code VARCHAR(2), bban VARCHAR(20)) 
RETURNS VARCHAR(2) AS $$
DECLARE
    rearranged VARCHAR(50);
    numeric_string VARCHAR(50);
    remainder INTEGER;
    check_digits INTEGER;
    i INTEGER;
    char_code INTEGER;
BEGIN
    -- Rearrange: BBAN + Country Code + "00"
    rearranged := bban || country_code || '00';
    
    -- Convert letters to numbers (A=10, B=11, ..., Z=35)
    numeric_string := '';
    FOR i IN 1..length(rearranged) LOOP
        IF substring(rearranged, i, 1) ~ '[A-Z]' THEN
            char_code := ascii(substring(rearranged, i, 1)) - ascii('A') + 10;
            numeric_string := numeric_string || char_code::TEXT;
        ELSE
            numeric_string := numeric_string || substring(rearranged, i, 1);
        END IF;
    END LOOP;
    
    -- Calculate mod 97
    remainder := 0;
    FOR i IN 1..length(numeric_string) LOOP
        remainder := (remainder * 10 + substring(numeric_string, i, 1)::INTEGER) % 97;
    END LOOP;
    
    -- Check digits = 98 - remainder
    check_digits := 98 - remainder;
    
    RETURN lpad(check_digits::TEXT, 2, '0');
END;
$$ LANGUAGE plpgsql;

-- Function to generate valid Bulgarian IBAN
CREATE OR REPLACE FUNCTION generate_valid_bg_iban() RETURNS VARCHAR(22) AS $$
DECLARE
    bank_codes VARCHAR[] := ARRAY['BANK', 'BNBG', 'CITI', 'UNCR', 'UBBS', 'DSK', 'FIB', 'PIR', 'POST', 'SGB'];
    bank_code VARCHAR(4);
    account_number VARCHAR(8);
    additional_digits VARCHAR(2);
    bban VARCHAR(20);
    check_digits VARCHAR(2);
    sequence_num INTEGER;
    country_code VARCHAR(2) := 'BG';
BEGIN
    -- Get sequence number (1 to 1,000,000)
    sequence_num := nextval('iban_sequence');
    
    -- Select bank code based on sequence (cycles through banks)
    bank_code := bank_codes[1 + (sequence_num % array_length(bank_codes, 1))];
    
    -- Generate account number (8 digits, padded with zeros)
    account_number := lpad((sequence_num % 100000000)::TEXT, 8, '0');
    
    -- Generate additional 2 digits for BBAN
    additional_digits := lpad((sequence_num % 100)::TEXT, 2, '0');
    
    -- Create BBAN (Bank Code + Account Number + Additional Digits)
    bban := bank_code || account_number || additional_digits;
    
    -- Calculate valid check digits
    check_digits := calculate_iban_check_digits(country_code, bban);
    
    -- Return complete IBAN: BG + Check Digits + BBAN
    RETURN country_code || check_digits || bban;
END;
$$ LANGUAGE plpgsql;

-- Function to generate risk level with weighted distribution
CREATE OR REPLACE FUNCTION generate_risk_level() RETURNS VARCHAR(10) AS $$
DECLARE
    rand_val NUMERIC;
BEGIN
    rand_val := random();
    RETURN CASE 
        WHEN rand_val < 0.60 THEN 'LOW'      -- 60% low risk
        WHEN rand_val < 0.85 THEN 'MEDIUM'   -- 25% medium risk  
        WHEN rand_val < 0.95 THEN 'HIGH'     -- 10% high risk
        ELSE 'BLOCKED'                        -- 5% blocked
    END;
END;
$$ LANGUAGE plpgsql;

-- Insert 1,000,000 valid IBAN records
INSERT INTO risk.iban_risk_lookup (iban, bank_code, account_number, risk_level, country_code)
SELECT 
    generate_valid_bg_iban() as iban,
    substring(generate_valid_bg_iban(), 5, 4) as bank_code,
    substring(generate_valid_bg_iban(), 9, 8) as account_number,
    generate_risk_level() as risk_level,
    'BG' as country_code
FROM generate_series(1, 1000000);

-- Verify the count
DO $$
DECLARE
    record_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO record_count FROM risk.iban_risk_lookup;
    RAISE NOTICE 'Generated % IBAN records', record_count;
    
    IF record_count != 1000000 THEN
        RAISE EXCEPTION 'Expected 1,000,000 records, but got %', record_count;
    END IF;
END $$;

-- Create additional performance indexes
CREATE INDEX CONCURRENTLY idx_iban_risk_lookup_iban_hash ON risk.iban_risk_lookup USING hash(iban);
CREATE INDEX CONCURRENTLY idx_iban_risk_lookup_risk_country ON risk.iban_risk_lookup(risk_level, country_code);

-- Update table statistics
ANALYZE risk.iban_risk_lookup;

-- Clean up functions (optional - can be kept for future use)
-- DROP FUNCTION IF EXISTS calculate_iban_check_digits(VARCHAR(2), VARCHAR(20)) CASCADE;
-- DROP FUNCTION IF EXISTS generate_valid_bg_iban() CASCADE;
-- DROP FUNCTION IF EXISTS generate_risk_level() CASCADE;
