package com.rbctcsworld.ecommerce.qa.api;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/** Sends any method/path/header combination - for security tests that must craft "bad" requests. */
public class RawClient extends ApiClient {

    public Response send(String method, String path, String token, Object body) {
        RequestSpecification spec = as(token);
        if (body != null) spec = spec.body(body);
        return spec.request(method, path);
    }

    public Response withAuthorizationHeader(String method, String path, String headerValue) {
        return anonymous().header("Authorization", headerValue).request(method, path);
    }

    public Response withHeader(String method, String path, String name, String value) {
        return anonymous().header(name, value).request(method, path);
    }
}
