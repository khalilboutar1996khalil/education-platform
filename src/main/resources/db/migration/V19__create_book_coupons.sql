-- Coupons printed in the parascolaire books. Each copy carries a QR code (its serial) and a secret
-- code under a scratch-off panel: whoever bought the book can see the secret, whoever only
-- flipped through it in the shop cannot. Redeeming one creates a student account for the book's
-- level, signed in with a generated username rather than an email address.

-- A student who came through a book may have no email address, so an account is now known by
-- its email, its username, or both.
ALTER TABLE users ALTER COLUMN email DROP NOT NULL;
ALTER TABLE users ADD COLUMN username VARCHAR(60);
ALTER TABLE users ADD CONSTRAINT uk_users_username UNIQUE (username);
ALTER TABLE users ADD CONSTRAINT ck_users_has_identifier CHECK (email IS NOT NULL OR username IS NOT NULL);

-- One print run: N coupons for one level, downloaded together as a PDF
CREATE TABLE coupon_batches (
    id         BIGSERIAL PRIMARY KEY,
    level      VARCHAR(20)  NOT NULL,
    quantity   INTEGER      NOT NULL,
    -- Free text for the admin, e.g. "Livre 7e — édition 2026"
    label      VARCHAR(255),

    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    deleted_at TIMESTAMP(6) WITH TIME ZONE,

    CONSTRAINT fk_coupon_batches_level FOREIGN KEY (level) REFERENCES school_levels (code),
    CONSTRAINT ck_coupon_batches_quantity CHECK (quantity BETWEEN 1 AND 2000)
);

CREATE TABLE coupons (
    id                  BIGSERIAL PRIMARY KEY,
    batch_id            BIGINT      NOT NULL,
    -- Printed in clear next to the QR code, e.g. "3-0042"
    serial              VARCHAR(20) NOT NULL,
    -- Under the scratch-off panel. Kept readable (not hashed) so the batch PDF can be printed again.
    code                VARCHAR(20) NOT NULL,
    redeemed_by_user_id BIGINT,
    redeemed_at         TIMESTAMP(6) WITH TIME ZONE,

    created_at          TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by          VARCHAR(100) NOT NULL,
    updated_by          VARCHAR(100) NOT NULL,
    deleted_at          TIMESTAMP(6) WITH TIME ZONE,

    CONSTRAINT uk_coupons_serial UNIQUE (serial),
    CONSTRAINT uk_coupons_code UNIQUE (code),
    CONSTRAINT fk_coupons_batch FOREIGN KEY (batch_id) REFERENCES coupon_batches (id),
    CONSTRAINT fk_coupons_redeemed_by FOREIGN KEY (redeemed_by_user_id) REFERENCES users (id),
    -- Redeemed means both who and when; unused means neither
    CONSTRAINT ck_coupons_redemption CHECK (
        (redeemed_by_user_id IS NULL AND redeemed_at IS NULL)
        OR (redeemed_by_user_id IS NOT NULL AND redeemed_at IS NOT NULL)
    )
);

CREATE INDEX idx_coupons_batch ON coupons (batch_id, serial);
