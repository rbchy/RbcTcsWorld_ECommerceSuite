package com.rbctcsworld.ecommerce.qa.tests.api;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import com.rbctcsworld.ecommerce.qa.api.AdminClient;
import com.rbctcsworld.ecommerce.qa.api.OrderClient;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Module 4 - shipping, delivery, tracking timeline and public tracking. */
@Tag("api")
@Tag("fulfillment")
@Epic("Fulfillment")
@Feature("Shipping and tracking")
class FulfillmentApiTest {

    private final OrderClient orders = new OrderClient();
    private final AdminClient admin = new AdminClient();
    private Customer customer;
    private long productId;

    @BeforeEach
    void setUp() {
        customer = Fixtures.newCustomer();
        productId = Fixtures.product("10.00", 10);
    }

    @ParameterizedTest(name = "ship with {0}")
    @ValueSource(strings = {"UPS", "FEDEX", "USPS"})
    @DisplayName("Ship a PAID order: status SHIPPED, carrier-prefixed 12-digit tracking number")
    void ship(String carrier) {
        long id = Fixtures.paidOrder(customer, productId, 1);
        admin.ship(Fixtures.adminToken(), id, carrier).then().statusCode(200)
                .body("status", equalTo("SHIPPED"))
                .body("carrier", equalTo(carrier))
                .body("trackingNumber", matchesPattern(carrier + "-\\d{12}"))
                .body("shippedAt", notNullValue());
    }

    @Test
    @Tag("smoke")
    @DisplayName("Timeline lists every status change in order: PLACED, PAID, SHIPPED, DELIVERED")
    void timeline() {
        long id = Fixtures.deliveredOrder(customer, productId, 1);
        orders.tracking(customer.token(), id).then().statusCode(200)
                .body("status", equalTo("DELIVERED"))
                .body("events.status", contains("PLACED", "PAID", "SHIPPED", "DELIVERED"));
    }

    @Test
    @Tag("security")
    @DisplayName("SECURITY: public tracking works without login and leaks no personal or price data")
    void publicTracking() {
        long id = Fixtures.paidOrder(customer, productId, 1);
        String tn = admin.ship(Fixtures.adminToken(), id, "FEDEX").then().extract().path("trackingNumber");

        String body = orders.publicTracking(tn.toLowerCase()).then().statusCode(200)
                .body("status", equalTo("SHIPPED"))
                .body("trackingNumber", equalTo(tn))
                .extract().asString();
        assertAll(
                () -> assertFalse(body.contains(customer.email()), "no e-mail"),
                () -> assertFalse(body.contains("customerId"), "no customer id"),
                () -> assertFalse(body.contains("total"), "no prices"),
                () -> assertFalse(body.contains("unitPrice"), "no prices"));
        orders.publicTracking("UPS-000000000000").then().statusCode(404);
    }

    @Test
    @DisplayName("State rules: cancelled order cannot ship; deliver needs SHIPPED; no double shipping; shipped order cannot be cancelled")
    void stateRules() {
        long cancelled = Fixtures.paidOrder(customer, productId, 1);
        orders.cancel(customer.token(), cancelled).then().statusCode(200);
        admin.ship(Fixtures.adminToken(), cancelled, "UPS").then().statusCode(409)
                .body("message", containsString("CANCELLED"));

        long id = Fixtures.paidOrder(customer, productId, 1);
        admin.deliver(Fixtures.adminToken(), id).then().statusCode(409);
        admin.ship(Fixtures.adminToken(), id, "UPS").then().statusCode(200);
        admin.ship(Fixtures.adminToken(), id, "UPS").then().statusCode(409);
        orders.cancel(customer.token(), id).then().statusCode(409).body("message", containsString("SHIPPED"));
    }

    @Test
    @Tag("security")
    @DisplayName("SECURITY: only ADMIN ships; invalid carrier -> 400; another customer cannot see my tracking")
    void accessControl() {
        long id = Fixtures.paidOrder(customer, productId, 1);
        admin.ship(null, id, "UPS").then().statusCode(401);
        admin.ship(customer.token(), id, "UPS").then().statusCode(403);
        admin.ship(Fixtures.adminToken(), id, "DHL").then().statusCode(400).body("fieldErrors.carrier", notNullValue());
        orders.tracking(Fixtures.newCustomer().token(), id).then().statusCode(404);
    }
}
