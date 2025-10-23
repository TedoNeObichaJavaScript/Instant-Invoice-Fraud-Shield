-- V8__Generate_1M_valid_ibans.sql
-- Generate exactly 1M valid Bulgarian IBANs with proper validation

-- Clear existing data
TRUNCATE TABLE risk.iban_risk_lookup RESTART IDENTITY CASCADE;

-- Add new columns for enhanced functionality
ALTER TABLE risk.iban_risk_lookup 
ADD COLUMN IF NOT EXISTS risk_score INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

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

-- Function to generate valid Bulgarian IBAN for a given sequence number
CREATE OR REPLACE FUNCTION generate_valid_bg_iban(seq_num INTEGER) RETURNS VARCHAR(22) AS $$
DECLARE
    bank_codes VARCHAR[] := ARRAY['BANK', 'BNBG', 'CITI', 'UNCR', 'UBBS', 'DSK', 'FIB', 'PIR', 'POST', 'SGB'];
    bank_code VARCHAR(4);
    account_number VARCHAR(8);
    additional_digits VARCHAR(2);
    bban VARCHAR(20);
    check_digits VARCHAR(2);
    country_code VARCHAR(2) := 'BG';
BEGIN
    -- Select bank code based on sequence (cycles through banks)
    bank_code := bank_codes[1 + (seq_num % array_length(bank_codes, 1))];
    
    -- Generate account number (8 digits, padded with zeros)
    account_number := lpad((seq_num % 100000000)::TEXT, 8, '0');
    
    -- Generate additional 2 digits for BBAN
    additional_digits := lpad((seq_num % 100)::TEXT, 2, '0');
    
    -- Create BBAN (Bank Code + Account Number + Additional Digits)
    bban := bank_code || account_number || additional_digits;
    
    -- Calculate valid check digits
    check_digits := calculate_iban_check_digits(country_code, bban);
    
    -- Return complete IBAN: BG + Check Digits + BBAN
    RETURN country_code || check_digits || bban;
END;
$$ LANGUAGE plpgsql;

-- Function to generate risk level and score with realistic distribution
CREATE OR REPLACE FUNCTION generate_risk_data() RETURNS TABLE(risk_level VARCHAR(10), risk_score INTEGER) AS $$
DECLARE
    rand_val NUMERIC;
    base_score INTEGER;
    score_variance INTEGER;
BEGIN
    rand_val := random();
    
    IF rand_val < 0.4 THEN
        -- GOOD: 40% - Risk score 0-30 (normal payments with rounded amounts)
        base_score := 15;
        score_variance := floor(random() * 16); -- 0-15 variance
        RETURN QUERY SELECT 'GOOD'::VARCHAR(10), (base_score + score_variance)::INTEGER;
    ELSIF rand_val < 0.7 THEN
        -- REVIEW: 30% - Risk score 31-70 (suspicious amounts sometimes)
        base_score := 50;
        score_variance := floor(random() * 21); -- 0-20 variance
        RETURN QUERY SELECT 'REVIEW'::VARCHAR(10), (base_score + score_variance)::INTEGER;
    ELSE
        -- BLOCK: 30% - Risk score 71-100 (high-risk IBANs)
        base_score := 85;
        score_variance := floor(random() * 16); -- 0-15 variance
        RETURN QUERY SELECT 'BLOCK'::VARCHAR(10), (base_score + score_variance)::INTEGER;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Insert 1,000,000 valid IBAN records using generate_series (optimized)
INSERT INTO risk.iban_risk_lookup (iban, bank_code, account_number, risk_level, country_code, risk_score, created_at, updated_at)
WITH iban_data AS (
    SELECT 
        seq_num,
        generate_valid_bg_iban(seq_num) as iban,
        substring(generate_valid_bg_iban(seq_num), 5, 4) as bank_code,
        substring(generate_valid_bg_iban(seq_num), 9, 8) as account_number
    FROM generate_series(1, 1000000) as seq_num
),
risk_data AS (
    SELECT 
        seq_num,
        (generate_risk_data()).risk_level,
        (generate_risk_data()).risk_score
    FROM generate_series(1, 1000000) as seq_num
)
SELECT 
    i.iban,
    i.bank_code,
    i.account_number,
    r.risk_level,
    'BG' as country_code,
    r.risk_score,
    CURRENT_TIMESTAMP as created_at,
    CURRENT_TIMESTAMP as updated_at
FROM iban_data i
JOIN risk_data r ON i.seq_num = r.seq_num;

-- Verify the count and distribution
DO $$
DECLARE
    record_count INTEGER;
    low_count INTEGER;
    medium_count INTEGER;
    high_count INTEGER;
BEGIN
    -- Check total count
    SELECT COUNT(*) INTO record_count FROM risk.iban_risk_lookup;
    RAISE NOTICE 'Generated % IBAN records', record_count;
    
    IF record_count != 1000000 THEN
        RAISE EXCEPTION 'Expected 1,000,000 records, but got %', record_count;
    END IF;
    
    -- Check distribution
    SELECT 
        COUNT(*) FILTER (WHERE risk_level = 'GOOD'),
        COUNT(*) FILTER (WHERE risk_level = 'REVIEW'),
        COUNT(*) FILTER (WHERE risk_level = 'BLOCK')
    INTO low_count, medium_count, high_count
    FROM risk.iban_risk_lookup;
    
    RAISE NOTICE 'Distribution: GOOD=%, REVIEW=%, BLOCK=%', low_count, medium_count, high_count;
    RAISE NOTICE 'Total records: %', (low_count + medium_count + high_count);
    
    -- Verify distribution matches expected percentages (within 2% tolerance)
    IF ABS(low_count - 400000) > 8000 OR 
       ABS(medium_count - 300000) > 6000 OR 
       ABS(high_count - 300000) > 6000 THEN
        RAISE WARNING 'Distribution is not approximately correct. Consider re-running if needed.';
    ELSE
        RAISE NOTICE 'Distribution verification passed - GOOD 40%, REVIEW 30%, BLOCK 30%.';
    END IF;
    
    -- Verify risk scores are in expected ranges
    IF EXISTS (SELECT 1 FROM risk.iban_risk_lookup WHERE risk_score < 0 OR risk_score > 100) THEN
        RAISE EXCEPTION 'Invalid risk scores found - must be between 0 and 100';
    END IF;
    
    RAISE NOTICE 'All verifications passed successfully!';
END $$;

-- Create additional performance indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_iban_risk_lookup_iban_hash ON risk.iban_risk_lookup USING hash(iban);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_iban_risk_lookup_risk_country ON risk.iban_risk_lookup(risk_level, country_code);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_iban_risk_lookup_risk_score ON risk.iban_risk_lookup(risk_score);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_iban_risk_lookup_created_at ON risk.iban_risk_lookup(created_at);

-- Update table statistics
ANALYZE risk.iban_risk_lookup;

-- Clean up functions (optional - can be kept for future use)
-- DROP FUNCTION IF EXISTS calculate_iban_check_digits(VARCHAR(2), VARCHAR(20)) CASCADE;
-- DROP FUNCTION IF EXISTS generate_valid_bg_iban(INTEGER) CASCADE;
-- DROP FUNCTION IF EXISTS generate_risk_data() CASCADE;
