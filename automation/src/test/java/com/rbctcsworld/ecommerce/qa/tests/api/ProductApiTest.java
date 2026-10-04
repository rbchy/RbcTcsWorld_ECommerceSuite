package com.rbctcsworld.ecommerce.qa.tests.api;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import com.rbctcsworld.ecommerce.qa.api.AuthClient;
import com.rbctcsworld.ecommerce.qa.api.ProductClient;
import com.rbctcsworld.ecommerce.qa.testdata.TestData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.containsStringIgnoringCase;

@Tag("api")
@Epic("Catalog")
@Feature("Products")
class ProductApiTest {

    private static final ProductClient products = new ProductClient();
    private static String adminToken;
    private static String customerToken;

    @BeforeAll
    static void tokens() {
        AuthClient auth = new AuthClient();
        adminToken = auth.adminToken();
        customerToken = auth.registerAndGetToken(TestData.uniqueEmail(), TestData.PASSWORD);
    }

    private static Map<String, Object> newProduct(String sku) {
        return ProductClient.body("QA Product " + sku, sku, "qa", new BigDecimal("15.00"), 10);
    }

    @Test
    @Tag("smoke")
    @DisplayName("Catalog is public and contains seeded products")
    void listProducts() {
        products.list().then().statusCode(200).body("size()", greaterThanOrEqualTo(8));
    }

    @Test
    @DisplayName("Search by name is case-insensitive")
    void search() {
        products.search("MOUSE").then().statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("name", everyItem(containsStringIgnoringCase("mouse")));
    }

    @Test
    @DisplayName("Unknown product -> 404 (was 500 before Module 0)")
    void missingProduct() {
        products.get(999_999).then().statusCode(404).body("status", equalTo(404));
    }

    @Test
    @DisplayName("Non-numeric id -> 400")
    void badId() {
        products.anonymousGet("/api/products/abc").then().statusCode(400);
    }

    @Test
    @DisplayName("SECURITY: anonymous create -> 401, customer create -> 403")
    void onlyAdminCanCreate() {
        products.create(null, newProduct(TestData.uniqueSku())).then().statusCode(401);
        products.create(customerToken, newProduct(TestData.uniqueSku())).then().statusCode(403);
    }

    @Test
    @DisplayName("Admin CRUD: create 201 -> update all fields 200 -> delete 204 -> get 404")
    void adminCrudLifecycle() {
        int id = products.create(adminToken, newProduct(TestData.uniqueSku()))
                .then().statusCode(201).extract().path("id");

        String newSku = TestData.uniqueSku();
        products.update(adminToken, id, ProductClient.body("Renamed", newSku, "books", new BigDecimal("99.99"), 7))
                .then().statusCode(200)
                .body("name", equalTo("Renamed"))
                .body("sku", equalTo(newSku))
                .body("category", equalTo("books"))
                .body("price", equalTo(99.99f))
                .body("stock", equalTo(7));

        products.delete(adminToken, id).then().statusCode(204);
        products.get(id).then().statusCode(404);
    }

    @Test
    @DisplayName("Duplicate SKU -> 409")
    void duplicateSku() {
        String sku = TestData.uniqueSku();
        products.create(adminToken, newProduct(sku)).then().statusCode(201);
        products.create(adminToken, newProduct(sku)).then().statusCode(409);
    }

    @Test
    @DisplayName("Negative price / negative stock -> 400")
    void invalidProduct() {
        products.create(adminToken, ProductClient.body("x", TestData.uniqueSku(), "qa", new BigDecimal("-1"), 1))
                .then().statusCode(400).body("fieldErrors.price", org.hamcrest.Matchers.notNullValue());
        products.create(adminToken, ProductClient.body("x", TestData.uniqueSku(), "qa", BigDecimal.ONE, -5))
                .then().statusCode(400).body("fieldErrors.stock", org.hamcrest.Matchers.notNullValue());
    }
}
