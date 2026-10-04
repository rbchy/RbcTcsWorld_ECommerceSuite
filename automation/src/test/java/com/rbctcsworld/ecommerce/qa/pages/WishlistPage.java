package com.rbctcsworld.ecommerce.qa.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Wishlist page (frontend/src/pages/Wishlist.jsx). */
public class WishlistPage extends BasePage {

    public WishlistPage(WebDriver driver) {
        super(driver);
    }

    public WishlistPage open() {
        openRoute("/wishlist");
        driver.navigate().refresh();
        return waitLoaded();
    }

    public WishlistPage waitLoaded() {
        wait.until(d -> isShown("wishlist-table") || isShown("wishlist-empty"));
        return this;
    }

    public int count() {
        return Integer.parseInt(text("wishlist-count"));
    }

    public boolean isEmpty() { return isShown("wishlist-empty"); }

    private By inLine(String sku, String testId) {
        return By.cssSelector("[data-testid='wishlist-line'][data-sku='" + sku + "'] [data-testid='" + testId + "']");
    }

    public boolean contains(String sku) {
        return !all(By.cssSelector("[data-testid='wishlist-line'][data-sku='" + sku + "']")).isEmpty();
    }

    public String priceDrop(String sku) {
        return wait.until(d -> d.findElement(inLine(sku, "wl-price-drop")).getText().trim());
    }

    public boolean isUnavailable(String sku) {
        return !all(inLine(sku, "wl-unavailable")).isEmpty();
    }

    public boolean canMoveToCart(String sku) {
        return driver.findElement(inLine(sku, "wl-move")).isEnabled();
    }

    public WishlistPage moveToCart(String sku) {
        String before = textOrEmpty("wishlist-count");
        driver.findElement(inLine(sku, "wl-move")).click();
        wait.until(d -> isShown("wishlist-error") || !textOrEmpty("wishlist-count").equals(before));
        return this;
    }

    public String notice() { return text("wishlist-notice"); }
}
