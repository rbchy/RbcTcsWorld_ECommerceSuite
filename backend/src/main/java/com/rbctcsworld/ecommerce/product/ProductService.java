package com.rbctcsworld.ecommerce.product;

import com.rbctcsworld.ecommerce.common.exception.ConflictException;
import com.rbctcsworld.ecommerce.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductService {

    private final ProductRepository repo;

    public ProductService(ProductRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<Product> all(String q) {
        return (q == null || q.isBlank())
                ? repo.findByActiveTrueOrderByIdAsc()
                : repo.findByNameContainingIgnoreCaseAndActiveTrueOrderByIdAsc(q.trim());
    }

    /** Deleted (inactive) products are invisible: 404, same as never existed. */
    @Transactional(readOnly = true)
    public Product get(long id) {
        return repo.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));
    }

    public Product create(ProductRequest r) {
        String sku = r.sku().trim();
        if (repo.existsBySku(sku)) {
            throw new ConflictException("SKU already exists: " + sku);
        }
        return repo.save(new Product(r.name().trim(), sku, r.category(), r.price(), r.stock()));
    }

    public Product update(long id, ProductRequest r) {
        Product p = get(id);
        String sku = r.sku().trim();
        repo.findBySku(sku)
                .filter(other -> !other.getId().equals(p.getId()))
                .ifPresent(other -> { throw new ConflictException("SKU already exists: " + sku); });
        p.update(r.name().trim(), sku, r.category(), r.price(), r.stock());
        return repo.save(p);
    }

    /** Soft delete: keeps history for orders/audit, hides the product from customers. */
    public void delete(long id) {
        Product p = get(id);
        p.setActive(false);
        repo.save(p);
    }
}
