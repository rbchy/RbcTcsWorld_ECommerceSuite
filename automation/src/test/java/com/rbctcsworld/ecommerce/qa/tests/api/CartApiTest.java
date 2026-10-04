package com.rbctcsworld.ecommerce.qa.tests.api;

import com.rbctcsworld.ecommerce.qa.api.AuthClient;
import com.rbctcsworld.ecommerce.qa.api.CartClient;
import com.rbctcsworld.ecommerce.qa.api.ProductClient;
import com.rbctcsworld.ecommerce.qa.testdata.TestData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/** Module 1 - shopping cart. Each test uses a brand-new customer, so tests are independent. */
@Tag("api")
@Tag("cart")
class CartApiTest {

    private static final AuthClient auth = new AuthClient();
    private static final ProductClient products = new ProductClient();
    private final CartClient cart = new CartClient();

    private static String adminToken;
    private static long mouseId;
    private String token;

    @BeforeAll
    static void lookups() {
        adminToken = auth.adminToken();
        mouseId = products.idBySku(TestData.SKU_MOUSE);
    }

    @BeforeEach
    void newCustomer() {
        token = auth.registerAndGetToken(TestData.uniqueEmail(), TestData.PASSWORD);
    }

    /** Creates a product with a known price and stock so totals are predictable. */
    private long productWithStock(int stock) {
        int id = products.create(adminToken,
                        ProductClient.body("Cart QA", TestData.uniqueSku(), "qa", new BigDecimal("10.00"), stock))
                .then().statusCode(201).extract().path("id");
        return id;
    }

    @Test
    @Tag("smoke")
    @DisplayName("New customer has an empty cart")
    void emptyCart() {
        cart.get(token).then().statusCode(200)
                .body("items", hasSize(0))
                .body("totalQuantity", equalTo(0))
                .body("subtotal", equalTo(0));
    }

    @Test
    @DisplayName("Add -> merge same product -> update -> remove; totals correct at every step")
    void fullLineLifecycle() {
        long productId = productWithStock(20);

        cart.add(token, productId, 2).then().statusCode(201)
                .body("items", hasSize(1))
                .body("items[0].quantity", equalTo(2))
                .body("subtotal", equalTo(20.0f));

        int itemId = cart.add(token, productId, 1).then().statusCode(201)
                .body("items", hasSize(1))                 // merged, not duplicated
                .body("items[0].quantity", equalTo(3))
                .body("items[0].lineTotal", equalTo(30.0f))
                .extract().path("items[0].itemId");

        cart.update(token, itemId, 5).then().statusCode(200)
                .body("totalQuantity", equalTo(5))
                .body("subtotal", equalTo(50.0f));

        cart.remove(token, itemId).then().statusCode(200).body("items", hasSize(0));
    }

    @Test
    @DisplayName("Two different products -> two lines, subtotal is the sum")
    void twoProducts() {
        long a = productWithStock(10);
        long b = productWithStock(10);
        cart.add(token, a, 1).then().statusCode(201);
        cart.add(token, b, 2).then().statusCode(201)
                .body("items", hasSize(2))
                .body("totalQuantity", equalTo(3))
                .body("subtotal", equalTo(30.0f));
    }

    @Test
    @DisplayName("Boundary: quantity equal to stock is allowed, stock + 1 is 409")
    void stockBoundary() {
        long productId = productWithStock(3);
        cart.add(token, productId, 4).then().statusCode(409)
                .body("message", containsString("available 3"));
        cart.add(token, productId, 3).then().statusCode(201);
    }

    @Test
    @DisplayName("Merge that would exceed stock is rejected and cart is unchanged")
    void mergeAboveStock() {
        long productId = productWithStock(3);
        cart.add(token, productId, 2).then().statusCode(201);
        cart.add(token, productId, 2).then().statusCode(409);
        cart.get(token).then().body("items[0].quantity", equalTo(2));
    }

    @ParameterizedTest(name = "quantity {0} -> 400")
    @ValueSource(ints = {0, -1, 11})
    @DisplayName("Boundary: quantity outside 1..10 -> 400")
    void invalidQuantity(int qty) {
        cart.add(token, mouseId, qty).then().statusCode(400);
    }

    @Test
    @DisplayName("Missing productId or quantity -> 400")
    void missingFields() {
        cart.add(token, null, 1).then().statusCode(400).body("fieldErrors.productId", org.hamcrest.Matchers.notNullValue());
        cart.add(token, mouseId, null).then().statusCode(400).body("fieldErrors.quantity", org.hamcrest.Matchers.notNullValue());
    }

    @Test
    @DisplayName("Unknown or deleted product -> 404")
    void unknownOrDeletedProduct() {
        cart.add(token, 999_999L, 1).then().statusCode(404);

        long productId = productWithStock(5);
        products.delete(adminToken, productId).then().statusCode(204);
        cart.add(token, productId, 1).then().statusCode(404);
    }

    @Test
    @DisplayName("Product deleted after being added -> line shows available=false")
    void lineBecomesUnavailable() {
        long productId = productWithStock(5);
        cart.add(token, productId, 1).then().statusCode(201).body("items[0].available", equalTo(true));
        products.delete(adminToken, productId).then().statusCode(204);
        cart.get(token).then().statusCode(200).body("items[0].available", equalTo(false));
    }

    @Test
    @Tag("security")
    @DisplayName("SECURITY: no token or forged token -> 401")
    void requiresAuthentication() {
        cart.get(null).then().statusCode(401);
        cart.get("forged.jwt.token").then().statusCode(401);
        cart.add(null, mouseId, 1).then().statusCode(401);
    }

    @Test
    @Tag("security")
    @DisplayName("SECURITY (IDOR): another customer's cart item cannot be changed or deleted")
    void idorProtection() {
        int aliceItem = cart.add(token, mouseId, 1).then().statusCode(201).extract().path("items[0].itemId");
        String bob = auth.registerAndGetToken(TestData.uniqueEmail(), TestData.PASSWORD);

        cart.update(bob, aliceItem, 5).then().statusCode(404);
        cart.remove(bob, aliceItem).then().statusCode(404);

        cart.get(token).then().body("items[0].quantity", equalTo(1));
        cart.get(bob).then().body("items", hasSize(0));
    }

    @Test
    @DisplayName("Clear cart -> 204 and cart is empty")
    void clearCart() {
        cart.add(token, mouseId, 1).then().statusCode(201);
        cart.clear(token).then().statusCode(204);
        cart.get(token).then().body("items", hasSize(0));
    }
}
