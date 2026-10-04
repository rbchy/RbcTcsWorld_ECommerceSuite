package com.rbctcsworld.ecommerce.qa.tests.ui;

import com.rbctcsworld.ecommerce.qa.drivers.DriverFactory;
import com.rbctcsworld.ecommerce.qa.pages.CartPage;
import com.rbctcsworld.ecommerce.qa.pages.CheckoutPage;
import com.rbctcsworld.ecommerce.qa.pages.HomePage;
import com.rbctcsworld.ecommerce.qa.pages.LoginPage;
import com.rbctcsworld.ecommerce.qa.pages.OrderPage;
import com.rbctcsworld.ecommerce.qa.pages.OrdersPage;
import com.rbctcsworld.ecommerce.qa.reporting.ScreenshotOnFailure;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures;
import com.rbctcsworld.ecommerce.qa.testdata.TestData;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.WebDriver;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The full customer journey through the browser: register -> add to cart -> coupon -> place order ->
 * declined card -> approved card. Prices use a fresh product ($30.00) so the totals are exact:
 * 2 x 30 = 60.00, WELCOME10 -6.00, shipping FREE (>= 50), tax 6% of 54.00 = 3.24, total 57.24.
 */
@Tag("ui")
@Epic("Storefront UI")
@Feature("Shopping journey")
class UiShoppingJourneyTest {

    private WebDriver driver;

    @RegisterExtension
    ScreenshotOnFailure screenshots = new ScreenshotOnFailure(() -> driver);

    @BeforeEach
    void start() {
        driver = DriverFactory.create();
    }

    @AfterEach
    void stop() {
        if (driver != null) driver.quit();
    }

    @Test
    @Tag("smoke")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("New customer registers, buys with a coupon and pays after one declined card")
    void endToEndPurchase() {
        Fixtures.Product product = Fixtures.namedProduct("30.00", 10);
        String email = TestData.uniqueEmail();

        HomePage home = new LoginPage(driver).open().register(email, TestData.PASSWORD);
        assertTrue(home.isLoggedInAs(email), "header shows the new customer");

        home.search(product.sku()).addToCart(product.sku(), 2);
        assertTrue(home.flash().contains("2 item(s)"), "flash message confirms the cart: " + home.flash());

        CartPage cart = home.goToCart();
        assertAll("cart",
                () -> assertEquals(1, cart.lineCount()),
                () -> assertEquals(2, cart.quantityOf(product.sku())),
                () -> assertEquals("$60.00", cart.subtotal()));

        CheckoutPage checkout = cart.checkout();
        assertAll("price before coupon",
                () -> assertEquals("$60.00", checkout.subtotal()),
                () -> assertEquals("FREE", checkout.shipping()),
                () -> assertEquals("$3.60", checkout.tax()),
                () -> assertEquals("$63.60", checkout.total()));

        checkout.applyCoupon(TestData.COUPON_WELCOME10);
        assertAll("price after WELCOME10",
                () -> assertEquals("-$6.00", checkout.discount()),
                () -> assertEquals("$3.24", checkout.tax()),
                () -> assertEquals("$57.24", checkout.total()));

        OrderPage order = checkout.placeOrder();
        assertAll("new order",
                () -> assertEquals("PLACED", order.status()),
                () -> assertEquals("$57.24", order.total()),
                () -> assertTrue(order.canPay()));

        order.pay(TestData.CARD_DECLINED);
        assertAll("declined card",
                () -> assertTrue(order.error().toLowerCase().contains("declined"), order.error()),
                () -> assertEquals("PLACED", order.status(), "order stays unpaid"));

        order.pay(TestData.CARD_OK).waitForStatus("PAID");
        assertAll("approved card",
                () -> assertTrue(order.notice().contains("Payment received")),
                () -> assertFalse(order.canPay(), "pay form disappears"),
                () -> assertEquals(List.of("PLACED", "PAID"), order.timeline()),
                () -> assertTrue(order.paymentStatuses().contains("FAILED"), "declined attempt is kept"));

        OrdersPage orders = new OrdersPage(driver).open();
        assertEquals(1, orders.count(), "customer sees exactly one order");
    }

    @Test
    @DisplayName("Invalid coupon shows an error and does not change the total")
    void invalidCoupon() {
        Fixtures.Customer c = Fixtures.newCustomer();
        Fixtures.Product product = Fixtures.namedProduct("30.00", 10);
        HomePage.loginWithToken(driver, c.token(), c.email());

        HomePage home = new HomePage(driver).open();
        home.search(product.sku()).addToCart(product.sku(), 1);
        home.flash();
        CheckoutPage checkout = home.goToCart().checkout();
        String total = checkout.total();

        checkout.applyCoupon(TestData.COUPON_EXPIRED);
        assertAll(
                () -> assertFalse(checkout.error().isBlank()),
                () -> assertEquals(total, checkout.total()));
    }

    @Test
    @DisplayName("Customer cancels a placed order from the order page")
    void cancelOrder() {
        Fixtures.Customer c = Fixtures.newCustomer();
        Fixtures.Product product = Fixtures.namedProduct("12.50", 5);
        HomePage.loginWithToken(driver, c.token(), c.email());

        HomePage home = new HomePage(driver).open();
        home.search(product.sku()).addToCart(product.sku(), 1);
        home.flash();
        OrderPage order = home.goToCart().checkout().placeOrder();

        order.cancel().waitForStatus("CANCELLED");
        assertAll(
                () -> assertFalse(order.canCancel()),
                () -> assertFalse(order.canPay()),
                () -> assertEquals(List.of("PLACED", "CANCELLED"), order.timeline()));
    }
}
