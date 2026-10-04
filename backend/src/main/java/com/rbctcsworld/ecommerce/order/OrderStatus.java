package com.rbctcsworld.ecommerce.order;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Order lifecycle as an explicit state machine (stored as VARCHAR, guarded by a DB CHECK constraint).
 *
 *   PLACED ──pay──> PAID ──ship──> SHIPPED ──deliver──> DELIVERED ──request return──> RETURN_REQUESTED
 *     │              │                                     ^                              │      │
 *     └──cancel──┐   └──cancel (refund)──┐                 └──────── reject return ───────┘      │
 *                v                       v                                                      approve
 *            CANCELLED <─────────────────┘                                                (refund)│
 *                                                                                                  v
 *                                                                                              RETURNED
 * Every move not listed in ALLOWED is rejected with 409.
 */
public final class OrderStatus {
    public static final String PLACED = "PLACED";
    public static final String PAID = "PAID";
    public static final String SHIPPED = "SHIPPED";
    public static final String DELIVERED = "DELIVERED";
    public static final String RETURN_REQUESTED = "RETURN_REQUESTED";
    public static final String RETURNED = "RETURNED";
    public static final String CANCELLED = "CANCELLED";

    public static final List<String> ALL =
            List.of(PLACED, PAID, SHIPPED, DELIVERED, RETURN_REQUESTED, RETURNED, CANCELLED);

    public static final Map<String, Set<String>> ALLOWED = Map.of(
            PLACED, Set.of(PAID, CANCELLED),
            PAID, Set.of(SHIPPED, CANCELLED),
            SHIPPED, Set.of(DELIVERED),
            DELIVERED, Set.of(RETURN_REQUESTED),
            RETURN_REQUESTED, Set.of(RETURNED, DELIVERED),
            RETURNED, Set.of(),
            CANCELLED, Set.of());

    private OrderStatus() {
    }

    public static boolean canMove(String from, String to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }
}
