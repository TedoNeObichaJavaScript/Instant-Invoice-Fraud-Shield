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

-- Function to generate valid Bulgarian IBAN
CREATE OR REPLACE FUNCTION generate_bg_iban() RETURNS VARCHAR(34) AS $$
DECLARE
    bank_codes VARCHAR[] := ARRAY['BANK', 'BNBG', 'CITI', 'UNCR', 'UBBS', 'EXPR', 'FIBK', 'INVE', 'KBCB', 'LBBG', 'POST', 'SGBG', 'TBIB', 'VKZB'];
    bank_code VARCHAR(10);
    account_num VARCHAR(20);
    check_digits VARCHAR(2);
    iban VARCHAR(34);
    remainder INTEGER;
    mod_result INTEGER;
BEGIN
    -- Select random bank code
    bank_code := bank_codes[1 + floor(random() * array_length(bank_codes, 1))];
    
    -- Generate random 10-digit account number
    account_num := lpad(floor(random() * 10000000000)::TEXT, 10, '0');
    
    -- Create IBAN without check digits
    iban := 'BG00' || bank_code || account_num;
    
    -- Calculate check digits using MOD 97-10 algorithm
    -- Move first 4 characters to end
    iban := substr(iban, 5) || substr(iban, 1, 4);
    
    -- Replace letters with numbers (A=10, B=11, ..., Z=35)
    iban := replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(
        replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(
        replace(replace(replace(replace(replace(replace(iban,
        'A', '10'), 'B', '11'), 'C', '12'), 'D', '13'), 'E', '14'), 'F', '15'), 'G', '16'),
        'H', '17'), 'I', '18'), 'J', '19'), 'K', '20'), 'L', '21'), 'M', '22'), 'N', '23'),
        'O', '24'), 'P', '25'), 'Q', '26'), 'R', '27'), 'S', '28'), 'T', '29'), 'U', '30'),
        'V', '31'), 'W', '32'), 'X', '33'), 'Y', '34'), 'Z', '35');
    
    -- Calculate MOD 97
    remainder := mod(iban::BIGINT, 97);
    mod_result := 98 - remainder;
    
    -- Format check digits
    check_digits := lpad(mod_result::TEXT, 2, '0');
    
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
