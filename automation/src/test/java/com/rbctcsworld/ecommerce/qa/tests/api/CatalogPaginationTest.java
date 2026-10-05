package com.rbctcsworld.ecommerce.qa.tests.api;

import com.rbctcsworld.ecommerce.qa.api.ProductClient;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DEF-007: GET /api/products returned the WHOLE catalog in one response, so it grew with every product.
 * Contract after the fix (backward compatible - the body is still a JSON array):
 *   ?page=0..&size=1..100 (default 20) &sort=id|name|price_asc|price_desc
 *   headers X-Total-Count, X-Total-Pages, X-Page, X-Page-Size and Link rel="next"/"prev".
 */
@Tag("api")
@Epic("Catalog")
@Feature("Pagination")
@Issue("DEF-007")
class CatalogPaginationTest {

    private static final ProductClient products = new ProductClient();
    private static final int CREATED = 25;
    private static String prefix;

    /** 25 products whose names share a unique prefix, with different prices. */
    @BeforeAll
    static void catalogOf25() {
        prefix = "PG" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        for (int i = 0; i < CREATED; i++) {
            String sku = prefix + "-" + i;
            products.create(Fixtures.adminToken(),
                    ProductClient.body(prefix + " item " + (char) ('A' + i), sku, "qa", new BigDecimal(10 + (i * 7) % 25 + ".50"), 5))
                    .then().statusCode(201);
        }
    }

    private static int header(Response r, String name) {
        String v = r.header(name);
        assertNotNull(v, "missing header " + name);
        return Integer.parseInt(v);
    }

    @Test
    @Tag("smoke")
    @DisplayName("Default page holds at most 20 products, headers tell the real total")
    void defaultPageIsBounded() {
        Response r = products.list();
        r.then().statusCode(200);
        int total = header(r, "X-Total-Count");
        assertAll(
                () -> assertTrue(r.jsonPath().getList("$").size() <= 20, "page size " + r.jsonPath().getList("$").size()),
                () -> assertTrue(total >= CREATED, "total " + total),
                () -> assertEquals(20, header(r, "X-Page-Size")),
                () -> assertEquals(0, header(r, "X-Page")));
    }

    @Test
    @DisplayName("Walking all pages returns every product exactly once (no gaps, no duplicates)")
    void walkAllPages() {
        Set<Integer> ids = new HashSet<>();
        List<Integer> sizes = new ArrayList<>();
        for (int page = 0; page < 3; page++) {
            Response r = products.page(prefix, page, 10, null);
            r.then().statusCode(200);
            assertEquals(CREATED, header(r, "X-Total-Count"));
            assertEquals(3, header(r, "X-Total-Pages"));
            List<Integer> pageIds = r.jsonPath().getList("id", Integer.class);
            sizes.add(pageIds.size());
            ids.addAll(pageIds);
        }
        assertAll(
                () -> assertEquals(List.of(10, 10, 5), sizes),
                () -> assertEquals(CREATED, ids.size(), "distinct ids over all pages"));
        products.page(prefix, 3, 10, null).then().statusCode(200).body("size()", org.hamcrest.Matchers.is(0));
    }

    @Test
    @DisplayName("Link header points to the next page, and the last page has no next link")
    void linkHeader() {
        String first = products.page(prefix, 0, 10, null).header("Link");
        String last = products.page(prefix, 2, 10, null).header("Link");
        assertAll(
                () -> assertTrue(first != null && first.contains("rel=\"next\"") && first.contains("page=1"), "first: " + first),
                () -> assertTrue(last != null && !last.contains("rel=\"next\"") && last.contains("rel=\"prev\""), "last: " + last));
    }

    @ParameterizedTest(name = "page={0} size={1} -> {2}")
    @CsvSource({
            "0,  1,   200",
            "0,  100, 200",
            "0,  0,   400",
            "0,  101, 400",
            "-1, 20,  400",
            "0,  abc, 400",
            "x,  20,  400"})
    @DisplayName("Boundaries: size 1..100, page >= 0, numbers only")
    void boundaries(String page, String size, int status) {
        products.page(null, page, size, null).then().statusCode(status);
    }

    @ParameterizedTest(name = "sort={0}")
    @CsvSource({"price_asc", "price_desc", "name"})
    @DisplayName("Sorting is applied before paging")
    void sorting(String sort) {
        Response r = products.page(prefix, 0, 25, sort);
        r.then().statusCode(200);
        if (sort.equals("name")) {
            List<String> names = r.jsonPath().getList("name", String.class);
            assertEquals(names.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(), names);
        } else {
            List<Float> prices = r.jsonPath().getList("price", Float.class);
            List<Float> expected = new ArrayList<>(prices);
            expected.sort(sort.equals("price_asc") ? Float::compare : (a, b) -> Float.compare(b, a));
            assertEquals(expected, prices);
        }
    }

    @Test
    @DisplayName("Unknown sort gives 400")
    void unknownSort() {
        products.page(null, 0, 20, "stock").then().statusCode(400);
    }

    @Test
    @DisplayName("Response size no longer grows with the catalog: default page stays under 10 KB")
    void responseSizeIsBounded() {
        Response r = products.list();
        int bytes = r.asByteArray().length;
        int total = header(r, "X-Total-Count");
        assertTrue(bytes < 10_000, bytes + " bytes for a catalog of " + total + " products");
        assertFalse(r.jsonPath().getList("$").isEmpty());
    }
}
