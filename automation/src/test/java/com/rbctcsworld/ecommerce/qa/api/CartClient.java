package com.rbctcsworld.ecommerce.qa.api;

import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Map;

public class CartClient extends ApiClient {

    public Response get(String token) {
        return as(token).get("/api/cart");
    }

    public Response add(String token, Long productId, Integer quantity) {
        Map<String, Object> body = new HashMap<>();   // HashMap allows null values for negative tests
        body.put("productId", productId);
        body.put("quantity", quantity);
        return as(token).body(body).post("/api/cart/items");
    }

    public Response update(String token, long itemId, Integer quantity) {
        Map<String, Object> body = new HashMap<>();
        body.put("quantity", quantity);
        return as(token).body(body).put("/api/cart/items/{id}", itemId);
    }

    public Response remove(String token, long itemId) {
        return as(token).delete("/api/cart/items/{id}", itemId);
    }

    public Response clear(String token) {
        return as(token).delete("/api/cart");
    }
}
