package com.rbctcsworld.ecommerce.qa.tests.ui;

import com.rbctcsworld.ecommerce.qa.drivers.DriverFactory;
import com.rbctcsworld.ecommerce.qa.pages.BasePage;
import com.rbctcsworld.ecommerce.qa.pages.OrderPage;
import com.rbctcsworld.ecommerce.qa.pages.OrdersPage;
import com.rbctcsworld.ecommerce.qa.reporting.ScreenshotOnFailure;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hybrid test: the slow preconditions (order paid, shipped, delivered by the warehouse) are created
 * through the API, and only the behaviour under test - what the customer sees and does - goes through the browser.
 */
@Tag("ui")
@Epic("Storefront UI")
@Feature("Tracking and returns")
class UiOrderAfterSalesTest {

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
    @DisplayName("Delivered order shows tracking and accepts a return request")
    void trackingAndReturn() {
        Fixtures.Customer c = Fixtures.newCustomer();
        long productId = Fixtures.product("20.00", 5);
        long orderId = Fixtures.deliveredOrder(c, productId, 1);
        BasePage.loginWithToken(driver, c.token(), c.email());

        OrdersPage orders = new OrdersPage(driver).open();
        assertEquals("DELIVERED", orders.statusOf(orderId));

        OrderPage order = orders.openOrder(orderId);
        assertAll("delivered order",
                () -> assertEquals("DELIVERED", order.status()),
                () -> assertTrue(order.trackingNumber().startsWith("UPS-"), "UPS tracking number: " + order.trackingNumber()),
                () -> assertEquals(List.of("PLACED", "PAID", "SHIPPED", "DELIVERED"), order.timeline()));

        order.requestReturn("DAMAGED").waitForStatus("RETURN_REQUESTED");
        assertAll("return requested",
                () -> assertTrue(order.returnStatus().contains("REQUESTED"), order.returnStatus()),
                () -> assertTrue(order.timeline().contains("RETURN_REQUESTED")));
    }
}
