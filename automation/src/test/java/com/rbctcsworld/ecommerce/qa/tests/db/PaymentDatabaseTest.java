package com.rbctcsworld.ecommerce.qa.tests.db;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import com.rbctcsworld.ecommerce.qa.api.CartClient;
import com.rbctcsworld.ecommerce.qa.api.OrderClient;
import com.rbctcsworld.ecommerce.qa.database.DatabaseUtils;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures.Customer;
import com.rbctcsworld.ecommerce.qa.testdata.TestData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Module 3 database checks, including card-data security (PCI-DSS principle: never store the full PAN or CVV). */
@Tag("db")
@Tag("security")
@Epic("Data integrity")
@Feature("Payments and card-data security")
class PaymentDatabaseTest {

    private final CartClient cart = new CartClient();
    private final OrderClient orders = new OrderClient();

    @BeforeAll
    static void databaseReachable() {
        assumeTrue(DatabaseUtils.isAvailable(), "PostgreSQL not reachable - DB validation skipped");
    }

    @Test
    @DisplayName("No table anywhere has a column that could hold a card number or CVV")
    void noCardDataColumns() {
        Object count = DatabaseUtils.scalar("select count(*) from information_schema.columns "
                + "where table_schema = 'public' and lower(column_name) in ('card_number', 'cardnumber', 'pan', 'cvv', 'cvc', 'card_cvv')");
        assertEquals(0, ((Number) count).intValue());
    }

    @Test
    @DisplayName("Stored card reference is exactly 4 digits - never the full number")
    void onlyLast4Stored() {
        Object bad = DatabaseUtils.scalar(
                "select count(*) from payment_transactions where card_last4 is not null and card_last4 !~ '^[0-9]{4}$'");
        assertEquals(0, ((Number) bad).intValue());
    }

    @Test
    @DisplayName("Pay + cancel: orders.paid_at set, CHARGE + REFUND rows, coupon redemption row, totals consistent")
    void paymentLifecycleRows() {
        Customer c = Fixtures.newCustomer();
        long p = Fixtures.product("30.00", 10);
        cart.add(c.token(), p, 2).then().statusCode(201);                         // 60.00
        int id = orders.place(c.token(), TestData.COUPON_WELCOME10).then().statusCode(201).extract().path("id");
        orders.pay(c.token(), id, TestData.CARD_OK).then().statusCode(200);

        Map<String, Object> order = DatabaseUtils.query(
                "select status, subtotal, discount, shipping_fee, tax, total, coupon_code, paid_at from orders where id = ?", id).get(0);
        assertAll(
                () -> assertEquals("PAID", order.get("status")),
                () -> assertNotNull(order.get("paid_at")),
                () -> assertEquals("WELCOME10", order.get("coupon_code")),
                () -> assertEquals(0, new BigDecimal("57.24").compareTo((BigDecimal) order.get("total"))),
                () -> assertEquals(0, ((BigDecimal) order.get("subtotal")).subtract((BigDecimal) order.get("discount"))
                        .add((BigDecimal) order.get("shipping_fee")).add((BigDecimal) order.get("tax"))
                        .compareTo((BigDecimal) order.get("total")), "total = subtotal - discount + shipping + tax"),
                () -> assertEquals(1, ((Number) DatabaseUtils.scalar(
                        "select count(*) from coupon_redemptions where order_id = ?", id)).intValue()));

        orders.cancel(c.token(), id).then().statusCode(200);
        List<Map<String, Object>> tx = DatabaseUtils.query(
                "select type, status, amount from payment_transactions where order_id = ? order by id", id);
        assertAll(
                () -> assertEquals(2, tx.size()),
                () -> assertEquals("CHARGE", tx.get(0).get("type")),
                () -> assertEquals("REFUND", tx.get(1).get("type")),
                () -> assertEquals(0, ((BigDecimal) tx.get(0).get("amount")).compareTo((BigDecimal) tx.get(1).get("amount")),
                        "refund equals charge"));
    }

    @Test
    @DisplayName("Declined payment leaves a FAILED row and the order unpaid")
    void declinedAttemptPersisted() {
        Customer c = Fixtures.newCustomer();
        long p = Fixtures.product("10.00", 10);
        cart.add(c.token(), p, 1).then().statusCode(201);
        int id = orders.place(c.token()).then().statusCode(201).extract().path("id");
        orders.pay(c.token(), id, TestData.CARD_DECLINED).then().statusCode(402);

        Map<String, Object> row = DatabaseUtils.query(
                "select status, failure_reason, card_last4 from payment_transactions where order_id = ?", id).get(0);
        assertAll(
                () -> assertEquals("FAILED", row.get("status")),
                () -> assertEquals("Card declined", row.get("failure_reason")),
                () -> assertEquals("0002", row.get("card_last4")),
                () -> assertEquals("PLACED", DatabaseUtils.scalar("select status from orders where id = ?", id)));
    }
}
