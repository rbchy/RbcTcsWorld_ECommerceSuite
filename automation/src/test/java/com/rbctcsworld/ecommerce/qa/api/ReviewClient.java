package com.rbctcsworld.ecommerce.qa.api;

import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Map;

/** Customer reviews, public product reviews and admin moderation. */
public class ReviewClient extends ApiClient {

    public static Map<String, Object> body(Long productId, Integer rating, String title, String text) {
        Map<String, Object> b = new HashMap<>();   // HashMap allows nulls for negative tests
        if (productId != null) b.put("productId", productId);
        b.put("rating", rating);
        if (title != null) b.put("title", title);
        if (text != null) b.put("body", text);
        return b;
    }

    public Response create(String token, long productId, Integer rating, String title, String text) {
        return as(token).body(body(productId, rating, title, text)).post("/api/reviews");
    }

    public Response create(String token, long productId, int rating) {
        return create(token, productId, rating, null, null);
    }

    public Response update(String token, long reviewId, Integer rating, String title, String text) {
        return as(token).body(body(null, rating, title, text)).put("/api/reviews/{id}", reviewId);
    }

    public Response delete(String token, long reviewId) {
        return as(token).delete("/api/reviews/{id}", reviewId);
    }

    public Response mine(String token) {
        return as(token).get("/api/reviews/mine");
    }

    /** Public: no token. */
    public Response forProduct(long productId, String sort) {
        return sort == null
                ? anonymous().get("/api/products/{id}/reviews", productId)
                : anonymous().queryParam("sort", sort).get("/api/products/{id}/reviews", productId);
    }

    public Response forProduct(long productId) {
        return forProduct(productId, null);
    }

    public Response adminList(String token, String status) {
        return status == null ? as(token).get("/api/admin/reviews")
                : as(token).queryParam("status", status).get("/api/admin/reviews");
    }

    public Response hide(String token, long reviewId) {
        return as(token).post("/api/admin/reviews/{id}/hide", reviewId);
    }

    public Response publish(String token, long reviewId) {
        return as(token).post("/api/admin/reviews/{id}/publish", reviewId);
    }
}
