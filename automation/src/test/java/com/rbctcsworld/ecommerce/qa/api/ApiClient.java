package com.rbctcsworld.ecommerce.qa.api;

import com.rbctcsworld.ecommerce.qa.config.TestConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

/** Base for all API clients: base URL, JSON, optional bearer token, request/response logged on failure. */
public abstract class ApiClient {

    protected RequestSpecification anonymous() {
        return given().spec(new RequestSpecBuilder()
                .setBaseUri(TestConfig.baseUrl())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build())
                .log().ifValidationFails(LogDetail.ALL);
    }

    protected RequestSpecification as(String token) {
        RequestSpecification spec = anonymous();
        return token == null ? spec : spec.header("Authorization", "Bearer " + token);
    }
}
