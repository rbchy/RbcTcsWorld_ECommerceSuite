package com.rbctcsworld.ecommerce.qa.tests.api;

import com.rbctcsworld.ecommerce.qa.api.AdminClient;
import com.rbctcsworld.ecommerce.qa.api.OrderClient;
import com.rbctcsworld.ecommerce.qa.api.ProductClient;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Module 4 - returns. Order used everywhere: 2 x 10.00 = 20.00 + 5.99 shipping + 1.20 tax = 27.19.
 */
@Tag("api")
@Tag("returns")
class ReturnApiTest {

    private final OrderClient orders = new OrderClient();
    private final AdminClient admin = new AdminClient();
    private final ProductClient products = new ProductClient();
    private Customer customer;
    private long productId;

    @BeforeEach
    void setUp() {
        customer = Fixtures.newCustomer();
        productId = Fixtures.product("10.00", 10);
    }

    private int stock() {
        return products.get(productId).then().extract().path("stock");
    }

    private long requested(long orderId, String reason) {
        int id = orders.requestReturn(customer.token(), orderId, reason).then().statusCode(201)
                .body("status", equalTo("REQUESTED")).extract().path("id");
        return id;
    }

    @ParameterizedTest(name = "{0}: refund {1}, restocked {2}, stock afterwards {3}")
    @CsvSource({
            "DAMAGED,          27.19, false, 8",
            "WRONG_ITEM,       27.19, true,  10",
            "NOT_AS_DESCRIBED, 27.19, true,  10",
            "NO_LONGER_NEEDED, 21.20, true,  10"
    })
    @Tag("smoke")
    @DisplayName("Decision table: refund amount and restock depend on the return reason")
    void decisionTable(String reason, float refund, boolean restocked, int stockAfter) {
        long orderId = Fixtures.deliveredOrder(customer, productId, 2);
        long returnId = requested(orderId, reason);
        orders.get(customer.token(), orderId).then().body("status", equalTo("RETURN_REQUESTED"));

        admin.approveReturn(Fixtures.adminToken(), returnId).then().statusCode(200)
                .body("status", equalTo("APPROVED"))
                .body("refundAmount", equalTo(refund))
                .body("restocked", equalTo(restocked))
                .body("resolvedAt", notNullValue());

        orders.get(customer.token(), orderId).then().body("status", equalTo("RETURNED"));
        orders.payments(customer.token(), orderId).then()
                .body("type", contains("CHARGE", "REFUND"))
                .body("[1].amount", equalTo(refund));
        org.junit.jupiter.api.Assertions.assertEquals(stockAfter, stock());
    }

    @Test
    @DisplayName("Only DELIVERED orders can be returned (PAID / SHIPPED -> 409)")
    void notDeliveredYet() {
        long paid = Fixtures.paidOrder(customer, productId, 1);
        orders.requestReturn(customer.token(), paid, "DAMAGED").then().statusCode(409);
        admin.ship(Fixtures.adminToken(), paid, "UPS").then().statusCode(200);
        orders.requestReturn(customer.token(), paid, "DAMAGED").then().statusCode(409);
    }

    @Test
    @DisplayName("Invalid reason -> 400; second request for the same order -> 409")
    void validationAndDuplicates() {
        long orderId = Fixtures.deliveredOrder(customer, productId, 1);
        orders.requestReturn(customer.token(), orderId, "BROKEN").then().statusCode(400)
                .body("fieldErrors.reason", notNullValue());
        requested(orderId, "WRONG_ITEM");
        orders.requestReturn(customer.token(), orderId, "WRONG_ITEM").then().statusCode(409);
        orders.getReturn(customer.token(), orderId).then().statusCode(200).body("reason", equalTo("WRONG_ITEM"));
    }

    @Test
    @DisplayName("Rejected return: order back to DELIVERED, no refund, no restock, cannot be approved later")
    void rejection() {
        long orderId = Fixtures.deliveredOrder(customer, productId, 2);
        long returnId = requested(orderId, "NO_LONGER_NEEDED");

        admin.rejectReturn(Fixtures.adminToken(), returnId, "Item was used").then().statusCode(200)
                .body("status", equalTo("REJECTED")).body("adminNote", equalTo("Item was used"));

        orders.get(customer.token(), orderId).then().body("status", equalTo("DELIVERED"));
        orders.payments(customer.token(), orderId).then().body("type", contains("CHARGE"));
        org.junit.jupiter.api.Assertions.assertEquals(8, stock());
        admin.approveReturn(Fixtures.adminToken(), returnId).then().statusCode(409);
    }

    @Test
    @DisplayName("Admin can filter returns by status")
    void adminList() {
        long orderId = Fixtures.deliveredOrder(customer, productId, 1);
        long returnId = requested(orderId, "DAMAGED");
        admin.returns(Fixtures.adminToken(), "REQUESTED").then().statusCode(200)
                .body("id", hasItem((int) returnId));
    }

    @Test
    @Tag("security")
    @DisplayName("SECURITY: customers cannot approve returns or return someone else's order")
    void accessControl() {
        long orderId = Fixtures.deliveredOrder(customer, productId, 1);
        Customer intruder = Fixtures.newCustomer();
        orders.requestReturn(intruder.token(), orderId, "DAMAGED").then().statusCode(404);

        long returnId = requested(orderId, "DAMAGED");
        admin.approveReturn(customer.token(), returnId).then().statusCode(403);
        admin.returns(customer.token(), null).then().statusCode(403);
    }
}
