-- Create fraud analysis log table
CREATE TABLE IF NOT EXISTS fraud_analysis_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id VARCHAR(255) NOT NULL UNIQUE,
    invoice_id VARCHAR(50) NOT NULL,
    supplier_iban VARCHAR(34) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    supplier_name VARCHAR(100) NOT NULL,
    risk_status VARCHAR(20) NOT NULL CHECK (risk_status IN ('ALLOW', 'REVIEW', 'BLOCK')),
    risk_level VARCHAR(20) NOT NULL CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    anomalies TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_fraud_log_transaction_id ON fraud_analysis_log(transaction_id);
CREATE INDEX IF NOT EXISTS idx_fraud_log_invoice_id ON fraud_analysis_log(invoice_id);
CREATE INDEX IF NOT EXISTS idx_fraud_log_supplier_iban ON fraud_analysis_log(supplier_iban);
CREATE INDEX IF NOT EXISTS idx_fraud_log_risk_status ON fraud_analysis_log(risk_status);
CREATE INDEX IF NOT EXISTS idx_fraud_log_created_at ON fraud_analysis_log(created_at);

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_fraud_analysis_log_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to automatically update updated_at
CREATE TRIGGER trigger_update_fraud_analysis_log_updated_at
    BEFORE UPDATE ON fraud_analysis_log
    FOR EACH ROW
    EXECUTE FUNCTION update_fraud_analysis_log_updated_at();

-- Add comments for documentation
COMMENT ON TABLE fraud_analysis_log IS 'Logs all fraud detection analysis results';
COMMENT ON COLUMN fraud_analysis_log.transaction_id IS 'Unique identifier for the fraud analysis transaction';
COMMENT ON COLUMN fraud_analysis_log.invoice_id IS 'Invoice ID from the payment request';
COMMENT ON COLUMN fraud_analysis_log.supplier_iban IS 'Supplier IBAN being validated';
COMMENT ON COLUMN fraud_analysis_log.amount IS 'Payment amount being validated';
COMMENT ON COLUMN fraud_analysis_log.supplier_name IS 'Supplier name from the payment request';
COMMENT ON COLUMN fraud_analysis_log.risk_status IS 'Final risk decision: ALLOW, REVIEW, or BLOCK';
COMMENT ON COLUMN fraud_analysis_log.risk_level IS 'Risk level: LOW, MEDIUM, HIGH, or CRITICAL';
COMMENT ON COLUMN fraud_analysis_log.anomalies IS 'Semicolon-separated list of detected anomalies';
COMMENT ON COLUMN fraud_analysis_log.created_at IS 'Timestamp when the analysis was performed';
COMMENT ON COLUMN fraud_analysis_log.updated_at IS 'Timestamp when the record was last updated';
