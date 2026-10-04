package com.rbctcsworld.ecommerce.qa.tests.ui;

import com.rbctcsworld.ecommerce.qa.drivers.DriverFactory;
import com.rbctcsworld.ecommerce.qa.pages.LoginPage;
import com.rbctcsworld.ecommerce.qa.pages.OrdersPage;
import com.rbctcsworld.ecommerce.qa.reporting.ScreenshotOnFailure;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures;
import com.rbctcsworld.ecommerce.qa.testdata.TestData;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("ui")
@Epic("Storefront UI")
@Feature("Login")
class UiAuthTest {

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
    @DisplayName("Wrong password shows an error and keeps the user on the login page")
    void wrongPassword() {
        Fixtures.Customer c = Fixtures.newCustomer();
        LoginPage login = new LoginPage(driver).open();
        login.submit(c.email(), "Wrong-Password1!");
        assertFalse(login.error().isBlank(), "error message shown");
        assertTrue(login.isDisplayed());
    }

    @Test
    @DisplayName("Protected page asks for login, then returns to that page")
    void protectedRouteRedirect() {
        Fixtures.Customer c = Fixtures.newCustomer();
        LoginPage login = new LoginPage(driver).openProtected("/orders");
        assertTrue(login.isDisplayed(), "anonymous user sees the login form");

        login.submit(c.email(), TestData.PASSWORD);
        OrdersPage orders = new OrdersPage(driver).waitLoaded();
        assertTrue(orders.isEmpty(), "back on My orders, which is empty for a new customer");
    }
}
