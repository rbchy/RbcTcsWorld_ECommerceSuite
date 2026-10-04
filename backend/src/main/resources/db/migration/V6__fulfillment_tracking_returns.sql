-- Module 4: shipping, tracking timeline, returns.

ALTER TABLE orders ADD COLUMN carrier         VARCHAR(10);
ALTER TABLE orders ADD COLUMN tracking_number VARCHAR(30);
ALTER TABLE orders ADD COLUMN shipped_at      TIMESTAMP;
ALTER TABLE orders ADD COLUMN delivered_at    TIMESTAMP;
ALTER TABLE orders ADD CONSTRAINT uk_orders_tracking UNIQUE (tracking_number);
ALTER TABLE orders ADD CONSTRAINT chk_orders_status CHECK (status IN
    ('PLACED', 'PAID', 'SHIPPED', 'DELIVERED', 'RETURN_REQUESTED', 'RETURNED', 'CANCELLED'));

-- Timeline of every status change (customer tracking page + audit trail)
CREATE TABLE order_events (
    id          BIGSERIAL    PRIMARY KEY,
    order_id    BIGINT       NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    status      VARCHAR(20)  NOT NULL,
    note        VARCHAR(255),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_order_events_order ON order_events(order_id);

-- Orders created before Module 4 get one event showing their current status
INSERT INTO order_events (order_id, status, note, created_at)
SELECT id, status, 'Backfilled by V6 migration', created_at FROM orders;

-- One return request per order
CREATE TABLE return_requests (
    id             BIGSERIAL     PRIMARY KEY,
    order_id       BIGINT        NOT NULL UNIQUE REFERENCES orders(id),
    reason         VARCHAR(20)   NOT NULL CHECK (reason IN ('DAMAGED', 'WRONG_ITEM', 'NOT_AS_DESCRIBED', 'NO_LONGER_NEEDED')),
    comment        VARCHAR(500),
    status         VARCHAR(10)   NOT NULL CHECK (status IN ('REQUESTED', 'APPROVED', 'REJECTED')),
    refund_amount  NUMERIC(12,2),
    restocked      BOOLEAN       NOT NULL DEFAULT FALSE,
    admin_note     VARCHAR(255),
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at    TIMESTAMP
);
