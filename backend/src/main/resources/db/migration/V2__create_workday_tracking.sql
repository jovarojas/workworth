CREATE TABLE workdays (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, local_date DATE NOT NULL UNIQUE, time_zone VARCHAR(64) NOT NULL,
 schedule_variant VARCHAR(16) NOT NULL, scheduled_start TIME NOT NULL, scheduled_end TIME NOT NULL,
 maximum_economic_seconds BIGINT NOT NULL, status VARCHAR(20) NOT NULL, cancelled_at TIMESTAMPTZ,
 cancellation_reason VARCHAR(500), created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT chk_workday_schedule CHECK (scheduled_end > scheduled_start),
 CONSTRAINT chk_workday_maximum_seconds CHECK (maximum_economic_seconds >= 0)
);
CREATE INDEX idx_workdays_local_date ON workdays(local_date);
CREATE TABLE meal_breaks (id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, workday_id BIGINT NOT NULL REFERENCES workdays(id), started_at TIMESTAMPTZ NOT NULL, ended_at TIMESTAMPTZ, ended_automatically BOOLEAN NOT NULL DEFAULT FALSE, CONSTRAINT chk_meal_break_range CHECK (ended_at IS NULL OR ended_at > started_at));
CREATE UNIQUE INDEX uk_meal_breaks_open_workday ON meal_breaks(workday_id) WHERE ended_at IS NULL;
CREATE TABLE partial_absences (id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, workday_id BIGINT NOT NULL REFERENCES workdays(id), started_at TIMESTAMPTZ NOT NULL, ended_at TIMESTAMPTZ NOT NULL, reason VARCHAR(500), CONSTRAINT chk_partial_absence_range CHECK (ended_at > started_at));
CREATE INDEX idx_partial_absences_workday ON partial_absences(workday_id);
CREATE TABLE workday_time_corrections (id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, workday_id BIGINT NOT NULL REFERENCES workdays(id), cause VARCHAR(32) NOT NULL, previous_economic_seconds BIGINT NOT NULL, new_economic_seconds BIGINT NOT NULL, corrected_at TIMESTAMPTZ NOT NULL, CONSTRAINT chk_correction_seconds CHECK (previous_economic_seconds >= 0 AND new_economic_seconds >= 0));
CREATE INDEX idx_workday_time_corrections_workday ON workday_time_corrections(workday_id, corrected_at);
