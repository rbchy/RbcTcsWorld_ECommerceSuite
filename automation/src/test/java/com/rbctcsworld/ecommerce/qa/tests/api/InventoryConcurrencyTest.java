package com.rbctcsworld.ecommerce.qa.tests.api;

import com.rbctcsworld.ecommerce.qa.api.CartClient;
import com.rbctcsworld.ecommerce.qa.api.OrderClient;
import com.rbctcsworld.ecommerce.qa.api.ProductClient;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures.Customer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * "Flash sale" race condition against the real backend + PostgreSQL:
 * 10 customers press "Place order" at the same moment for a product with only 5 units.
 */
@Tag("api")
@Tag("concurrency")
class InventoryConcurrencyTest {

    private static final int BUYERS = 10;
    private static final int STOCK = 5;

    @Test
    @DisplayName("No overselling: exactly 5 of 10 simultaneous orders succeed, the rest get 409, stock ends at 0")
    void flashSale() throws Exception {
        CartClient cart = new CartClient();
        OrderClient orders = new OrderClient();
        long productId = Fixtures.product("1.00", STOCK);

        List<Customer> buyers = new ArrayList<>();
        for (int i = 0; i < BUYERS; i++) {
            Customer c = Fixtures.newCustomer();
            cart.add(c.token(), productId, 1).then().statusCode(201);
            buyers.add(c);
        }

        ExecutorService pool = Executors.newFixedThreadPool(BUYERS);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> statuses = new ArrayList<>();
        for (Customer c : buyers) {
            statuses.add(pool.submit(() -> {
                go.await();
                return orders.place(c.token()).statusCode();
            }));
        }
        go.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS), "requests finished");

        int created = 0;
        int conflict = 0;
        for (Future<Integer> f : statuses) {
            int s = f.get();
            if (s == 201) created++;
            else if (s == 409) conflict++;
        }
        int finalStock = new ProductClient().get(productId).then().extract().path("stock");

        final int c201 = created;
        final int c409 = conflict;
        assertAll(
                () -> assertEquals(STOCK, c201, "orders created"),
                () -> assertEquals(BUYERS - STOCK, c409, "orders rejected with 409"),
                () -> assertEquals(0, finalStock, "final stock"));
    }
}
