package com.rbctcsworld.ecommerce.coupon;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_redemptions")
public class CouponRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "redeemed_at", nullable = false)
    private LocalDateTime redeemedAt;

    protected CouponRedemption() {
    }

    public CouponRedemption(Long couponId, Long userId, Long orderId) {
        this.couponId = couponId;
        this.userId = userId;
        this.orderId = orderId;
    }

    @PrePersist
    void onCreate() {
        redeemedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getCouponId() { return couponId; }
    public Long getUserId() { return userId; }
    public Long getOrderId() { return orderId; }
}
