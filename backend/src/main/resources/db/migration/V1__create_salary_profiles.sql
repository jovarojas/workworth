CREATE TABLE salary_profiles (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    effective_from DATE NOT NULL,
    gross_annual NUMERIC(19, 2),
    net_monthly_real NUMERIC(19, 2),
    currency_code CHAR(3) NOT NULL,
    pay_periods SMALLINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_salary_profiles_effective_from UNIQUE (effective_from),
    CONSTRAINT chk_salary_profiles_effective_from_first_day
        CHECK (EXTRACT(DAY FROM effective_from) = 1),
    CONSTRAINT chk_salary_profiles_gross_annual_positive
        CHECK (gross_annual IS NULL OR gross_annual > 0),
    CONSTRAINT chk_salary_profiles_net_monthly_real_positive
        CHECK (net_monthly_real IS NULL OR net_monthly_real > 0),
    CONSTRAINT chk_salary_profiles_currency_code_uppercase
        CHECK (currency_code = UPPER(currency_code)),
    CONSTRAINT chk_salary_profiles_pay_periods_mvp
        CHECK (pay_periods = 12)
);

CREATE INDEX idx_salary_profiles_effective_from_desc
    ON salary_profiles (effective_from DESC);

CREATE TABLE salary_estimates (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    salary_profile_id BIGINT NOT NULL,
    fiscal_year SMALLINT NOT NULL,
    country_code CHAR(2) NOT NULL,
    region_code VARCHAR(10) NOT NULL,
    rule_set_version VARCHAR(40) NOT NULL,
    input_snapshot JSONB NOT NULL,
    estimated_net_annual NUMERIC(19, 2) NOT NULL,
    estimated_net_monthly NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_salary_estimates_profile
        FOREIGN KEY (salary_profile_id) REFERENCES salary_profiles (id),
    CONSTRAINT uk_salary_estimates_profile_year_rule
        UNIQUE (salary_profile_id, fiscal_year, rule_set_version),
    CONSTRAINT chk_salary_estimates_fiscal_year_positive
        CHECK (fiscal_year > 0),
    CONSTRAINT chk_salary_estimates_annual_positive
        CHECK (estimated_net_annual > 0),
    CONSTRAINT chk_salary_estimates_monthly_positive
        CHECK (estimated_net_monthly > 0)
);

CREATE INDEX idx_salary_estimates_profile_created_at_desc
    ON salary_estimates (salary_profile_id, created_at DESC);
