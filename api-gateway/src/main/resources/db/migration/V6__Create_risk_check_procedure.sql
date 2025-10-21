-- V6__Create_risk_check_procedure.sql
-- Stored procedure for IBAN risk assessment
-- Optimized for <200ms response time requirement

CREATE OR REPLACE FUNCTION risk.check_iban_risk(
    p_iban VARCHAR(34)
) RETURNS TABLE(
    risk_level VARCHAR(10),
    decision VARCHAR(10),
    response_time_ms INTEGER
) AS $$
DECLARE
    start_time TIMESTAMP;
    end_time TIMESTAMP;
    risk_result VARCHAR(10);
    decision_result VARCHAR(10);
    response_time INTEGER;
BEGIN
    start_time := clock_timestamp();
    
    -- Look up IBAN risk level
    SELECT rl.risk_level INTO risk_result
    FROM risk.iban_risk_lookup rl
    WHERE rl.iban = p_iban
    LIMIT 1;
    
    -- Determine decision based on risk level
    decision_result := CASE 
        WHEN risk_result IS NULL THEN 'REVIEW'  -- Unknown IBAN
        WHEN risk_result = 'LOW' THEN 'ALLOW'
        WHEN risk_result = 'MEDIUM' THEN 'REVIEW'
        WHEN risk_result = 'HIGH' THEN 'REVIEW'
        WHEN risk_result = 'BLOCKED' THEN 'BLOCK'
        ELSE 'REVIEW'
    END;
    
    end_time := clock_timestamp();
    response_time := EXTRACT(EPOCH FROM (end_time - start_time)) * 1000;
    
    -- Return result
    RETURN QUERY SELECT 
        COALESCE(risk_result, 'UNKNOWN')::VARCHAR(10),
        decision_result::VARCHAR(10),
        response_time::INTEGER;
END;
$$ LANGUAGE plpgsql;

-- Create function for batch risk checks (for performance testing)
CREATE OR REPLACE FUNCTION risk.batch_risk_check(
    p_ibans TEXT[]
) RETURNS TABLE(
    iban VARCHAR(34),
    risk_level VARCHAR(10),
    decision VARCHAR(10)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        rl.iban,
        COALESCE(rl.risk_level, 'UNKNOWN')::VARCHAR(10),
        CASE 
            WHEN rl.risk_level IS NULL THEN 'REVIEW'
            WHEN rl.risk_level = 'LOW' THEN 'ALLOW'
            WHEN rl.risk_level = 'MEDIUM' THEN 'REVIEW'
            WHEN rl.risk_level = 'HIGH' THEN 'REVIEW'
            WHEN rl.risk_level = 'BLOCKED' THEN 'BLOCK'
            ELSE 'REVIEW'
        END::VARCHAR(10)
    FROM unnest(p_ibans) AS input_iban
    LEFT JOIN risk.iban_risk_lookup rl ON rl.iban = input_iban;
END;
$$ LANGUAGE plpgsql;
