package com.rbctcsworld.ecommerce.qa.pages;

import com.rbctcsworld.ecommerce.qa.config.TestConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/** Customer storefront home page (frontend/src/main.jsx). Locators use data-testid attributes. */
public class HomePage extends BasePage {

    private static final By SEARCH = By.cssSelector("[data-testid='search-input']");
    private static final By PRODUCT_CARD = By.cssSelector("[data-testid='product-card']");
    private static final By PRODUCT_NAME = By.cssSelector("[data-testid='product-name']");

    public HomePage(WebDriver driver) {
        super(driver);
    }

    public HomePage open() {
        driver.get(TestConfig.uiUrl());
        visible(SEARCH);
        return this;
    }

    public HomePage search(String text) {
        WebElement box = visible(SEARCH);
        box.clear();
        box.sendKeys(text);
        // wait until every visible card matches the search (results are filtered by the API)
        wait.until(d -> productNames().stream().allMatch(n -> n.toLowerCase().contains(text.toLowerCase())));
        return this;
    }

    public int productCount() {
        wait.until(d -> !all(PRODUCT_CARD).isEmpty());
        return all(PRODUCT_CARD).size();
    }

    public List<String> productNames() {
        return all(PRODUCT_NAME).stream().map(WebElement::getText).toList();
    }
}
