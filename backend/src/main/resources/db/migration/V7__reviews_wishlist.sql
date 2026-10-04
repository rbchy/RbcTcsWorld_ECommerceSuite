-- Module 5: product reviews & ratings, wishlist.

-- One review per customer per product. Only PUBLISHED reviews are shown and counted.
CREATE TABLE reviews (
    id          BIGSERIAL     PRIMARY KEY,
    product_id  BIGINT        NOT NULL REFERENCES products(id),
    user_id     BIGINT        NOT NULL REFERENCES users(id),
    rating      INTEGER       NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title       VARCHAR(100),
    body        VARCHAR(2000),
    status      VARCHAR(10)   NOT NULL DEFAULT 'PUBLISHED' CHECK (status IN ('PUBLISHED', 'HIDDEN')),
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP,
    CONSTRAINT uk_reviews_user_product UNIQUE (user_id, product_id)
);
CREATE INDEX idx_reviews_product ON reviews(product_id);

-- Denormalised rating summary on the product (fast product list). Recalculated on every review change
-- while the product row is locked, so concurrent reviews can never leave a wrong average.
ALTER TABLE products ADD COLUMN rating_average NUMERIC(2,1) NOT NULL DEFAULT 0;
ALTER TABLE products ADD COLUMN rating_count   INTEGER      NOT NULL DEFAULT 0;
ALTER TABLE products ADD CONSTRAINT chk_products_rating CHECK (rating_average BETWEEN 0 AND 5 AND rating_count >= 0);

-- Wishlist: price_when_added lets the UI show "price dropped".
CREATE TABLE wishlist_items (
    id                BIGSERIAL      PRIMARY KEY,
    user_id           BIGINT         NOT NULL REFERENCES users(id),
    product_id        BIGINT         NOT NULL REFERENCES products(id),
    price_when_added  NUMERIC(12,2)  NOT NULL,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_wishlist_user_product UNIQUE (user_id, product_id)
);
