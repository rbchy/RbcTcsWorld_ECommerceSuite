package com.rbctcsworld.ecommerce.qa.tests.db;

import com.rbctcsworld.ecommerce.qa.api.AdminClient;
import com.rbctcsworld.ecommerce.qa.api.OrderClient;
import com.rbctcsworld.ecommerce.qa.database.DatabaseUtils;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures.Customer;
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

/** Module 4 - after a full lifecycle, every table tells the same consistent story. */
@Tag("db")
class LifecycleDatabaseTest {

    @BeforeAll
    static void databaseReachable() {
        assumeTrue(DatabaseUtils.isAvailable(), "PostgreSQL not reachable - DB validation skipped");
    }

    @Test
    @DisplayName("Place -> pay -> ship -> deliver -> return -> approve: events, return row, refund and restock agree")
    void fullLifecycleIsConsistent() {
        Customer c = Fixtures.newCustomer();
        long productId = Fixtures.product("10.00", 10);
        long orderId = Fixtures.deliveredOrder(c, productId, 2);
        OrderClient orders = new OrderClient();
        int returnId = orders.requestReturn(c.token(), orderId, "WRONG_ITEM").then().statusCode(201).extract().path("id");
        new AdminClient().approveReturn(Fixtures.adminToken(), returnId).then().statusCode(200);

        List<Object> events = DatabaseUtils.query(
                "select status from order_events where order_id = ? order by id", orderId)
                .stream().map(r -> r.get("status")).toList();
        Map<String, Object> order = DatabaseUtils.query(
                "select status, tracking_number, shipped_at, delivered_at, total from orders where id = ?", orderId).get(0);
        Map<String, Object> ret = DatabaseUtils.query(
                "select status, refund_amount, restocked, resolved_at from return_requests where order_id = ?", orderId).get(0);
        Object refundTx = DatabaseUtils.scalar(
                "select amount from payment_transactions where order_id = ? and type = 'REFUND'", orderId);
        Object restockQty = DatabaseUtils.scalar(
                "select sum(change_qty) from stock_movements where order_id = ? and reason = 'RETURN_RESTOCK'", orderId);
        Object stock = DatabaseUtils.scalar("select stock from products where id = ?", productId);

        assertAll(
                () -> assertEquals(List.of("PLACED", "PAID", "SHIPPED", "DELIVERED", "RETURN_REQUESTED", "RETURNED"), events),
                () -> assertEquals("RETURNED", order.get("status")),
                () -> assertNotNull(order.get("tracking_number")),
                () -> assertNotNull(order.get("shipped_at")),
                () -> assertNotNull(order.get("delivered_at")),
                () -> assertEquals("APPROVED", ret.get("status")),
                () -> assertEquals(Boolean.TRUE, ret.get("restocked")),
                () -> assertNotNull(ret.get("resolved_at")),
                () -> assertEquals(0, ((BigDecimal) ret.get("refund_amount")).compareTo((BigDecimal) refundTx),
                        "return refund == payment refund"),
                () -> assertEquals(0, ((BigDecimal) order.get("total")).compareTo((BigDecimal) refundTx),
                        "WRONG_ITEM refunds the full total"),
                () -> assertEquals(2, ((Number) restockQty).intValue()),
                () -> assertEquals(10, ((Number) stock).intValue()));
    }

    @Test
    @DisplayName("Every order has a timeline, and its last event matches the order's current status")
    void timelineMatchesStatus() {
        Object mismatches = DatabaseUtils.scalar("""
                select count(*) from orders o
                where o.status <> (select e.status from order_events e where e.order_id = o.id order by e.id desc limit 1)
                   or not exists (select 1 from order_events e where e.order_id = o.id)""");
        assertEquals(0, ((Number) mismatches).intValue());
    }
}
