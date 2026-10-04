package com.rbctcsworld.ecommerce.qa.tests.api;

import com.rbctcsworld.ecommerce.qa.api.ProductClient;
import com.rbctcsworld.ecommerce.qa.api.ReviewClient;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures.Customer;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
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

/**
 * Lost-update race on the rating summary: 8 verified buyers post their review at the same moment.
 * Without the product row lock each transaction would calculate the average without the others'
 * reviews, and the stored ratingCount / ratingAverage would be wrong.
 */
@Tag("api")
@Tag("concurrency")
@Epic("Customer engagement")
@Feature("Reviews and ratings")
class ReviewConcurrencyTest {

    private static final int[] STARS = {5, 5, 4, 4, 4, 3, 2, 1};   // sum 28 / 8 = 3.5

    @Test
    @DisplayName("8 simultaneous reviews: all saved, ratingCount 8, ratingAverage 3.5")
    void simultaneousReviews() throws Exception {
        ReviewClient reviews = new ReviewClient();
        long productId = Fixtures.product("5.00", 50);
        List<Customer> buyers = new ArrayList<>();
        for (int i = 0; i < STARS.length; i++) buyers.add(Fixtures.verifiedBuyer(productId));

        ExecutorService pool = Executors.newFixedThreadPool(STARS.length);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < STARS.length; i++) {
            Customer c = buyers.get(i);
            int stars = STARS[i];
            results.add(pool.submit(() -> {
                start.await();
                Response r = reviews.create(c.token(), productId, stars);
                return r.statusCode();
            }));
        }
        start.countDown();                                   // everyone presses "Submit" together
        int created = 0;
        for (Future<Integer> f : results) if (f.get(30, TimeUnit.SECONDS) == 201) created++;
        pool.shutdown();

        int expectedCreated = created;
        float average = new ProductClient().get(productId).then().extract().path("ratingAverage");
        int count = new ProductClient().get(productId).then().extract().path("ratingCount");
        assertAll(
                () -> assertEquals(STARS.length, expectedCreated, "every review accepted"),
                () -> assertEquals(STARS.length, count, "ratingCount"),
                () -> assertEquals(3.5f, average, 0.0001, "ratingAverage"));
    }
}
