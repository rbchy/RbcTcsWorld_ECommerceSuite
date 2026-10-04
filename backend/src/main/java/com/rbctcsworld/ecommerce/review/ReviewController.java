package com.rbctcsworld.ecommerce.review;

import com.rbctcsworld.ecommerce.review.ReviewDtos.CreateReviewRequest;
import com.rbctcsworld.ecommerce.review.ReviewDtos.ProductReviews;
import com.rbctcsworld.ecommerce.review.ReviewDtos.ReviewResponse;
import com.rbctcsworld.ecommerce.review.ReviewDtos.UpdateReviewRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * GET    /api/products/{id}/reviews?sort=newest|highest|lowest   public  200 | 400 bad sort | 404
 * POST   /api/reviews          JWT  201 | 400 validation | 403 not received | 404 product | 409 already reviewed
 * PUT    /api/reviews/{id}     JWT  200 | 400 | 404 (missing or not yours)
 * DELETE /api/reviews/{id}     JWT  204 | 404
 * GET    /api/reviews/mine     JWT  200 my reviews (any status)
 */
@RestController
public class ReviewController {

    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    @GetMapping("/api/products/{productId}/reviews")
    public ProductReviews forProduct(@PathVariable Long productId, @RequestParam(required = false) String sort) {
        return service.forProduct(productId, sort);
    }

    @PostMapping("/api/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(Authentication auth, @Valid @RequestBody CreateReviewRequest r) {
        return service.create(auth.getName(), r.productId(), r.rating(), r.title(), r.body());
    }

    @PutMapping("/api/reviews/{id}")
    public ReviewResponse update(Authentication auth, @PathVariable Long id, @Valid @RequestBody UpdateReviewRequest r) {
        return service.update(auth.getName(), id, r.rating(), r.title(), r.body());
    }

    @DeleteMapping("/api/reviews/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication auth, @PathVariable Long id) {
        service.delete(auth.getName(), id);
    }

    @GetMapping("/api/reviews/mine")
    public List<ReviewResponse> mine(Authentication auth) {
        return service.mine(auth.getName());
    }
}
