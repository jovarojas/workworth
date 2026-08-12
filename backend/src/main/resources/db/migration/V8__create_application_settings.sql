CREATE TABLE application_settings (
    id SMALLINT PRIMARY KEY,
    currency_code VARCHAR(3) NOT NULL,
    currency_locked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_application_settings_singleton CHECK (id = 1),
    CONSTRAINT chk_application_settings_currency_code CHECK (currency_code IN ('EUR', 'USD'))
);

INSERT INTO application_settings (id, currency_code, currency_locked_at, created_at, updated_at)
VALUES (
    1,
    'EUR',
    CASE
        WHEN EXISTS (SELECT 1 FROM salary_profiles)
          OR EXISTS (SELECT 1 FROM workday_earnings)
        THEN CURRENT_TIMESTAMP
        ELSE NULL
    END,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
