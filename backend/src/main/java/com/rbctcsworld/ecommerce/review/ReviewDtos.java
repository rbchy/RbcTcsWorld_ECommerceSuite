package com.rbctcsworld.ecommerce.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class ReviewDtos {

    private ReviewDtos() {
    }

    public record CreateReviewRequest(
            @NotNull Long productId,
            @NotNull @Min(1) @Max(5) Integer rating,
            @Size(max = 100) String title,
            @Size(max = 2000) String body) {
    }

    public record UpdateReviewRequest(
            @NotNull @Min(1) @Max(5) Integer rating,
            @Size(max = 100) String title,
            @Size(max = 2000) String body) {
    }

    /** reviewer is masked ("ja***"): a public endpoint must never expose customer e-mail addresses. */
    public record ReviewResponse(Long id, Long productId, int rating, String title, String body, String reviewer,
                                 boolean verifiedPurchase, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }

    /** distribution: number of reviews for every star value 5..1 (always all five keys). */
    public record ProductReviews(Long productId, BigDecimal averageRating, int reviewCount,
                                 Map<Integer, Long> distribution, List<ReviewResponse> reviews) {
    }

    public record RatingSummary(BigDecimal average, int count, Map<Integer, Long> distribution) {
    }
}
