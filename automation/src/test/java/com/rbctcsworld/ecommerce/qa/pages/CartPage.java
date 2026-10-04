package com.rbctcsworld.ecommerce.qa.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Cart page (frontend/src/pages/Cart.jsx). */
public class CartPage extends BasePage {

    public CartPage(WebDriver driver) {
        super(driver);
    }

    public CartPage open() {
        openRoute("/cart");
        return waitLoaded();
    }

    CartPage waitLoaded() {
        wait.until(d -> isShown("cart-table") || isShown("cart-empty"));
        return this;
    }

    public int lineCount() {
        return all(testId("cart-line")).size();
    }

    public int quantityOf(String sku) {
        return Integer.parseInt(driver.findElement(By.cssSelector("[data-testid='cart-line'][data-sku='" + sku + "'] [data-testid='line-qty']"))
                .getDomProperty("value"));
    }

    public String subtotal() {
        return text("cart-subtotal");
    }

    public boolean isEmpty() {
        return isShown("cart-empty");
    }

    public CheckoutPage checkout() {
        click("go-checkout");
        return new CheckoutPage(driver).waitLoaded();
    }
}
