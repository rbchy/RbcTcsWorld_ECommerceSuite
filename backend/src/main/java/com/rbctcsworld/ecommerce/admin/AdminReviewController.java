package com.rbctcsworld.ecommerce.admin;

import com.rbctcsworld.ecommerce.review.Review;
import com.rbctcsworld.ecommerce.review.ReviewDtos.ReviewResponse;
import com.rbctcsworld.ecommerce.review.ReviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Review moderation (ROLE_ADMIN via SecurityConfig).
 * GET  /api/admin/reviews?status=PUBLISHED|HIDDEN   200
 * POST /api/admin/reviews/{id}/hide                 200 (removed from list and from the average) | 404
 * POST /api/admin/reviews/{id}/publish              200 | 404
 */
@RestController
@RequestMapping("/api/admin/reviews")
public class AdminReviewController {

    private final ReviewService reviews;

    public AdminReviewController(ReviewService reviews) {
        this.reviews = reviews;
    }

    @GetMapping
    public List<ReviewResponse> list(@RequestParam(required = false) String status) {
        return reviews.adminList(status);
    }

    @PostMapping("/{id}/hide")
    public ReviewResponse hide(@PathVariable Long id) {
        return reviews.moderate(id, Review.HIDDEN);
    }

    @PostMapping("/{id}/publish")
    public ReviewResponse publish(@PathVariable Long id) {
        return reviews.moderate(id, Review.PUBLISHED);
    }
}
