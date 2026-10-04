package com.rbctcsworld.ecommerce.qa.api;

import io.restassured.response.Response;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProductClient extends ApiClient {

    public Response list() {
        return anonymous().get("/api/products");
    }

    public Response search(String q) {
        return anonymous().queryParam("q", q).get("/api/products");
    }

    /** Raw GET for negative tests with malformed paths. */
    public Response anonymousGet(String path) {
        return anonymous().get(path);
    }

    public Response get(long id) {
        return anonymous().get("/api/products/{id}", id);
    }

    public Response create(String token, Map<String, Object> body) {
        return as(token).body(body).post("/api/products");
    }

    public Response update(String token, long id, Map<String, Object> body) {
        return as(token).body(body).put("/api/products/{id}", id);
    }

    public Response delete(String token, long id) {
        return as(token).delete("/api/products/{id}", id);
    }

    /** Finds the id of an active product by SKU (seed data has fixed SKUs). */
    public long idBySku(String sku) {
        List<Map<String, Object>> products = list().then().statusCode(200).extract().jsonPath().getList("$");
        return products.stream()
                .filter(p -> sku.equals(p.get("sku")))
                .map(p -> ((Number) p.get("id")).longValue())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No active product with SKU " + sku));
    }

    public static Map<String, Object> body(String name, String sku, String category, BigDecimal price, int stock) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("sku", sku);
        m.put("category", category);
        m.put("price", price);
        m.put("stock", stock);
        return m;
    }
}
