ALTER TABLE session_token RENAME COLUMN token TO token_hash;

ALTER INDEX IF EXISTS idx_session_token_active RENAME TO idx_session_token_hash_active;
