package com.rbctcsworld.ecommerce.wishlist;

import com.rbctcsworld.ecommerce.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wishlist_items")
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "price_when_added", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceWhenAdded;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected WishlistItem() {
    }

    public WishlistItem(Long userId, Product product, LocalDateTime now) {
        this.userId = userId;
        this.product = product;
        this.priceWhenAdded = product.getPrice();
        this.createdAt = now;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Product getProduct() { return product; }
    public BigDecimal getPriceWhenAdded() { return priceWhenAdded; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
