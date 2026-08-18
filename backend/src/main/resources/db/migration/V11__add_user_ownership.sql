CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    identity_subject VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(320) NOT NULL,
    status VARCHAR(16) NOT NULL,
    time_zone VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    disabled_at TIMESTAMPTZ,
    CONSTRAINT chk_app_users_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

DELETE FROM application_settings;
ALTER TABLE application_settings DROP CONSTRAINT application_settings_pkey;
ALTER TABLE application_settings DROP CONSTRAINT chk_application_settings_singleton;
ALTER TABLE application_settings DROP COLUMN id;
ALTER TABLE application_settings ADD COLUMN user_id UUID NOT NULL REFERENCES app_users(id);
ALTER TABLE application_settings ADD PRIMARY KEY (user_id);

ALTER TABLE salary_profiles ADD COLUMN user_id UUID NOT NULL REFERENCES app_users(id);
ALTER TABLE salary_profiles DROP CONSTRAINT uk_salary_profiles_effective_from;
ALTER TABLE salary_profiles ADD CONSTRAINT uk_salary_profiles_user_effective_from UNIQUE (user_id, effective_from);
CREATE INDEX idx_salary_profiles_user_effective_from_desc ON salary_profiles (user_id, effective_from DESC);

ALTER TABLE workdays ADD COLUMN user_id UUID NOT NULL REFERENCES app_users(id);
ALTER TABLE workdays DROP CONSTRAINT workdays_local_date_key;
ALTER TABLE workdays ADD CONSTRAINT uk_workdays_user_local_date UNIQUE (user_id, local_date);
CREATE INDEX idx_workdays_user_local_date ON workdays (user_id, local_date);

ALTER TABLE rewards ADD COLUMN user_id UUID NOT NULL REFERENCES app_users(id);
CREATE INDEX idx_rewards_user_status_id ON rewards (user_id, status, id);

ALTER TABLE goals ADD COLUMN user_id UUID NOT NULL REFERENCES app_users(id);
CREATE INDEX idx_goals_user_status_id ON goals (user_id, status, id);
CREATE INDEX idx_goals_user_closed_at ON goals (user_id, closed_at DESC, id DESC);
