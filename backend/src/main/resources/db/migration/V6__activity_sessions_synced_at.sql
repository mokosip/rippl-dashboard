ALTER TABLE activity_sessions
    ADD COLUMN synced_at TIMESTAMPTZ NOT NULL DEFAULT now();
