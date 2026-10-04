package com.rbctcsworld.ecommerce.returns;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "return_requests")
public class ReturnRequest {

    public static final String REQUESTED = "REQUESTED";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false, length = 20)
    private String reason;

    @Column(length = 500)
    private String comment;

    @Column(nullable = false, length = 10)
    private String status = REQUESTED;

    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(nullable = false)
    private boolean restocked;

    @Column(name = "admin_note")
    private String adminNote;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    protected ReturnRequest() {
    }

    public ReturnRequest(Long orderId, String reason, String comment) {
        this.orderId = orderId;
        this.reason = reason;
        this.comment = comment;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    void approve(BigDecimal refundAmount, boolean restocked, String note) {
        this.status = APPROVED;
        this.refundAmount = refundAmount;
        this.restocked = restocked;
        this.adminNote = note;
        this.resolvedAt = LocalDateTime.now();
    }

    void reject(String note) {
        this.status = REJECTED;
        this.adminNote = note;
        this.resolvedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public String getReason() { return reason; }
    public String getComment() { return comment; }
    public String getStatus() { return status; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public boolean isRestocked() { return restocked; }
    public String getAdminNote() { return adminNote; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
}
