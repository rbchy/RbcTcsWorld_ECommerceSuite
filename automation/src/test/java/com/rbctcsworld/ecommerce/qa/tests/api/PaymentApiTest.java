package com.rbctcsworld.ecommerce.qa.tests.api;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import com.rbctcsworld.ecommerce.qa.api.CartClient;
import com.rbctcsworld.ecommerce.qa.api.OrderClient;
import com.rbctcsworld.ecommerce.qa.api.ProductClient;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures.Customer;
import com.rbctcsworld.ecommerce.qa.testdata.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

/** Module 3 - mock payment gateway: approve, decline, validation, refunds, IDOR. */
@Tag("api")
@Tag("payment")
@Epic("Checkout")
@Feature("Payments")
class PaymentApiTest {

    private final CartClient cart = new CartClient();
    private final OrderClient orders = new OrderClient();
    private Customer customer;
    private long productId;

    @BeforeEach
    void orderReady() {
        customer = Fixtures.newCustomer();
        productId = Fixtures.product("10.00", 10);
    }

    /** 2 x 10.00 = 20.00 + 5.99 shipping + 1.20 tax = 27.19 */
    private long placedOrder() {
        cart.add(customer.token(), productId, 2).then().statusCode(201);
        int id = orders.place(customer.token()).then().statusCode(201).body("total", equalTo(27.19f)).extract().path("id");
        return id;
    }

    @Test
    @Tag("smoke")
    @DisplayName("Approved card -> 200 PAID, one SUCCEEDED charge with last 4 digits only")
    void approved() {
        long id = placedOrder();
        orders.pay(customer.token(), id, TestData.CARD_OK).then().statusCode(200)
                .body("status", equalTo("PAID"))
                .body("paidAt", notNullValue());
        orders.payments(customer.token(), id).then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].type", equalTo("CHARGE"))
                .body("[0].status", equalTo("SUCCEEDED"))
                .body("[0].amount", equalTo(27.19f))
                .body("[0].cardLast4", equalTo("4242"));
    }

    @ParameterizedTest(name = "{0} -> 402 ''{1}''")
    @CsvSource({
            "4000000000000002, Card declined",
            "4000000000009995, Insufficient funds"
    })
    @DisplayName("Declined cards -> 402, order stays PLACED, FAILED attempt recorded, retry with good card works")
    void declined(String card, String reason) {
        long id = placedOrder();
        orders.pay(customer.token(), id, card).then().statusCode(402)
                .body("message", equalTo("Payment declined: " + reason));
        orders.get(customer.token(), id).then().body("status", equalTo("PLACED"));
        orders.payments(customer.token(), id).then()
                .body("status", contains("FAILED"))
                .body("[0].failureReason", equalTo(reason));

        orders.pay(customer.token(), id, TestData.CARD_OK).then().statusCode(200).body("status", equalTo("PAID"));
        orders.payments(customer.token(), id).then().body("status", contains("FAILED", "SUCCEEDED"));
    }

    @Test
    @DisplayName("Validation: bad Luhn, expired card, bad month, short CVV, missing fields -> 400, nothing recorded")
    void cardValidation() {
        long id = placedOrder();
        orders.pay(customer.token(), id, TestData.CARD_BAD_LUHN).then().statusCode(400)
                .body("message", equalTo("Invalid card number"));
        orders.pay(customer.token(), id, TestData.CARD_OK, 1, 2020, "123").then().statusCode(400)
                .body("message", equalTo("Card has expired"));
        orders.pay(customer.token(), id, TestData.CARD_OK, 13, 2035, "123").then().statusCode(400)
                .body("fieldErrors.expiryMonth", notNullValue());
        orders.pay(customer.token(), id, TestData.CARD_OK, 12, 2035, "12").then().statusCode(400)
                .body("fieldErrors.cvv", notNullValue());
        orders.pay(customer.token(), id, null, 12, 2035, "123").then().statusCode(400)
                .body("fieldErrors.cardNumber", notNullValue());
        orders.payments(customer.token(), id).then().body("$", hasSize(0));
    }

    @Test
    @DisplayName("Double payment is prevented: second pay -> 409")
    void noDoubleCharge() {
        long id = placedOrder();
        orders.pay(customer.token(), id, TestData.CARD_OK).then().statusCode(200);
        orders.pay(customer.token(), id, TestData.CARD_OK).then().statusCode(409);
        orders.payments(customer.token(), id).then().body("$", hasSize(1));
    }

    @Test
    @DisplayName("Cancel PAID order -> full refund recorded, stock restored, cannot pay a cancelled order")
    void refundOnCancel() {
        long id = placedOrder();
        orders.pay(customer.token(), id, TestData.CARD_OK).then().statusCode(200);
        orders.cancel(customer.token(), id).then().statusCode(200).body("status", equalTo("CANCELLED"));

        orders.payments(customer.token(), id).then()
                .body("type", contains("CHARGE", "REFUND"))
                .body("[1].amount", equalTo(27.19f))
                .body("[1].cardLast4", equalTo("4242"));
        new ProductClient().get(productId).then().body("stock", equalTo(10));
        orders.pay(customer.token(), id, TestData.CARD_OK).then().statusCode(409);
    }

    @Test
    @Tag("security")
    @DisplayName("SECURITY (IDOR): cannot pay for or read payments of someone else's order")
    void idor() {
        long id = placedOrder();
        Customer intruder = Fixtures.newCustomer();
        orders.pay(intruder.token(), id, TestData.CARD_OK).then().statusCode(404);
        orders.payments(intruder.token(), id).then().statusCode(404);
        orders.get(customer.token(), id).then().body("status", equalTo("PLACED"));
    }
}
