package com.rbctcsworld.ecommerce.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
public class Review {

    public static final String PUBLISHED = "PUBLISHED";
    public static final String HIDDEN = "HIDDEN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private int rating;

    @Column(length = 100)
    private String title;

    @Column(length = 2000)
    private String body;

    @Column(nullable = false, length = 10)
    private String status = PUBLISHED;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected Review() {
    }

    public Review(Long productId, Long userId, int rating, String title, String body, LocalDateTime now) {
        this.productId = productId;
        this.userId = userId;
        this.rating = rating;
        this.title = title;
        this.body = body;
        this.createdAt = now;
    }

    public void edit(int rating, String title, String body, LocalDateTime now) {
        this.rating = rating;
        this.title = title;
        this.body = body;
        this.updatedAt = now;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public Long getUserId() { return userId; }
    public int getRating() { return rating; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
