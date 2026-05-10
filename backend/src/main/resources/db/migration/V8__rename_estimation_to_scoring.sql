-- Rename table
ALTER TABLE estimated_sessions RENAME TO scored_sessions;

-- Rename enums
ALTER TYPE estimation_confidence RENAME TO scoring_confidence;
ALTER TYPE estimation_method RENAME TO scoring_method;

-- Rename columns
ALTER TABLE scored_sessions RENAME COLUMN estimation_method TO scoring_method;
ALTER TABLE scored_sessions RENAME COLUMN estimated_at TO scored_at;

-- Rename index
ALTER INDEX idx_estimated_sessions_user_confidence
    RENAME TO idx_scored_sessions_user_confidence;

-- Rename unique constraint
ALTER INDEX uq_estimated_sessions_activity_session
    RENAME TO uq_scored_sessions_activity_session;
