CREATE TYPE estimation_confidence AS ENUM ('low', 'medium', 'high');

CREATE TYPE estimation_method AS ENUM (
    'profile_default',
    'feedback_adjusted',
    'collector_signal_adjusted',
    'global_fallback'
);

CREATE TABLE estimated_sessions (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    activity_session_id     UUID REFERENCES activity_sessions(id) ON DELETE CASCADE NOT NULL,
    user_id                 UUID NOT NULL,
    inferred_task_mix       JSONB NOT NULL,
    effective_multiplier    DOUBLE PRECISION NOT NULL,
    estimated_time_saved_ms BIGINT NOT NULL,
    confidence              estimation_confidence NOT NULL,
    estimation_method       estimation_method NOT NULL,
    estimated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_estimated_sessions_activity_session UNIQUE (activity_session_id)
);

CREATE INDEX idx_estimated_sessions_user_confidence
    ON estimated_sessions(user_id, confidence);
