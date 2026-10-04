package com.rbctcsworld.ecommerce.qa.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** "My orders" list (frontend/src/pages/Orders.jsx). */
public class OrdersPage extends BasePage {

    public OrdersPage(WebDriver driver) {
        super(driver);
    }

    public OrdersPage open() {
        openRoute("/orders");
        driver.navigate().refresh();
        return waitLoaded();
    }

    public OrdersPage waitLoaded() {
        wait.until(d -> isShown("orders-table") || isShown("orders-empty"));
        return this;
    }

    public boolean isEmpty() {
        return isShown("orders-empty");
    }

    public int count() {
        return all(testId("order-row")).size();
    }

    public String statusOf(long orderId) {
        return driver.findElement(By.cssSelector(
                "[data-testid='order-row'][data-order-id='" + orderId + "'] [data-testid='order-row-status']")).getText();
    }

    public OrderPage openOrder(long orderId) {
        driver.findElement(By.cssSelector(
                "[data-testid='order-row'][data-order-id='" + orderId + "'] [data-testid='order-link']")).click();
        return new OrderPage(driver).waitLoaded();
    }
}
