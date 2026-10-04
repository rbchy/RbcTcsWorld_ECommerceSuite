package com.rbctcsworld.ecommerce.qa.tests.api;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import com.rbctcsworld.ecommerce.qa.api.AdminClient;
import com.rbctcsworld.ecommerce.qa.api.CartClient;
import com.rbctcsworld.ecommerce.qa.api.CheckoutClient;
import com.rbctcsworld.ecommerce.qa.api.OrderClient;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures.Customer;
import com.rbctcsworld.ecommerce.qa.testdata.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/** Module 3 - price breakdown and coupon rules, through /api/checkout/quote and /api/orders. */
@Tag("api")
@Tag("checkout")
@Epic("Checkout")
@Feature("Pricing and coupons")
class CheckoutApiTest {

    private final CartClient cart = new CartClient();
    private final CheckoutClient checkout = new CheckoutClient();
    private final OrderClient orders = new OrderClient();
    private Customer customer;

    @BeforeEach
    void newCustomer() {
        customer = Fixtures.newCustomer();
    }

    private void cartWorth(String unitPrice, int qty) {
        long p = Fixtures.product(unitPrice, 20);
        cart.add(customer.token(), p, qty).then().statusCode(201);
    }

    @ParameterizedTest(name = "[{index}] cart {0} x{1} -> shipping {2}, tax {3}, total {4}")
    @CsvSource({
            // unit,  qty, shipping, tax,  total
            "49.99,   1,   5.99,     3.00, 58.98",   // 1 cent below free shipping
            "50.00,   1,   0.0,      3.00, 53.00",   // exactly at threshold
            "10.00,   2,   5.99,     1.20, 27.19",
            "0.10,    1,   5.99,     0.01,  6.10"    // tax rounding HALF_UP
    })
    @Tag("smoke")
    @DisplayName("Boundary: free shipping threshold and tax rounding")
    void priceBreakdown(String unit, int qty, float shipping, float tax, float total) {
        cartWorth(unit, qty);
        checkout.quote(customer.token(), null).then().statusCode(200)
                .body("couponCode", nullValue())
                .body("shippingFee", equalTo(shipping))
                .body("tax", equalTo(tax))
                .body("total", equalTo(total));
    }

    @Test
    @DisplayName("WELCOME10 (lower-case) on 60.00: discount 6.00 makes 54.00 -> free shipping")
    void percentCoupon() {
        cartWorth("30.00", 2);
        checkout.quote(customer.token(), "welcome10").then().statusCode(200)
                .body("couponCode", equalTo("WELCOME10"))
                .body("discount", equalTo(6.0f))
                .body("shippingFee", equalTo(0.0f))
                .body("tax", equalTo(3.24f))
                .body("total", equalTo(57.24f));
    }

    @Test
    @DisplayName("Boundary: SAVE5 needs 25.00 -> 24.99 is 400, 25.00 is OK")
    void minimumOrderAmount() {
        cartWorth("24.99", 1);
        checkout.quote(customer.token(), TestData.COUPON_SAVE5).then().statusCode(400)
                .body("message", containsString("Minimum order amount"));

        customer = Fixtures.newCustomer();
        cartWorth("25.00", 1);
        checkout.quote(customer.token(), TestData.COUPON_SAVE5).then().statusCode(200)
                .body("discount", equalTo(5.0f));
    }

    @ParameterizedTest(name = "{0} -> 400 ''{1}''")
    @CsvSource({
            "EXPIRED20,    expired",
            "FUTURE15,     not active yet",
            "DISABLED,     Invalid coupon code",
            "NO-SUCH-CODE, Invalid coupon code"
    })
    @DisplayName("Invalid, expired, future and disabled coupons -> 400")
    void invalidCoupons(String code, String message) {
        cartWorth("30.00", 1);
        checkout.quote(customer.token(), code).then().statusCode(400).body("message", containsString(message));
    }

    @Test
    @DisplayName("Quote is read-only: it never reserves stock or uses a coupon")
    void quoteHasNoSideEffects() {
        cartWorth("30.00", 1);
        checkout.quote(customer.token(), TestData.COUPON_WELCOME10).then().statusCode(200);
        checkout.quote(customer.token(), TestData.COUPON_WELCOME10).then().statusCode(200);   // still unused
        orders.place(customer.token(), TestData.COUPON_WELCOME10).then().statusCode(201)
                .body("couponCode", equalTo("WELCOME10"));
    }

    @Test
    @DisplayName("Coupon once per customer: second order with WELCOME10 -> 409, nothing changes")
    void oncePerCustomer() {
        cartWorth("30.00", 1);
        orders.place(customer.token(), TestData.COUPON_WELCOME10).then().statusCode(201).body("discount", equalTo(3.0f));

        cartWorth("30.00", 1);
        orders.place(customer.token(), TestData.COUPON_WELCOME10).then().statusCode(409)
                .body("message", containsString("already used"));
        cart.get(customer.token()).then().body("items.size()", equalTo(1));
    }

    @Test
    @DisplayName("Global usage limit: coupon with maxUses=1 works for the first customer only")
    void globalLimit() {
        String code = TestData.uniqueCouponCode();
        new AdminClient().createCoupon(Fixtures.adminToken(),
                        Map.of("code", code, "type", "FIXED", "value", 2.00, "maxUses", 1))
                .then().statusCode(201).body("code", equalTo(code));

        cartWorth("30.00", 1);
        orders.place(customer.token(), code).then().statusCode(201);

        customer = Fixtures.newCustomer();
        cartWorth("30.00", 1);
        orders.place(customer.token(), code).then().statusCode(409).body("message", containsString("usage limit"));
    }

    @Test
    @Tag("security")
    @DisplayName("SECURITY: only ADMIN can create coupons; invalid definitions are rejected")
    void couponAdministration() {
        AdminClient admin = new AdminClient();
        Map<String, Object> ok = Map.of("code", TestData.uniqueCouponCode(), "type", "FIXED", "value", 1);
        admin.createCoupon(null, ok).then().statusCode(401);
        admin.createCoupon(customer.token(), ok).then().statusCode(403);
        admin.createCoupon(Fixtures.adminToken(),
                Map.of("code", TestData.uniqueCouponCode(), "type", "PERCENT", "value", 150)).then().statusCode(400);
        admin.createCoupon(Fixtures.adminToken(),
                Map.of("code", TestData.uniqueCouponCode(), "type", "BOGUS", "value", 1)).then().statusCode(400)
                .body("fieldErrors.type", org.hamcrest.Matchers.notNullValue());
        admin.createCoupon(Fixtures.adminToken(),
                Map.of("code", TestData.COUPON_WELCOME10, "type", "FIXED", "value", 1)).then().statusCode(409);
    }
}
