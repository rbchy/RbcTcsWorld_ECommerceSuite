package com.rbctcsworld.ecommerce.qa.testdata;

import com.rbctcsworld.ecommerce.qa.api.AdminClient;
import com.rbctcsworld.ecommerce.qa.api.AuthClient;
import com.rbctcsworld.ecommerce.qa.api.CartClient;
import com.rbctcsworld.ecommerce.qa.api.OrderClient;
import com.rbctcsworld.ecommerce.qa.api.ProductClient;

import java.math.BigDecimal;

/** Builds test preconditions through the public API (no database shortcuts). */
public final class Fixtures {

    private static final AuthClient AUTH = new AuthClient();
    private static final ProductClient PRODUCTS = new ProductClient();
    private static final CartClient CART = new CartClient();
    private static final OrderClient ORDERS = new OrderClient();
    private static final AdminClient ADMIN = new AdminClient();
    private static String adminToken;

    private Fixtures() {
    }

    public record Customer(String email, String token) {
    }

    public static synchronized String adminToken() {
        if (adminToken == null) {
            adminToken = AUTH.adminToken();
        }
        return adminToken;
    }

    public static Customer newCustomer() {
        String email = TestData.uniqueEmail();
        return new Customer(email, AUTH.registerAndGetToken(email, TestData.PASSWORD));
    }

    /** New product with a known price and stock, so totals and stock maths are predictable. */
    public static long product(String price, int stock) {
        int id = PRODUCTS.create(adminToken(),
                        ProductClient.body("QA Item", TestData.uniqueSku(), "qa", new BigDecimal(price), stock))
                .then().statusCode(201).extract().path("id");
        return id;
    }

    /** Customer orders {@code qty} of the product and pays with the approved test card. Returns the order id. */
    public static long paidOrder(Customer c, long productId, int qty) {
        CART.add(c.token(), productId, qty).then().statusCode(201);
        int id = ORDERS.place(c.token()).then().statusCode(201).extract().path("id");
        ORDERS.pay(c.token(), id, TestData.CARD_OK).then().statusCode(200);
        return id;
    }

    /** paidOrder + warehouse ships (UPS) and marks delivered. */
    public static long deliveredOrder(Customer c, long productId, int qty) {
        long id = paidOrder(c, productId, qty);
        ADMIN.ship(adminToken(), id, "UPS").then().statusCode(200);
        ADMIN.deliver(adminToken(), id).then().statusCode(200);
        return id;
    }
}
