-- Truncate dependent tables first (early stage, data not precious)
TRUNCATE TABLE estimated_sessions CASCADE;
TRUNCATE TABLE activity_feedback CASCADE;
TRUNCATE TABLE activity_sessions CASCADE;

-- Drop old JSONB blob columns
ALTER TABLE activity_sessions
    DROP COLUMN collector,
    DROP COLUMN source,
    DROP COLUMN session,
    DROP COLUMN privacy,
    DROP COLUMN metrics,
    DROP COLUMN context;

-- Drop old BIGINT time columns (will be re-added as TIMESTAMPTZ)
ALTER TABLE activity_sessions
    DROP COLUMN started_at,
    DROP COLUMN ended_at;

-- Add new typed columns
ALTER TABLE activity_sessions
    ADD COLUMN collector_version    TEXT,
    ADD COLUMN source_type          TEXT NOT NULL,
    ADD COLUMN source_version       TEXT,
    ADD COLUMN domain               TEXT NOT NULL,
    ADD COLUMN surface              TEXT NOT NULL,
    ADD COLUMN started_at           TIMESTAMPTZ NOT NULL,
    ADD COLUMN ended_at             TIMESTAMPTZ NOT NULL,
    ADD COLUMN duration_ms          BIGINT NOT NULL,
    ADD COLUMN active_ms            BIGINT NOT NULL,
    ADD COLUMN collector_metrics    JSONB DEFAULT '{}'::jsonb,
    ADD COLUMN collector_context    JSONB DEFAULT '{}'::jsonb;

-- Add CHECK constraints
ALTER TABLE activity_sessions
    ADD CONSTRAINT chk_time_range CHECK (ended_at >= started_at),
    ADD CONSTRAINT chk_duration CHECK (duration_ms >= 0),
    ADD CONSTRAINT chk_active CHECK (active_ms >= 0);

-- Drop old indexes (they reference old columns or will be recreated)
DROP INDEX IF EXISTS idx_activity_sessions_user_created;

-- Create new indexes
CREATE INDEX idx_activity_sessions_user_time
    ON activity_sessions(user_id, started_at DESC);

CREATE INDEX idx_activity_sessions_domain
    ON activity_sessions(user_id, domain);

CREATE INDEX idx_activity_sessions_collector
    ON activity_sessions(user_id, collector_type);
