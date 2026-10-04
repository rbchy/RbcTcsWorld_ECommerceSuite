package com.rbctcsworld.ecommerce.inventory;

import com.rbctcsworld.ecommerce.common.exception.ConflictException;
import com.rbctcsworld.ecommerce.product.Product;
import com.rbctcsworld.ecommerce.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The ONLY place that changes product stock for orders.
 *
 * Overselling protection: stock is decreased with one conditional SQL statement
 *   UPDATE products SET stock = stock - :qty WHERE id = :id AND active AND stock >= :qty
 * The database executes it atomically, so two customers buying the last unit at the same
 * moment cannot both succeed - one update affects 1 row, the other affects 0 rows.
 * A CHECK (stock >= 0) constraint is a second safety net.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY) // must run inside the caller's order transaction
public class InventoryService {

    private final ProductRepository products;
    private final StockMovementRepository movements;

    public InventoryService(ProductRepository products, StockMovementRepository movements) {
        this.products = products;
        this.movements = movements;
    }

    /** Reserves stock for an order line or throws 409. Caller's transaction rolls back everything on failure. */
    public void reserve(Product product, int quantity, Long orderId) {
        int updated = products.decrementStock(product.getId(), quantity);
        if (updated == 0) {
            if (!product.isActive()) {
                throw new ConflictException("Product no longer available: " + product.getSku());
            }
            int available = products.currentStock(product.getId());
            throw new ConflictException("Insufficient stock for " + product.getSku()
                    + ": requested " + quantity + ", available " + available);
        }
        int after = products.currentStock(product.getId());
        movements.save(new StockMovement(product.getId(), orderId, -quantity, StockMovement.ORDER_PLACED, after));
    }

    /** Puts stock back when an order is cancelled. */
    public void release(Long productId, int quantity, Long orderId) {
        release(productId, quantity, orderId, StockMovement.ORDER_CANCELLED);
    }

    /** Puts stock back with an explicit reason (ORDER_CANCELLED or RETURN_RESTOCK). */
    public void release(Long productId, int quantity, Long orderId, String reason) {
        products.incrementStock(productId, quantity);
        int after = products.currentStock(productId);
        movements.save(new StockMovement(productId, orderId, quantity, reason, after));
    }
}
