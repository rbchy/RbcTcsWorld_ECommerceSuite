package com.rbctcsworld.ecommerce.qa.pages;

import org.openqa.selenium.WebDriver;

/** Checkout page: price breakdown, coupon, place order (frontend/src/pages/Checkout.jsx). */
public class CheckoutPage extends BasePage {

    public CheckoutPage(WebDriver driver) {
        super(driver);
    }

    CheckoutPage waitLoaded() {
        visible("price-total");
        return this;
    }

    public CheckoutPage applyCoupon(String code) {
        String before = total();
        type("coupon-input", code);
        click("coupon-apply");
        // wait for either a new price or an error message
        wait.until(d -> isShown("checkout-error") || !total().equals(before));
        return this;
    }

    public String subtotal() { return text("price-subtotal"); }
    public String discount() { return text("price-discount"); }
    public String shipping() { return text("price-shipping"); }
    public String tax() { return text("price-tax"); }
    public String total() { return text("price-total"); }

    public String error() {
        return text("checkout-error");
    }

    public OrderPage placeOrder() {
        click("place-order");
        return new OrderPage(driver).waitLoaded();
    }
}
