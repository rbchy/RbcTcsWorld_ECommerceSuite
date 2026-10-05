package com.rbctcsworld.ecommerce.qa.tests.security;

import com.rbctcsworld.ecommerce.qa.api.AuthClient;
import com.rbctcsworld.ecommerce.qa.api.ProductClient;
import com.rbctcsworld.ecommerce.qa.api.RawClient;
import com.rbctcsworld.ecommerce.qa.api.ReviewClient;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** OWASP A03 Injection, A05 Security Misconfiguration and data exposure, against the running backend. */
@Tag("security")
@Epic("Security")
@Feature("Injection, headers and information exposure")
class InputAndExposureTest {

    private final RawClient raw = new RawClient();
    private final ProductClient products = new ProductClient();
    private final AuthClient auth = new AuthClient();

    // ---------- injection ----------

    @ParameterizedTest(name = "search q = {0}")
    @ValueSource(strings = {
            "' OR '1'='1", "%' OR 1=1 --", "'; DROP TABLE products; --", "\" OR \"\"=\"",
            "1 UNION SELECT email, password FROM users --", "%", "_", "\\", "${7*7}", "<script>alert(1)</script>"})
    @DisplayName("SQL injection in product search: handled as plain text (queries are parameterized)")
    void sqlInjectionInSearch(String payload) {
        int before = products.list().then().statusCode(200).extract().jsonPath().getList("$").size();
        Response r = products.search(payload);
        r.then().statusCode(200)
                .body(not(containsString("password")))
                .body(not(containsString("SQL")));
        // the payload is a literal name filter, so it can never return MORE than the whole catalog
        assertTrue(r.jsonPath().getList("$").size() <= before);
        products.list().then().statusCode(200);                     // table still exists
    }

    @ParameterizedTest(name = "login email = {0}")
    @ValueSource(strings = {"admin@rbctcsworld.com' --", "' OR 1=1 --", "admin@rbctcsworld.com\" OR \"1\"=\"1"})
    @DisplayName("SQL injection in login never logs in and never causes a server error")
    void sqlInjectionInLogin(String email) {
        int status = auth.login(email, "anything123").statusCode();
        assertTrue(status == 400 || status == 401, "status " + status);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/api/products/..%2f..%2fetc%2fpasswd", "/api/products/1;select", "/api/products/-1",
            "/api/products/99999999999999999999", "/api/orders/%00"})
    @DisplayName("Odd paths and ids give 4xx, never 5xx or file contents")
    void oddPaths(String path) {
        Response r = raw.send("GET", path, Fixtures.newCustomer().token(), null);
        assertAll(
                () -> assertTrue(r.statusCode() >= 400 && r.statusCode() < 500, path + " -> " + r.statusCode()),
                () -> assertFalse(r.asString().contains("root:"), "no file contents"));
    }

    @Test
    @DisplayName("Oversized input is rejected with 400, not accepted and not a crash")
    void oversizedInput() {
        long p = Fixtures.product("5.00", 5);
        new ReviewClient().create(Fixtures.verifiedBuyer(p).token(), p, 5, "t", "x".repeat(100_000))
                .then().statusCode(400);
        products.search("y".repeat(5_000)).then().statusCode(lessThan(500));
    }

    @Test
    @DisplayName("Wrong content type and broken JSON give 4xx with a clean message")
    void badBodies() {
        raw.withHeader("POST", "/api/auth/login", "Content-Type", "text/plain").then().statusCode(anyOf(is(400), is(415)));
        raw.send("POST", "/api/auth/login", null, "{\"email\": ").then().statusCode(400)
                .body("message", equalTo("Malformed or missing JSON request body"))
                .body(not(containsString("Exception")));
    }

    // ---------- headers ----------

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({"GET, /api/products", "GET, /api/cart", "GET, /api/does-not-exist", "POST, /api/auth/login"})
    @DisplayName("Security headers on success AND error responses")
    void securityHeaders(String method, String path) {
        Response r = raw.send(method, path, null, method.equals("POST") ? "{}" : null);
        assertAll(
                () -> assertEquals("nosniff", r.header("X-Content-Type-Options")),
                () -> assertEquals("DENY", r.header("X-Frame-Options")),
                () -> assertEquals("default-src 'none'; frame-ancestors 'none'", r.header("Content-Security-Policy")),
                () -> assertEquals("no-referrer", r.header("Referrer-Policy")),
                () -> assertTrue(String.valueOf(r.header("Cache-Control")).contains("no-store"), "Cache-Control"),
                () -> assertTrue(r.header("X-Powered-By") == null, "no X-Powered-By"),
                () -> assertFalse(String.valueOf(r.header("Server")).matches(".*\\d.*"), "Server header without version"));
    }

    @Test
    @DisplayName("CORS: a foreign website gets no permission to call the API from a browser")
    void noCorsForForeignOrigins() {
        raw.withHeader("GET", "/api/products", "Origin", "https://evil.example").then()
                .header("Access-Control-Allow-Origin", nullValue());
    }

    // ---------- information exposure ----------

    @Test
    @DisplayName("Error bodies never contain stack traces, class names or SQL")
    void cleanErrors() {
        for (String path : List.of("/api/products/abc", "/api/does-not-exist", "/api/orders/999999999")) {
            Response r = raw.send("GET", path, Fixtures.newCustomer().token(), null);
            String body = r.asString();
            assertAll(path,
                    () -> assertTrue(r.statusCode() >= 400 && r.statusCode() < 500),
                    () -> assertFalse(body.contains("Exception"), body),
                    () -> assertFalse(body.contains("at com."), body),
                    () -> assertFalse(body.toLowerCase().contains("select "), body),
                    () -> assertFalse(body.contains("Whitelabel"), body));
        }
    }

    @Test
    @DisplayName("Health endpoint shows only UP/DOWN, no internal details")
    void healthWithoutDetails() {
        raw.send("GET", "/actuator/health", null, null).then().statusCode(200)
                .body("status", equalTo("UP"))
                .body(not(containsString("diskSpace")))
                .body(not(containsString("db")));
    }

    @Test
    @DisplayName("No password hash or internal ids leak in auth and product responses")
    void noSensitiveFields() {
        String email = com.rbctcsworld.ecommerce.qa.testdata.TestData.uniqueEmail();
        String body = auth.register(email, "Password1!").then().statusCode(201).extract().asString();
        assertFalse(body.contains("password") || body.contains("$2a$"), body);
        products.list().then().body("$", everyItem(not(org.hamcrest.Matchers.hasKey("createdBy"))))
                .body("size()", greaterThanOrEqualTo(0));
    }
}
