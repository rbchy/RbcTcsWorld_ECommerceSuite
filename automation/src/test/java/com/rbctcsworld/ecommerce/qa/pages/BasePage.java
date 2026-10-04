package com.rbctcsworld.ecommerce.qa.pages;

import com.rbctcsworld.ecommerce.qa.config.TestConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Explicit waits only (no implicit waits / Thread.sleep) - the main cause of flaky UI tests.
 * Every locator uses the data-testid attribute the frontend exposes, so CSS/text changes never break tests.
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    protected static By testId(String id) {
        return By.cssSelector("[data-testid='" + id + "']");
    }

    protected WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement visible(String testId) {
        return visible(testId(testId));
    }

    protected List<WebElement> all(By locator) {
        return driver.findElements(locator);
    }

    protected void click(String testId) {
        wait.until(ExpectedConditions.elementToBeClickable(testId(testId))).click();
    }

    /** Cmd on macOS, Ctrl elsewhere. Sending both breaks Linux: Chrome types Meta+A as a literal "a". */
    private static final Keys SELECT_ALL_MODIFIER =
            System.getProperty("os.name", "").toLowerCase().contains("mac") ? Keys.COMMAND : Keys.CONTROL;

    protected void type(String testId, String text) {
        WebElement e = visible(testId);
        e.sendKeys(Keys.chord(SELECT_ALL_MODIFIER, "a"), Keys.DELETE);
        e.sendKeys(text);
        // guard: fail here with a clear message instead of later with a confusing one
        new WebDriverWait(driver, Duration.ofSeconds(3))
                .withMessage("field '" + testId + "' should contain '" + text + "'")
                .until(d -> text.equals(e.getDomProperty("value")));
    }

    protected String text(String testId) {
        return visible(testId).getText().trim();
    }

    protected boolean isShown(String testId) {
        return !all(testId(testId)).isEmpty() && all(testId(testId)).get(0).isDisplayed();
    }

    /** Opens a client-side route such as "/cart" or "/orders/12". */
    protected void openRoute(String route) {
        driver.get(TestConfig.uiUrl() + "/#" + route);
    }

    /**
     * Fast login for tests whose subject is NOT the login page: put the JWT (from an API call) into
     * localStorage exactly like the app does after a real login. Saves ~2s and removes a dependency per test.
     */
    public static void loginWithToken(WebDriver driver, String token, String email) {
        driver.get(TestConfig.uiUrl() + "/#/");
        ((JavascriptExecutor) driver).executeScript(
                "localStorage.setItem('token', arguments[0]); localStorage.setItem('email', arguments[1]);"
                        + "localStorage.setItem('role', 'CUSTOMER');", token, email);
    }

    public String title() {
        return driver.getTitle();
    }
}
