package com.rbctcsworld.ecommerce.qa.tests.api;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import com.rbctcsworld.ecommerce.qa.api.AdminClient;
import com.rbctcsworld.ecommerce.qa.api.CartClient;
import com.rbctcsworld.ecommerce.qa.api.OrderClient;
import com.rbctcsworld.ecommerce.qa.api.ProductClient;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;

/** Module 2 - orders + inventory, verified through the API only (see OrderDatabaseTest for DB checks). */
@Tag("api")
@Tag("order")
@Epic("Orders")
@Feature("Order placement")
class OrderApiTest {

    private final CartClient cart = new CartClient();
    private final OrderClient orders = new OrderClient();
    private final ProductClient products = new ProductClient();
    private final AdminClient admin = new AdminClient();
    private Customer customer;

    @BeforeEach
    void newCustomer() {
        customer = Fixtures.newCustomer();
    }

    private int stockOf(long productId) {
        return products.get(productId).then().statusCode(200).extract().path("stock");
    }

    private long placeOrder(Customer c) {
        int id = orders.place(c.token()).then().statusCode(201).extract().path("id");
        return id;
    }

    @Test
    @Tag("smoke")
    @DisplayName("Place order: 201, totals correct, stock reduced, cart emptied, order in history")
    void placeOrderHappyPath() {
        long a = Fixtures.product("10.00", 5);
        long b = Fixtures.product("2.50", 10);
        cart.add(customer.token(), a, 2).then().statusCode(201);
        cart.add(customer.token(), b, 4).then().statusCode(201);

        int orderId = orders.place(customer.token()).then().statusCode(201)
                .body("orderNumber", matchesPattern("ORD-\\d{8}-[A-Z2-9]{6}"))
                .body("status", equalTo("PLACED"))
                .body("items", hasSize(2))
                .body("totalQuantity", equalTo(6))
                .body("subtotal", equalTo(30.0f))
                .body("createdAt", notNullValue())
                .extract().path("id");

        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.junit.jupiter.api.Assertions.assertEquals(3, stockOf(a)),
                () -> org.junit.jupiter.api.Assertions.assertEquals(6, stockOf(b)));
        cart.get(customer.token()).then().body("items", hasSize(0));
        orders.mine(customer.token()).then().statusCode(200).body("[0].id", equalTo(orderId));
    }

    @Test
    @DisplayName("Empty cart -> 400 'Cart is empty'")
    void emptyCart() {
        orders.place(customer.token()).then().statusCode(400).body("message", equalTo("Cart is empty"));
    }

    @Test
    @DisplayName("Atomicity: if one line lacks stock, nothing is reserved and the cart is kept")
    void allOrNothing() {
        long plenty = Fixtures.product("1.00", 50);
        long scarce = Fixtures.product("1.00", 2);
        cart.add(customer.token(), plenty, 3).then().statusCode(201);
        cart.add(customer.token(), scarce, 2).then().statusCode(201);

        Customer rival = Fixtures.newCustomer();               // buys the scarce stock first
        cart.add(rival.token(), scarce, 2).then().statusCode(201);
        placeOrder(rival);

        orders.place(customer.token()).then().statusCode(409).body("message", containsString("Insufficient stock"));
        org.junit.jupiter.api.Assertions.assertEquals(50, stockOf(plenty), "no partial reservation");
        cart.get(customer.token()).then().body("items", hasSize(2));
        orders.mine(customer.token()).then().body("$", hasSize(0));
    }

    @Test
    @DisplayName("Price snapshot: later price change does not alter an existing order")
    void priceSnapshot() {
        long p = Fixtures.product("10.00", 5);
        cart.add(customer.token(), p, 1).then().statusCode(201);
        long orderId = placeOrder(customer);

        products.update(Fixtures.adminToken(), p, ProductClient.body("QA Item", com.rbctcsworld.ecommerce.qa.testdata.TestData.uniqueSku(),
                "qa", new java.math.BigDecimal("999.00"), 4)).then().statusCode(200);

        orders.get(customer.token(), orderId).then().statusCode(200)
                .body("items[0].unitPrice", equalTo(10.0f))
                .body("subtotal", equalTo(10.0f));
    }

    @Test
    @DisplayName("Cancel: 200, stock restored; second cancel -> 409 and stock not restored twice")
    void cancelRestoresStockOnce() {
        long p = Fixtures.product("5.00", 4);
        cart.add(customer.token(), p, 3).then().statusCode(201);
        long orderId = placeOrder(customer);
        org.junit.jupiter.api.Assertions.assertEquals(1, stockOf(p));

        orders.cancel(customer.token(), orderId).then().statusCode(200)
                .body("status", equalTo("CANCELLED"))
                .body("cancelledAt", notNullValue());
        org.junit.jupiter.api.Assertions.assertEquals(4, stockOf(p));

        orders.cancel(customer.token(), orderId).then().statusCode(409);
        org.junit.jupiter.api.Assertions.assertEquals(4, stockOf(p));

        admin.stockMovements(Fixtures.adminToken(), p).then().statusCode(200)
                .body("changeQty", org.hamcrest.Matchers.contains(-3, 3))
                .body("reason", org.hamcrest.Matchers.contains("ORDER_PLACED", "ORDER_CANCELLED"));
    }

    @Test
    @Tag("security")
    @DisplayName("SECURITY (IDOR): another customer cannot read or cancel my order")
    void idor() {
        long p = Fixtures.product("1.00", 5);
        cart.add(customer.token(), p, 1).then().statusCode(201);
        long orderId = placeOrder(customer);

        Customer intruder = Fixtures.newCustomer();
        orders.get(intruder.token(), orderId).then().statusCode(404);
        orders.cancel(intruder.token(), orderId).then().statusCode(404);
        orders.get(customer.token(), orderId).then().statusCode(200).body("status", equalTo("PLACED"));
    }

    @Test
    @Tag("security")
    @DisplayName("SECURITY (RBAC): admin endpoints -> 401 anonymous, 403 customer, 200 admin")
    void adminOnly() {
        admin.allOrders(null).then().statusCode(401);
        admin.allOrders(customer.token()).then().statusCode(403);
        admin.allOrders(Fixtures.adminToken()).then().statusCode(200);
    }

    @Test
    @Tag("security")
    @DisplayName("SECURITY: orders require login")
    void requiresLogin() {
        orders.place(null).then().statusCode(401);
        orders.mine(null).then().statusCode(401);
    }
}
