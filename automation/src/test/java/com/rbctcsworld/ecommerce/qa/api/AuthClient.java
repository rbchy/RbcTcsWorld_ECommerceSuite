package com.rbctcsworld.ecommerce.qa.api;

import com.rbctcsworld.ecommerce.qa.config.TestConfig;
import io.restassured.response.Response;

import java.util.Map;

public class AuthClient extends ApiClient {

    public Response register(String email, String password) {
        return anonymous().body(Map.of("email", email, "password", password)).post("/api/auth/register");
    }

    public Response login(String email, String password) {
        return anonymous().body(Map.of("email", email, "password", password)).post("/api/auth/login");
    }

    /** Registers a fresh customer and returns its JWT. */
    public String registerAndGetToken(String email, String password) {
        return register(email, password).then().statusCode(201).extract().path("token");
    }

    public String adminToken() {
        return login(TestConfig.adminEmail(), TestConfig.adminPassword())
                .then().statusCode(200).extract().path("token");
    }
}
