package com.rbctcsworld.ecommerce.returns;

import com.rbctcsworld.ecommerce.order.CustomerOrder;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Decision table for approved returns:
 *
 * | Reason            | Refund                 | Put back in stock |
 * |-------------------|------------------------|-------------------|
 * | DAMAGED           | full total             | NO  (written off) |
 * | WRONG_ITEM        | full total             | yes               |
 * | NOT_AS_DESCRIBED  | full total             | yes               |
 * | NO_LONGER_NEEDED  | total - shipping fee   | yes               |
 */
public final class ReturnPolicy {

    public static final String DAMAGED = "DAMAGED";
    public static final String WRONG_ITEM = "WRONG_ITEM";
    public static final String NOT_AS_DESCRIBED = "NOT_AS_DESCRIBED";
    public static final String NO_LONGER_NEEDED = "NO_LONGER_NEEDED";
    public static final Set<String> REASONS = Set.of(DAMAGED, WRONG_ITEM, NOT_AS_DESCRIBED, NO_LONGER_NEEDED);

    private ReturnPolicy() {
    }

    public static BigDecimal refundAmount(String reason, CustomerOrder order) {
        return NO_LONGER_NEEDED.equals(reason)
                ? order.getTotal().subtract(order.getShippingFee())
                : order.getTotal();
    }

    public static boolean restock(String reason) {
        return !DAMAGED.equals(reason);
    }
}
