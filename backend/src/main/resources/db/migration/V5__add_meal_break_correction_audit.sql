ALTER TABLE workday_time_corrections
    ADD COLUMN meal_break_id BIGINT REFERENCES meal_breaks(id),
    ADD COLUMN previous_break_started_at TIMESTAMPTZ,
    ADD COLUMN previous_break_ended_at TIMESTAMPTZ,
    ADD COLUMN new_break_started_at TIMESTAMPTZ,
    ADD COLUMN new_break_ended_at TIMESTAMPTZ,
    ADD COLUMN previous_break_ended_automatically BOOLEAN;

CREATE INDEX idx_workday_time_corrections_meal_break
    ON workday_time_corrections(meal_break_id)
    WHERE meal_break_id IS NOT NULL;
