-- Module 3: price breakdown on orders, coupons, mock payments.

-- 1. Orders get a full price breakdown: total = subtotal - discount + shipping_fee + tax
ALTER TABLE orders ADD COLUMN discount     NUMERIC(12,2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN shipping_fee NUMERIC(12,2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN tax          NUMERIC(12,2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN total        NUMERIC(12,2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN coupon_code  VARCHAR(40);
ALTER TABLE orders ADD COLUMN paid_at      TIMESTAMP;
UPDATE orders SET total = subtotal;   -- orders created before Module 3 had no tax/shipping
ALTER TABLE orders ADD CONSTRAINT chk_orders_total_non_negative CHECK (total >= 0);

-- 2. Coupons
CREATE TABLE coupons (
    id                BIGSERIAL     PRIMARY KEY,
    code              VARCHAR(40)   NOT NULL UNIQUE,        -- stored upper-case
    type              VARCHAR(10)   NOT NULL CHECK (type IN ('PERCENT', 'FIXED')),
    discount_value    NUMERIC(12,2) NOT NULL CHECK (discount_value > 0),
    min_order_amount  NUMERIC(12,2) NOT NULL DEFAULT 0,
    max_uses          INTEGER,                              -- NULL = unlimited
    used_count        INTEGER       NOT NULL DEFAULT 0,
    valid_from        TIMESTAMP,
    valid_until       TIMESTAMP,
    active            BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_coupon_usage CHECK (max_uses IS NULL OR used_count <= max_uses)
);

-- One redemption per customer per coupon (enforced by the database too)
CREATE TABLE coupon_redemptions (
    id           BIGSERIAL PRIMARY KEY,
    coupon_id    BIGINT    NOT NULL REFERENCES coupons(id),
    user_id      BIGINT    NOT NULL REFERENCES users(id),
    order_id     BIGINT    NOT NULL REFERENCES orders(id),
    redeemed_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_coupon_user UNIQUE (coupon_id, user_id)
);

-- 3. Payment attempts and refunds. Only the last 4 card digits are ever stored (PCI-DSS principle).
CREATE TABLE payment_transactions (
    id              BIGSERIAL     PRIMARY KEY,
    order_id        BIGINT        NOT NULL REFERENCES orders(id),
    type            VARCHAR(10)   NOT NULL CHECK (type IN ('CHARGE', 'REFUND')),
    status          VARCHAR(10)   NOT NULL CHECK (status IN ('SUCCEEDED', 'FAILED')),
    amount          NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    card_last4      VARCHAR(4),
    failure_reason  VARCHAR(100),
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_payment_tx_order ON payment_transactions(order_id);

-- 4. Demo coupons
INSERT INTO coupons (code, type, discount_value, min_order_amount, max_uses, valid_from, valid_until, active) VALUES
 ('WELCOME10', 'PERCENT', 10.00,  0.00, NULL, NULL, NULL, TRUE),                          -- 10% off, once per customer
 ('SAVE5',     'FIXED',    5.00, 25.00, NULL, NULL, NULL, TRUE),                          -- $5 off orders of $25+
 ('EXPIRED20', 'PERCENT', 20.00,  0.00, NULL, NULL, '2020-01-01 00:00:00', TRUE),         -- expired
 ('FUTURE15',  'PERCENT', 15.00,  0.00, NULL, '2099-01-01 00:00:00', NULL, TRUE),         -- not active yet
 ('DISABLED',  'FIXED',   10.00,  0.00, NULL, NULL, NULL, FALSE);                         -- switched off
