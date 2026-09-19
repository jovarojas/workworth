-- Manual, visual priority for pending rewards: an independent integer per reward that the user
-- can reorder freely (e.g. drag and drop) without touching its price/name or creation date.
-- Backfilled to the existing id so current listings (already ordered by id) do not change until
-- the user actually reorders something.
ALTER TABLE rewards ADD COLUMN display_order BIGINT;
UPDATE rewards SET display_order = id;
ALTER TABLE rewards ALTER COLUMN display_order SET NOT NULL;

CREATE INDEX idx_rewards_user_status_display_order ON rewards (user_id, status, display_order);
