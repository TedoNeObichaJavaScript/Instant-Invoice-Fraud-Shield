-- V4__Create_iban_risk_lookup_table.sql
-- IBAN risk lookup table with 1M records for payment risk assessment

CREATE TABLE risk.iban_risk_lookup (
    id BIGSERIAL PRIMARY KEY,
    iban VARCHAR(34) NOT NULL UNIQUE,
    bank_code VARCHAR(10) NOT NULL,
    account_number VARCHAR(20) NOT NULL,
    risk_level VARCHAR(10) NOT NULL CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'BLOCKED')),
    country_code VARCHAR(2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance (critical for <200ms requirement)
CREATE INDEX idx_iban_risk_lookup_iban ON risk.iban_risk_lookup(iban);
CREATE INDEX idx_iban_risk_lookup_bank_code ON risk.iban_risk_lookup(bank_code);
CREATE INDEX idx_iban_risk_lookup_risk_level ON risk.iban_risk_lookup(risk_level);
CREATE INDEX idx_iban_risk_lookup_country ON risk.iban_risk_lookup(country_code);

-- Create sequence for predictable IBAN generation
CREATE SEQUENCE iban_sequence START 1;

-- Function to generate predictable Bulgarian IBAN (for testing)
CREATE OR REPLACE FUNCTION generate_bg_iban() RETURNS VARCHAR(22) AS $$
DECLARE
    bank_codes VARCHAR[] := ARRAY['BANK', 'BNBG', 'CITI', 'UNCR', 'UBBS'];
    bank_code VARCHAR(4);
    account_num VARCHAR(8);
    check_digits VARCHAR(2);
    sequence_num INTEGER;
BEGIN
    -- Get the current sequence number from a simple counter
    -- This will be called 1000 times during migration
    sequence_num := nextval('iban_sequence');
    
    -- Select bank code based on sequence (cycles through banks)
    bank_code := bank_codes[1 + (sequence_num % array_length(bank_codes, 1))];
    
    -- Generate account number based on sequence
    account_num := lpad(sequence_num::TEXT, 8, '0');
    
    -- Generate check digits based on sequence
    check_digits := lpad((sequence_num % 100)::TEXT, 2, '0');
    
    -- Return final IBAN
    RETURN 'BG' || check_digits || bank_code || account_num;
END;
$$ LANGUAGE plpgsql;

-- Function to generate risk level
CREATE OR REPLACE FUNCTION generate_risk_level() RETURNS VARCHAR(10) AS $$
BEGIN
    RETURN CASE 
        WHEN random() < 0.7 THEN 'LOW'
        WHEN random() < 0.9 THEN 'MEDIUM'
        WHEN random() < 0.98 THEN 'HIGH'
        ELSE 'BLOCKED'
    END;
END;
$$ LANGUAGE plpgsql;
