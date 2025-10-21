-- V2__Create_jwt_tokens_table.sql
-- JWT tokens table for stateful authentication

CREATE TABLE jwt_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_revoked BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP NULL
);

-- Indexes for performance
CREATE INDEX idx_jwt_tokens_user_id ON jwt_tokens(user_id);
CREATE INDEX idx_jwt_tokens_hash ON jwt_tokens(token_hash);
CREATE INDEX idx_jwt_tokens_expires ON jwt_tokens(expires_at);
CREATE INDEX idx_jwt_tokens_active ON jwt_tokens(is_revoked, expires_at);
