ALTER TABLE earning_corrections
    ADD COLUMN previous_correction_id BIGINT REFERENCES earning_corrections(id);

CREATE UNIQUE INDEX uk_earning_corrections_previous
    ON earning_corrections(previous_correction_id)
    WHERE previous_correction_id IS NOT NULL;
