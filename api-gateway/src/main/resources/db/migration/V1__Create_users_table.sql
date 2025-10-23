-- V1__Create_users_table.sql
-- Users table for authentication

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_active ON users(is_active);

-- Insert default admin user (password: admin123)
-- Note: This uses a proper BCrypt hash generated with PostgreSQL's crypt() function
INSERT INTO users (username, email, password_hash) VALUES 
('admin', 'admin@microservices.com', '$2a$06$bHmzJLVFEcpwWVrenD1HUugThmlRAMeMEmtOiA7vuwPxRqkU5poNO');
