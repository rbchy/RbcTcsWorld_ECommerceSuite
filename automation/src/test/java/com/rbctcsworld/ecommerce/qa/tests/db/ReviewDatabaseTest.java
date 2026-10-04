package com.rbctcsworld.ecommerce.qa.tests.db;

import com.rbctcsworld.ecommerce.qa.api.ReviewClient;
import com.rbctcsworld.ecommerce.qa.api.WishlistClient;
import com.rbctcsworld.ecommerce.qa.database.DatabaseUtils;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures.Customer;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Module 5 database checks: the denormalised rating summary must always match the review rows. */
@Tag("db")
@Epic("Data integrity")
@Feature("Reviews and wishlist")
class ReviewDatabaseTest {

    private final ReviewClient reviews = new ReviewClient();
    private final WishlistClient wishlist = new WishlistClient();

    @BeforeAll
    static void databaseReachable() {
        assumeTrue(DatabaseUtils.isAvailable(), "PostgreSQL not reachable - DB validation skipped");
    }

    @Test
    @DisplayName("For EVERY product, rating_count/rating_average equal a fresh calculation over PUBLISHED reviews")
    void ratingSummaryMatchesReviewsForAllProducts() {
        long p = Fixtures.product("10.00", 20);
        reviews.create(Fixtures.verifiedBuyer(p).token(), p, 5).then().statusCode(201);
        long hidden = reviews.create(Fixtures.verifiedBuyer(p).token(), p, 1).then().statusCode(201).extract().<Integer>path("id");
        reviews.hide(Fixtures.adminToken(), hidden).then().statusCode(200);

        // ROUND(numeric, 1) in PostgreSQL rounds half away from zero = Java HALF_UP for positive values
        List<Map<String, Object>> drift = DatabaseUtils.query("""
                select p.id, p.rating_count, p.rating_average, s.cnt, s.avg
                from products p
                left join (select product_id, count(*) cnt, round(avg(rating), 1) avg
                           from reviews where status = 'PUBLISHED' group by product_id) s on s.product_id = p.id
                where p.rating_count <> coalesce(s.cnt, 0) or p.rating_average <> coalesce(s.avg, 0)
                """);
        assertTrue(drift.isEmpty(), "products whose rating summary drifted: " + drift);

        Map<String, Object> row = DatabaseUtils.query("select rating_count, rating_average from products where id = ?", p).get(0);
        assertAll(
                () -> assertEquals(1, ((Number) row.get("rating_count")).intValue(), "hidden review not counted"),
                () -> assertEquals(0, new BigDecimal("5.0").compareTo((BigDecimal) row.get("rating_average"))));
    }

    @Test
    @DisplayName("Every review belongs to a customer who has a delivered/returned order with that product")
    void everyReviewIsAVerifiedPurchase() {
        long p = Fixtures.product("10.00", 20);
        reviews.create(Fixtures.verifiedBuyer(p).token(), p, 4).then().statusCode(201);

        Object unverified = DatabaseUtils.scalar("""
                select count(*) from reviews r
                where not exists (select 1 from orders o join order_items i on i.order_id = o.id
                                  where o.user_id = r.user_id and i.product_id = r.product_id
                                    and o.status in ('DELIVERED', 'RETURN_REQUESTED', 'RETURNED'))
                """);
        assertEquals(0, ((Number) unverified).intValue());
    }

    @Test
    @DisplayName("Schema enforces the rules even without the API: unique (user, product) and rating 1..5")
    void constraintsExist() {
        // read-only check of the schema (the automation never writes to the database directly)
        List<Map<String, Object>> rows = DatabaseUtils.query(
                "select conname, pg_get_constraintdef(oid) def from pg_constraint where conrelid = 'reviews'::regclass");
        String all = rows.toString();
        assertAll(
                () -> assertTrue(all.contains("uk_reviews_user_product") && all.contains("UNIQUE (user_id, product_id)"), all),
                () -> assertTrue(all.contains("rating >= 1") && all.contains("rating <= 5"), all));
    }

    @Test
    @DisplayName("Wishlist row keeps the price at the time of adding; one row per customer and product")
    void wishlistRow() {
        Customer c = Fixtures.newCustomer();
        long p = Fixtures.product("12.34", 3);
        wishlist.add(c.token(), p).then().statusCode(201);
        wishlist.add(c.token(), p).then().statusCode(200);

        List<Map<String, Object>> rows = DatabaseUtils.query("select price_when_added from wishlist_items where product_id = ?", p);
        assertAll(
                () -> assertEquals(1, rows.size()),
                () -> assertEquals(0, new BigDecimal("12.34").compareTo((BigDecimal) rows.get(0).get("price_when_added"))));
    }
}
