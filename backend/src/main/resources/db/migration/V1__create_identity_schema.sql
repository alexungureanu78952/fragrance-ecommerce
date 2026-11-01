CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE customers (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE administrators (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE roles (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

INSERT INTO roles (id, code) VALUES
    ('00000000-0000-0000-0000-000000000001', 'CUSTOMER'),
    ('00000000-0000-0000-0000-000000000002', 'ADMINISTRATOR');

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(80) NOT NULL,
    resource_type VARCHAR(80) NOT NULL,
    resource_id VARCHAR(120),
    outcome VARCHAR(30) NOT NULL,
    request_id VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_actor_user_id ON audit_logs(actor_user_id);
