package com.rbctcsworld.ecommerce.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/** Request/response shapes for the cart API (kept separate from JPA entities). */
public final class CartDtos {

    private CartDtos() {
    }

    public record AddItemRequest(
            @NotNull Long productId,
            @NotNull @Min(1) @Max(CartService.MAX_QTY_PER_ITEM) Integer quantity) {
    }

    public record UpdateItemRequest(
            @NotNull @Min(1) @Max(CartService.MAX_QTY_PER_ITEM) Integer quantity) {
    }

    /**
     * available = false when the product was deleted or its stock dropped below the cart quantity
     * after it was added - the checkout module will refuse such lines.
     */
    public record CartLine(Long itemId, Long productId, String sku, String name, BigDecimal unitPrice,
                           int quantity, BigDecimal lineTotal, boolean available) {
    }

    public record CartResponse(List<CartLine> items, int totalQuantity, BigDecimal subtotal) {
    }
}
