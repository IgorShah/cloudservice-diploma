CREATE TABLE app_user (
    id BIGSERIAL PRIMARY KEY,
    login VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL
);

CREATE TABLE session_token (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL,
    active BOOLEAN NOT NULL
);

CREATE INDEX idx_session_token_user_id ON session_token (user_id);
CREATE INDEX idx_session_token_active ON session_token (token, active);

CREATE TABLE stored_file (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL,
    size BIGINT NOT NULL,
    content_type VARCHAR(255),
    storage_path VARCHAR(512) NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_stored_file_user_filename UNIQUE (user_id, filename)
);

CREATE INDEX idx_stored_file_user_id ON stored_file (user_id);
CREATE INDEX idx_stored_file_user_filename ON stored_file (user_id, filename);
