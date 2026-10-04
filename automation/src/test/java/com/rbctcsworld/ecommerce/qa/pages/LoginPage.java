package com.rbctcsworld.ecommerce.qa.pages;

import org.openqa.selenium.WebDriver;

/** Login / registration page (frontend/src/pages/Login.jsx). */
public class LoginPage extends BasePage {

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage open() {
        openRoute("/login");
        visible("auth-title");
        return this;
    }

    /** Opens a page that needs a session (e.g. "/orders") while logged out: the app shows this login form. */
    public LoginPage openProtected(String route) {
        openRoute(route);
        visible("auth-title");
        return this;
    }

    public LoginPage switchToRegister() {
        if (text("auth-title").startsWith("Log in")) click("auth-toggle");
        wait.until(d -> text("auth-title").startsWith("Create"));
        return this;
    }

    public void submit(String email, String password) {
        type("email-input", email);
        type("password-input", password);
        click("auth-submit");
    }

    /** Registers and waits until the header shows the new user. */
    public HomePage register(String email, String password) {
        switchToRegister();
        submit(email, password);
        wait.until(d -> isShown("nav-user"));
        return new HomePage(driver);
    }

    public String error() {
        return text("auth-error");
    }

    public boolean isDisplayed() {
        return isShown("auth-title");
    }
}
