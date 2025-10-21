-- V3__Create_audit_logs_table.sql
-- Audit logs table for tracking API Gateway activities

CREATE TABLE audit.audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    endpoint VARCHAR(255) NOT NULL,
    method VARCHAR(10) NOT NULL,
    ip_address INET,
    user_agent TEXT,
    request_body TEXT,
    response_status INTEGER,
    response_time_ms INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_audit_logs_user_id ON audit.audit_logs(user_id);
CREATE INDEX idx_audit_logs_endpoint ON audit.audit_logs(endpoint);
CREATE INDEX idx_audit_logs_created_at ON audit.audit_logs(created_at);
CREATE INDEX idx_audit_logs_status ON audit.audit_logs(response_status);
