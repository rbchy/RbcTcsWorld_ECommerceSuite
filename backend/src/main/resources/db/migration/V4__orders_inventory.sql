-- Module 2: orders + inventory audit trail.

CREATE TABLE orders (
    id            BIGSERIAL     PRIMARY KEY,
    order_number  VARCHAR(30)   NOT NULL UNIQUE,
    user_id       BIGINT        NOT NULL REFERENCES users(id),
    status        VARCHAR(20)   NOT NULL,
    total_items   INTEGER       NOT NULL CHECK (total_items > 0),
    subtotal      NUMERIC(12,2) NOT NULL CHECK (subtotal >= 0),
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at  TIMESTAMP
);
CREATE INDEX idx_orders_user ON orders(user_id);

-- Snapshot of product data at purchase time: later price/name changes must not alter old orders.
CREATE TABLE order_items (
    id            BIGSERIAL     PRIMARY KEY,
    order_id      BIGINT        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id    BIGINT        NOT NULL REFERENCES products(id),
    sku           VARCHAR(100)  NOT NULL,
    product_name  VARCHAR(255)  NOT NULL,
    unit_price    NUMERIC(12,2) NOT NULL,
    quantity      INTEGER       NOT NULL CHECK (quantity > 0),
    line_total    NUMERIC(12,2) NOT NULL
);
CREATE INDEX idx_order_items_order ON order_items(order_id);

-- Every stock change is recorded (who/why/when/how much/resulting stock): inventory audit trail.
CREATE TABLE stock_movements (
    id            BIGSERIAL     PRIMARY KEY,
    product_id    BIGINT        NOT NULL REFERENCES products(id),
    order_id      BIGINT        REFERENCES orders(id),
    change_qty    INTEGER       NOT NULL CHECK (change_qty <> 0),
    reason        VARCHAR(30)   NOT NULL,
    stock_after   INTEGER       NOT NULL CHECK (stock_after >= 0),
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_stock_movements_product ON stock_movements(product_id);

-- Database-level guarantee that stock can never go negative (last line of defence against overselling).
ALTER TABLE products ADD CONSTRAINT chk_products_stock_non_negative CHECK (stock >= 0);
