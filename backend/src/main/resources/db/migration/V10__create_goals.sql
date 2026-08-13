CREATE TABLE goals (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    target_amount NUMERIC(19, 2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ,
    CONSTRAINT chk_goals_target_amount_positive CHECK (target_amount > 0),
    CONSTRAINT chk_goals_currency_code CHECK (currency_code IN ('EUR', 'USD')),
    CONSTRAINT chk_goals_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_goals_closed_at CHECK (
        (status = 'ACTIVE' AND closed_at IS NULL)
        OR (status IN ('COMPLETED', 'CANCELLED') AND closed_at IS NOT NULL)
    )
);

CREATE INDEX idx_goals_status_id ON goals (status, id);
CREATE INDEX idx_goals_closed_at ON goals (closed_at DESC, id DESC);
