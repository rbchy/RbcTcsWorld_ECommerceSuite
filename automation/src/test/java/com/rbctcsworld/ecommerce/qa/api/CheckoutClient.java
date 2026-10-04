package com.rbctcsworld.ecommerce.qa.api;

import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Map;

public class CheckoutClient extends ApiClient {

    /** Price preview for the current cart. couponCode may be null. */
    public Response quote(String token, String couponCode) {
        Map<String, Object> body = new HashMap<>();
        body.put("couponCode", couponCode);
        return as(token).body(body).post("/api/checkout/quote");
    }
}
