CREATE TABLE user_profiles (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                     UUID REFERENCES users(id) ON DELETE CASCADE NOT NULL UNIQUE,
    task_mix                    JSONB NOT NULL DEFAULT '{"writing":0.15,"coding":0.15,"research":0.2,"planning":0.15,"communication":0.2,"other":0.15}'::jsonb,
    personal_adjustment_factor  DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_adjustment_factor CHECK (personal_adjustment_factor >= 0.1 AND personal_adjustment_factor <= 3.0)
);

CREATE INDEX idx_user_profiles_user_id ON user_profiles(user_id);
