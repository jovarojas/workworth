CREATE TABLE rewards (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    price NUMERIC(19, 2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    last_reached_context VARCHAR(16),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_rewards_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_rewards_price_positive CHECK (price > 0),
    CONSTRAINT chk_rewards_currency_code CHECK (currency_code IN ('EUR', 'USD')),
    CONSTRAINT chk_rewards_status CHECK (status IN ('PENDING', 'ACQUIRED')),
    CONSTRAINT chk_rewards_last_reached_context CHECK (
        last_reached_context IS NULL OR last_reached_context IN ('TODAY', 'WEEK', 'MONTH', 'ALL_TIME')
    )
);

CREATE INDEX idx_rewards_status_id ON rewards (status, id);
