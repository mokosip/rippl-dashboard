CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       TEXT UNIQUE NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE auth_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID REFERENCES users(id) ON DELETE CASCADE,
    token_hash  TEXT NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ
);

CREATE TABLE collectors (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID REFERENCES users(id) ON DELETE CASCADE,
    type        TEXT NOT NULL,
    enabled     BOOLEAN DEFAULT true,
    linked_at   TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE sessions (
    id                        TEXT PRIMARY KEY,
    user_id                   UUID REFERENCES users(id) ON DELETE CASCADE NOT NULL,
    domain                    TEXT NOT NULL,
    started_at                BIGINT NOT NULL,
    ended_at                  BIGINT NOT NULL,
    active_seconds            INT NOT NULL,
    date                      DATE NOT NULL,
    activity_type             TEXT,
    estimated_without_minutes INT,
    time_saved_minutes        INT,
    logged                    BOOLEAN DEFAULT false,
    synced_at                 TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_sessions_user_date ON sessions(user_id, date);
CREATE INDEX idx_sessions_user_domain ON sessions(user_id, domain);
