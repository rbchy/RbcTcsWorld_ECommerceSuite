package com.rbctcsworld.ecommerce.qa.tests.ui;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import com.rbctcsworld.ecommerce.qa.drivers.DriverFactory;
import com.rbctcsworld.ecommerce.qa.pages.HomePage;
import com.rbctcsworld.ecommerce.qa.reporting.ScreenshotOnFailure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Needs backend (8081) + frontend (npm run dev, 5173) running. Excluded with -DexcludedGroups=ui. */
@Tag("ui")
@Epic("Storefront UI")
@Feature("Home page")
class HomePageUiTest {

    private WebDriver driver;
    private HomePage home;

    @RegisterExtension
    ScreenshotOnFailure screenshots = new ScreenshotOnFailure(() -> driver);

    @BeforeEach
    void start() {
        driver = DriverFactory.create();
        home = new HomePage(driver).open();
    }

    @AfterEach
    void stop() {
        if (driver != null) driver.quit();
    }

    @Test
    @Tag("smoke")
    @DisplayName("Storefront loads with title and seeded products")
    void homeLoads() {
        assertAll(
                () -> assertTrue(home.title().toLowerCase().contains("e-commerce"), "title"),
                () -> assertTrue(home.productCount() >= 8, "seeded products visible"));
    }

    @Test
    @DisplayName("Search filters the product grid")
    void searchFilters() {
        home.search("mouse");
        assertFalse(home.productNames().isEmpty(), "at least one result");
        assertTrue(home.productNames().stream().allMatch(n -> n.toLowerCase().contains("mouse")));
    }
}
