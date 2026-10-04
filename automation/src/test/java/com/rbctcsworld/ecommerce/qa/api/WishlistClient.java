package com.rbctcsworld.ecommerce.qa.api;

import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Map;

public class WishlistClient extends ApiClient {

    public Response get(String token) {
        return as(token).get("/api/wishlist");
    }

    public Response add(String token, Long productId) {
        Map<String, Object> body = new HashMap<>();
        body.put("productId", productId);
        return as(token).body(body).post("/api/wishlist");
    }

    public Response remove(String token, long productId) {
        return as(token).delete("/api/wishlist/{id}", productId);
    }

    public Response moveToCart(String token, long productId) {
        return as(token).post("/api/wishlist/{id}/move-to-cart", productId);
    }
}
