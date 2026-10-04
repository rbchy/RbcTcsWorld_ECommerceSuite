package com.rbctcsworld.ecommerce.qa.tests.api;

import com.rbctcsworld.ecommerce.qa.api.AuthClient;
import com.rbctcsworld.ecommerce.qa.testdata.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Tag("api")
@Tag("smoke")
class AuthApiTest {

    private final AuthClient auth = new AuthClient();

    @Test
    @DisplayName("Register new customer -> 201 with token and CUSTOMER role")
    void registerCustomer() {
        String email = TestData.uniqueEmail();
        auth.register(email, TestData.PASSWORD).then()
                .statusCode(201)
                .body("token", notNullValue())
                .body("email", equalTo(email))
                .body("role", equalTo("CUSTOMER"));
    }

    @Test
    @DisplayName("Email is case-insensitive: login with upper-case email works")
    void emailIsNormalized() {
        String email = TestData.uniqueEmail();
        auth.register(email, TestData.PASSWORD).then().statusCode(201);
        auth.login(email.toUpperCase(), TestData.PASSWORD).then().statusCode(200);
    }

    @Test
    @DisplayName("Duplicate registration -> 409")
    void duplicateRegistration() {
        String email = TestData.uniqueEmail();
        auth.register(email, TestData.PASSWORD).then().statusCode(201);
        auth.register(email, TestData.PASSWORD).then()
                .statusCode(409)
                .body("message", equalTo("Email already registered"));
    }

    @Test
    @DisplayName("Wrong password and unknown user return the SAME 401 message (no account enumeration)")
    void invalidLoginIs401() {
        String email = TestData.uniqueEmail();
        auth.register(email, TestData.PASSWORD).then().statusCode(201);

        auth.login(email, "WrongPass123!").then().statusCode(401)
                .body("message", equalTo("Invalid email or password"));
        auth.login(TestData.uniqueEmail(), "WrongPass123!").then().statusCode(401)
                .body("message", equalTo("Invalid email or password"));
    }

    @ParameterizedTest(name = "[{index}] email=''{0}'' password=''{1}'' -> 400 on {2}")
    @CsvSource({
            "not-an-email, Password1!, email",
            "'',           Password1!, email",
            "ok@test.com,  short,      password",
            "ok@test.com,  '',         password"
    })
    @DisplayName("Validation errors -> 400 with fieldErrors")
    void validationErrors(String email, String password, String badField) {
        auth.register(email, password).then()
                .statusCode(400)
                .body("fieldErrors." + badField, notNullValue());
    }

    @Test
    @DisplayName("Seeded admin can log in with ADMIN role")
    void adminLogin() {
        auth.login(com.rbctcsworld.ecommerce.qa.config.TestConfig.adminEmail(),
                        com.rbctcsworld.ecommerce.qa.config.TestConfig.adminPassword())
                .then().statusCode(200).body("role", equalTo("ADMIN"));
    }
}
