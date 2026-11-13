-- Yearly subscriptions. A student asks for access with the plan of their level and says how they
-- paid (D17 or bank transfer); the admin checks the payment and accepts or refuses, exactly as
-- for any other request. Accepting gives access until users.access_until.

-- One plan per level, priced in dinars. A level without an active plan keeps the old free request.
CREATE TABLE subscription_plans (
    id            BIGSERIAL PRIMARY KEY,
    level         VARCHAR(20)   NOT NULL,
    price_dt      NUMERIC(10,3) NOT NULL,
    duration_days INTEGER       NOT NULL DEFAULT 365,
    active        BOOLEAN       NOT NULL DEFAULT TRUE,

    created_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by    VARCHAR(100) NOT NULL,
    updated_by    VARCHAR(100) NOT NULL,
    deleted_at    TIMESTAMP(6) WITH TIME ZONE,

    CONSTRAINT uq_subscription_plans_level UNIQUE (level),
    CONSTRAINT fk_subscription_plans_level FOREIGN KEY (level) REFERENCES school_levels (code),
    CONSTRAINT ck_subscription_plans_price CHECK (price_dt >= 0),
    CONSTRAINT ck_subscription_plans_duration CHECK (duration_days > 0)
);

-- Where students send the money: shown on the public form. A single row, edited by the admin.
CREATE TABLE payment_settings (
    id             BIGSERIAL PRIMARY KEY,
    d17_number     VARCHAR(40),
    bank_name      VARCHAR(120),
    rib            VARCHAR(40),
    account_holder VARCHAR(120),
    instructions   VARCHAR(1000),

    created_at     TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by     VARCHAR(100) NOT NULL,
    updated_by     VARCHAR(100) NOT NULL,
    deleted_at     TIMESTAMP(6) WITH TIME ZONE
);

-- What the student chose and paid, copied onto the request so a later price change does not
-- rewrite history. renewal_of_id: the existing account a renewal extends instead of creating one.
ALTER TABLE access_requests
    ADD COLUMN plan_id           BIGINT,
    ADD COLUMN price_dt          NUMERIC(10,3),
    ADD COLUMN duration_days     INTEGER,
    ADD COLUMN payment_method    VARCHAR(20),
    ADD COLUMN payment_reference VARCHAR(100),
    ADD COLUMN renewal_of_id     BIGINT,
    ADD CONSTRAINT fk_access_requests_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans (id),
    ADD CONSTRAINT fk_access_requests_renewal_of FOREIGN KEY (renewal_of_id) REFERENCES users (id),
    -- A paid request says how it was paid; a free one (level without a plan) says nothing
    ADD CONSTRAINT ck_access_requests_payment CHECK (
        (plan_id IS NULL AND payment_method IS NULL)
        OR (plan_id IS NOT NULL AND price_dt IS NOT NULL AND duration_days IS NOT NULL
            AND payment_method IS NOT NULL AND payment_reference IS NOT NULL)
    );

-- Last day a student may sign in; null means no end (admins).
ALTER TABLE users ADD COLUMN access_until DATE;

-- Students already enrolled keep their access for the current school year, offered.
UPDATE users SET access_until = DATE '2027-06-30' WHERE role = 'STUDENT';

CREATE INDEX idx_users_access_until ON users (access_until) WHERE access_until IS NOT NULL;
