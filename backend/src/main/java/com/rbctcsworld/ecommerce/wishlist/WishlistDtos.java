package com.rbctcsworld.ecommerce.wishlist;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class WishlistDtos {

    private WishlistDtos() {
    }

    public record AddRequest(@NotNull Long productId) {
    }

    /**
     * available  = product still sold and in stock.
     * priceDrop  = priceWhenAdded - currentPrice when the price went DOWN, otherwise 0.00.
     */
    public record WishlistLine(Long productId, String sku, String name, BigDecimal priceWhenAdded,
                               BigDecimal currentPrice, BigDecimal priceDrop, boolean available,
                               BigDecimal ratingAverage, int ratingCount, LocalDateTime addedAt) {
    }

    public record WishlistResponse(List<WishlistLine> items, int count) {
    }

    /** created = false when the product was already on the wishlist (idempotent add, HTTP 200 instead of 201). */
    public record AddResult(boolean created, WishlistResponse wishlist) {
    }
}
