package com.rbctcsworld.ecommerce.qa.context;

import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * State shared by all step classes inside ONE scenario.
 * PicoContainer creates a fresh instance per scenario and injects it into each step class.
 */
public class ScenarioContext {

    private Response lastResponse;
    private String token;
    private final Map<String, Object> data = new HashMap<>();

    public Response lastResponse() {
        if (lastResponse == null) throw new IllegalStateException("No request has been sent in this scenario");
        return lastResponse;
    }

    public void lastResponse(Response r) { this.lastResponse = r; }

    public String token() { return token; }

    public void token(String token) { this.token = token; }

    /** Free-form values shared between steps, e.g. "productId", "orderId". */
    public void put(String key, Object value) { data.put(key, value); }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        if (!data.containsKey(key)) throw new IllegalStateException("Nothing stored under '" + key + "' in this scenario");
        return (T) data.get(key);
    }
}
