package com.rbctcsworld.ecommerce.qa.tests.security;

import com.rbctcsworld.ecommerce.qa.api.AuthClient;
import com.rbctcsworld.ecommerce.qa.api.RawClient;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures;
import com.rbctcsworld.ecommerce.qa.testdata.TestData;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OWASP A07 Identification and Authentication Failures + JWT attacks, against the running backend.
 * Tokens are crafted by hand (Base64 + HMAC) - exactly what an attacker would do.
 */
@Tag("security")
@Epic("Security")
@Feature("Authentication and JWT")
class AuthenticationAttackTest {

    private final AuthClient auth = new AuthClient();
    private final RawClient raw = new RawClient();

    private static String b64(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String hs256(String headerAndPayload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(headerAndPayload.getBytes(StandardCharsets.UTF_8)));
    }

    private static final String ADMIN_CLAIMS = "{\"iss\":\"rbctcsworld-ecommerce\",\"sub\":\"admin@rbctcsworld.com\",\"exp\":4102444800}";

    // ---------- JWT ----------

    @Test
    @DisplayName("alg:none token claiming to be the admin is rejected (401)")
    void algNone() {
        String token = b64("{\"alg\":\"none\",\"typ\":\"JWT\"}") + "." + b64(ADMIN_CLAIMS) + ".";
        raw.withAuthorizationHeader("GET", "/api/admin/orders", "Bearer " + token).then().statusCode(401);
    }

    @Test
    @DisplayName("Customer edits the token payload to become the admin: signature no longer matches (401)")
    void editedPayload() {
        String[] parts = Fixtures.newCustomer().token().split("\\.");
        String forged = parts[0] + "." + b64(ADMIN_CLAIMS) + "." + parts[2];
        raw.withAuthorizationHeader("GET", "/api/admin/orders", "Bearer " + forged).then().statusCode(401);
        raw.withAuthorizationHeader("GET", "/api/cart", "Bearer " + forged).then().statusCode(401);
    }

    @ParameterizedTest(name = "guessed secret ''{0}''")
    @ValueSource(strings = {"secret", "changeme", "password123456789012345678901234567890", "rbctcsworld-ecommerce-jwt-secret-key-2026"})
    @DisplayName("Token signed with a guessed secret is rejected (401)")
    void guessedSecret(String secret) throws Exception {
        String unsigned = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}") + "." + b64(ADMIN_CLAIMS);
        String token = unsigned + "." + hs256(unsigned, secret);
        raw.withAuthorizationHeader("GET", "/api/admin/orders", "Bearer " + token).then().statusCode(401);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer", "Bearer ", "Bearer x.y.z", "bearer abc", "Basic YWRtaW46QWRtaW5AMTIzNDU=", "Token abc"})
    @DisplayName("Malformed Authorization headers never authenticate and never cause 500")
    void malformedHeaders(String header) {
        raw.withAuthorizationHeader("GET", "/api/cart", header).then().statusCode(401)
                .body("message", containsString("Authentication required"));
    }

    @Test
    @DisplayName("Role is taken from the database: a CUSTOMER token can never reach admin endpoints")
    void customerTokenCannotEscalate() {
        raw.send("GET", "/api/admin/orders", Fixtures.newCustomer().token(), null).then().statusCode(403);
    }

    // ---------- registration ----------

    @Test
    @DisplayName("Mass assignment: \"role\":\"ADMIN\" in the registration body is ignored")
    void massAssignment() {
        Map<String, Object> body = new HashMap<>();
        body.put("email", TestData.uniqueEmail());
        body.put("password", TestData.PASSWORD);
        body.put("role", "ADMIN");
        body.put("id", 1);
        Response r = raw.send("POST", "/api/auth/register", null, body);
        r.then().statusCode(201).body("role", equalTo("CUSTOMER"));
        raw.send("GET", "/api/admin/orders", r.path("token"), null).then().statusCode(403);
    }

    @ParameterizedTest
    @ValueSource(strings = {"short", "1234567", ""})
    @DisplayName("Passwords shorter than 8 characters are refused")
    void weakPasswords(String password) {
        auth.register(TestData.uniqueEmail(), password).then().statusCode(400);
    }

    @Test
    @DisplayName("Passwords longer than 72 bytes are refused (bcrypt would silently ignore the rest)")
    void bcryptLimit() {
        auth.register(TestData.uniqueEmail(), "A1!" + "x".repeat(70)).then().statusCode(400);
    }

    // ---------- brute force ----------

    @Test
    @DisplayName("Brute force: 5 wrong passwords lock the account for 15 min (429 + Retry-After), even for the right password")
    void bruteForceLockout() {
        String email = TestData.uniqueEmail();
        auth.register(email, TestData.PASSWORD).then().statusCode(201);
        for (int i = 1; i <= 5; i++) auth.login(email, "Wrong-Pass-" + i).then().statusCode(401);

        Response locked = auth.login(email, TestData.PASSWORD);
        int retryAfter = Integer.parseInt(locked.header("Retry-After"));
        assertAll(
                () -> assertEquals(429, locked.statusCode()),
                () -> assertTrue(retryAfter > 800 && retryAfter <= 900, "Retry-After " + retryAfter),
                () -> assertTrue(locked.path("message").toString().contains("Too many failed login attempts")));

        auth.login(com.rbctcsworld.ecommerce.qa.config.TestConfig.adminEmail(), com.rbctcsworld.ecommerce.qa.config.TestConfig.adminPassword()).then().statusCode(200);   // others unaffected
    }

    @Test
    @DisplayName("Unknown e-mails are locked the same way, so 429 does not reveal which accounts exist")
    void lockoutDoesNotEnumerate() {
        String ghost = TestData.uniqueEmail();
        for (int i = 0; i < 5; i++) auth.login(ghost, "Wrong-Pass-1").then().statusCode(401);
        auth.login(ghost, "Wrong-Pass-1").then().statusCode(429);
    }

    // ---------- account enumeration by timing ----------

    @Test
    @Tag("timing")
    @DisplayName("Timing: a wrong password for a real account and an unknown e-mail take about the same time")
    void noTimingEnumeration() {
        List<Long> existing = new ArrayList<>();
        List<Long> unknown = new ArrayList<>();
        for (int u = 0; u < 3; u++) {
            String email = TestData.uniqueEmail();
            auth.register(email, TestData.PASSWORD).then().statusCode(201);
            for (int i = 0; i < 3; i++) {                        // 3 attempts per user: stays below the lockout
                existing.add(auth.login(email, "Wrong-Pass-1").then().statusCode(401).extract().time());
                unknown.add(auth.login(TestData.uniqueEmail(), "Wrong-Pass-1").then().statusCode(401).extract().time());
            }
        }
        long real = median(existing);
        long ghost = median(unknown);
        // Before the fix: unknown ~2 ms vs real ~80 ms (bcrypt). After: both run bcrypt.
        assertTrue(ghost >= real * 0.5, "unknown e-mail median " + ghost + " ms vs real account median " + real + " ms");
    }

    private static long median(List<Long> values) {
        List<Long> sorted = values.stream().sorted().toList();
        return sorted.get(sorted.size() / 2);
    }
}
