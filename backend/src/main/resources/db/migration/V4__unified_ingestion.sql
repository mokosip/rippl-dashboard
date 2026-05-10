ALTER TABLE extension_tokens
    ADD COLUMN scope TEXT NOT NULL DEFAULT 'ingest';

CREATE TABLE activity_sessions (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID REFERENCES users(id) ON DELETE CASCADE NOT NULL,
    collector_type       TEXT NOT NULL,
    collector_session_id TEXT NOT NULL,
    collector            JSONB NOT NULL,
    source               JSONB NOT NULL,
    session              JSONB NOT NULL,
    privacy              JSONB NOT NULL,
    metrics              JSONB,
    context              JSONB,
    raw_payload          JSONB NOT NULL,
    started_at           BIGINT NOT NULL,
    ended_at             BIGINT NOT NULL,
    created_at           TIMESTAMPTZ DEFAULT now()
);

CREATE UNIQUE INDEX uq_activity_sessions_dedupe
    ON activity_sessions(user_id, collector_type, collector_session_id);

CREATE INDEX idx_activity_sessions_user_created
    ON activity_sessions(user_id, created_at DESC);

CREATE TABLE activity_feedback (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id    UUID REFERENCES activity_sessions(id) ON DELETE CASCADE NOT NULL,
    feedback_type TEXT NOT NULL,
    feedback_value JSONB NOT NULL,
    updated_at    TIMESTAMPTZ DEFAULT now(),
    UNIQUE(session_id, feedback_type)
);

CREATE INDEX idx_activity_feedback_session
    ON activity_feedback(session_id);
