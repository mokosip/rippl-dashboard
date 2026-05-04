CREATE TABLE extension_tokens (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    collector_id  UUID REFERENCES collectors(id) ON DELETE CASCADE NOT NULL UNIQUE,
    user_id       UUID REFERENCES users(id) ON DELETE CASCADE NOT NULL,
    token_hash    TEXT NOT NULL UNIQUE,
    created_at    TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_extension_tokens_hash ON extension_tokens(token_hash);
