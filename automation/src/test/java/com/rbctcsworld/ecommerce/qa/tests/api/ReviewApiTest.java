package com.rbctcsworld.ecommerce.qa.tests.api;

import com.rbctcsworld.ecommerce.qa.api.ProductClient;
import com.rbctcsworld.ecommerce.qa.api.ReviewClient;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures.Customer;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;


import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/** Module 5 - product reviews and ratings. */
@Tag("api")
@Tag("reviews")
@Epic("Customer engagement")
@Feature("Reviews and ratings")
class ReviewApiTest {

    private final ReviewClient reviews = new ReviewClient();
    private final ProductClient products = new ProductClient();
    private long productId;

    @BeforeEach
    void setUp() {
        productId = Fixtures.product("15.00", 50);
    }

    // ---------- who may review ----------

    @ParameterizedTest(name = "order {0} -> HTTP {1}")
    @CsvSource({"NONE, 403", "PLACED, 403", "PAID, 403", "SHIPPED, 403", "DELIVERED, 201"})
    @Tag("smoke")
    @DisplayName("Verified purchase: only a customer who RECEIVED the product may review it")
    void verifiedPurchaseMatrix(String orderState, int expected) {
        Customer c = Fixtures.newCustomer();
        switch (orderState) {
            case "PLACED" -> Fixtures.placedOrder(c, productId, 1);
            case "PAID" -> Fixtures.paidOrder(c, productId, 1);
            case "SHIPPED" -> Fixtures.shippedOrder(c, productId, 1);
            case "DELIVERED" -> Fixtures.deliveredOrder(c, productId, 1);
            default -> { }
        }
        reviews.create(c.token(), productId, 5).then().statusCode(expected);
    }

    @Test
    @DisplayName("Anonymous user cannot post a review (401), but can read reviews")
    void anonymous() {
        reviews.create(null, productId, 5).then().statusCode(401);
        reviews.forProduct(productId).then().statusCode(200).body("reviewCount", equalTo(0));
    }

    @Test
    @DisplayName("Second review of the same product by the same customer is a 409")
    void oneReviewPerCustomer() {
        Customer c = Fixtures.verifiedBuyer(productId);
        reviews.create(c.token(), productId, 5).then().statusCode(201);
        reviews.create(c.token(), productId, 1).then().statusCode(409)
                .body("message", containsString("already reviewed"));
    }

    // ---------- validation boundaries ----------

    @ParameterizedTest(name = "rating {0} -> {1}")
    @CsvSource({"0, 400", "1, 201", "5, 201", "6, 400", "-1, 400"})
    @DisplayName("Rating boundaries 1..5")
    void ratingBoundaries(int rating, int expected) {
        reviews.create(Fixtures.verifiedBuyer(productId).token(), productId, rating).then().statusCode(expected);
    }

    @ParameterizedTest(name = "title length {0} -> {1}")
    @CsvSource({"100, 201", "101, 400"})
    @DisplayName("Title max 100 characters")
    void titleBoundary(int length, int expected) {
        reviews.create(Fixtures.verifiedBuyer(productId).token(), productId, 4, "t".repeat(length), null)
                .then().statusCode(expected);
    }

    @ParameterizedTest(name = "body length {0} -> {1}")
    @CsvSource({"2000, 201", "2001, 400"})
    @DisplayName("Body max 2000 characters")
    void bodyBoundary(int length, int expected) {
        reviews.create(Fixtures.verifiedBuyer(productId).token(), productId, 4, null, "b".repeat(length))
                .then().statusCode(expected);
    }

    @Test
    @DisplayName("Missing rating gives 400 with a field error")
    void missingRating() {
        reviews.create(Fixtures.verifiedBuyer(productId).token(), productId, null, "t", "b")
                .then().statusCode(400).body("fieldErrors", hasKey("rating"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"random", "HIGHEST'--", "price"})
    @DisplayName("Unknown sort value gives 400")
    void badSort(String sort) {
        reviews.forProduct(productId, sort).then().statusCode(400);
    }

    @Test
    @DisplayName("Reviewing an unknown product gives 404")
    void unknownProduct() {
        reviews.create(Fixtures.newCustomer().token(), 99_999_999L, 5).then().statusCode(404);
    }

    // ---------- rating maths ----------

    @Test
    @Tag("smoke")
    @DisplayName("Average 4,4,5 = 4.3 (HALF_UP), distribution and product summary agree")
    void averageAndDistribution() {
        for (int stars : new int[]{4, 4, 5}) {
            reviews.create(Fixtures.verifiedBuyer(productId).token(), productId, stars).then().statusCode(201);
        }
        reviews.forProduct(productId).then().statusCode(200)
                .body("averageRating", equalTo(4.3f))
                .body("reviewCount", equalTo(3))
                .body("distribution.'5'", equalTo(1))
                .body("distribution.'4'", equalTo(2))
                .body("distribution.'3'", equalTo(0));
        products.get(productId).then()
                .body("ratingAverage", equalTo(4.3f))
                .body("ratingCount", equalTo(3));
    }

    @ParameterizedTest(name = "sort={0} -> {1}")
    @CsvSource({"highest, '5,3,1'", "lowest, '1,3,5'"})
    @DisplayName("Sorting by rating")
    void sorting(String sort, String expected) {
        for (int stars : new int[]{3, 1, 5}) {
            reviews.create(Fixtures.verifiedBuyer(productId).token(), productId, stars).then().statusCode(201);
        }
        Integer[] order = java.util.Arrays.stream(expected.split(",")).map(Integer::valueOf).toArray(Integer[]::new);
        reviews.forProduct(productId, sort).then().body("reviews.rating", contains(order));
    }

    @Test
    @DisplayName("Newest first is the default order")
    void newestFirst() {
        long first = reviews.create(Fixtures.verifiedBuyer(productId).token(), productId, 2).then().extract().<Integer>path("id");
        long second = reviews.create(Fixtures.verifiedBuyer(productId).token(), productId, 4).then().extract().<Integer>path("id");
        reviews.forProduct(productId).then().body("reviews.id", contains((int) second, (int) first));
    }

    // ---------- privacy and security ----------

    @Test
    @Tag("security")
    @DisplayName("Public reviews show a masked reviewer, never an e-mail or user id")
    void reviewerMasked() {
        reviews.create(Fixtures.verifiedBuyer(productId).token(), productId, 5, "Nice", "Good").then().statusCode(201);
        reviews.forProduct(productId).then()
                .body("reviews[0].reviewer", matchesPattern("^qa\\*\\*\\*$"))
                .body(not(containsString("@")))
                .body("reviews[0]", not(hasKey("userId")));
    }

    @Test
    @Tag("security")
    @DisplayName("HTML/script in a review is returned as plain data (the UI escapes it)")
    void scriptStoredAsText() {
        String xss = "<img src=x onerror=alert(1)>";
        reviews.create(Fixtures.verifiedBuyer(productId).token(), productId, 3, xss, xss).then().statusCode(201)
                .body("title", equalTo(xss));
    }

    @Test
    @Tag("security")
    @DisplayName("IDOR: another customer cannot edit or delete my review (404)")
    void idor() {
        Customer owner = Fixtures.verifiedBuyer(productId);
        long id = reviews.create(owner.token(), productId, 5).then().statusCode(201).extract().<Integer>path("id");
        Customer attacker = Fixtures.verifiedBuyer(productId);

        reviews.update(attacker.token(), id, 1, "hacked", null).then().statusCode(404);
        reviews.delete(attacker.token(), id).then().statusCode(404);
        reviews.forProduct(productId).then().body("reviews[0].rating", equalTo(5));
    }

    // ---------- edit, delete, moderation ----------

    @Test
    @DisplayName("Owner edits (average follows) and deletes (average resets)")
    void editAndDelete() {
        Customer c = Fixtures.verifiedBuyer(productId);
        long id = reviews.create(c.token(), productId, 2, "meh", null).then().extract().<Integer>path("id");

        reviews.update(c.token(), id, 4, "Better after update", null).then().statusCode(200)
                .body("rating", equalTo(4)).body("updatedAt", notNullValue());
        products.get(productId).then().body("ratingAverage", equalTo(4.0f));

        reviews.delete(c.token(), id).then().statusCode(204);
        products.get(productId).then().body("ratingAverage", equalTo(0.0f)).body("ratingCount", equalTo(0));
        reviews.create(c.token(), productId, 3).then().statusCode(201);   // may review again after deleting
    }

    @Test
    @DisplayName("Admin hides a review: gone from the list and the average; author still sees it as HIDDEN")
    void moderation() {
        reviews.create(Fixtures.verifiedBuyer(productId).token(), productId, 5).then().statusCode(201);
        Customer spammer = Fixtures.verifiedBuyer(productId);
        long spam = reviews.create(spammer.token(), productId, 1, "SPAM", "cheap watches").then().extract().<Integer>path("id");
        products.get(productId).then().body("ratingAverage", equalTo(3.0f));

        reviews.hide(spammer.token(), spam).then().statusCode(403);
        reviews.hide(Fixtures.adminToken(), spam).then().statusCode(200).body("status", equalTo("HIDDEN"));

        reviews.forProduct(productId).then().body("reviewCount", equalTo(1)).body("reviews.title", not(contains("SPAM")));
        products.get(productId).then().body("ratingAverage", equalTo(5.0f));
        reviews.mine(spammer.token()).then().body("[0].status", equalTo("HIDDEN"));
        reviews.adminList(Fixtures.adminToken(), "HIDDEN").then().statusCode(200).body("id", org.hamcrest.Matchers.hasItem((int) spam));
        reviews.adminList(Fixtures.adminToken(), "WHATEVER").then().statusCode(400);

        reviews.publish(Fixtures.adminToken(), spam).then().statusCode(200);
        products.get(productId).then().body("ratingAverage", equalTo(3.0f));
    }

    @Test
    @DisplayName("Blank title and body are stored as null")
    void blankFieldsBecomeNull() {
        reviews.create(Fixtures.verifiedBuyer(productId).token(), productId, 4, "   ", "  ").then().statusCode(201)
                .body("title", nullValue()).body("body", nullValue());
    }

}
