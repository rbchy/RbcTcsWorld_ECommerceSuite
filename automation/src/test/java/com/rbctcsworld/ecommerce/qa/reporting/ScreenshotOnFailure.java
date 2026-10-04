package com.rbctcsworld.ecommerce.qa.reporting;

import io.qameta.allure.Allure;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.ByteArrayInputStream;
import java.util.function.Supplier;

/**
 * Attaches a screenshot and the current URL to the Allure report when a UI test fails.
 * Uses AfterTestExecutionCallback because it runs BEFORE @AfterEach (where the browser is closed).
 *
 * Usage in a test class:
 *   @RegisterExtension ScreenshotOnFailure screenshots = new ScreenshotOnFailure(() -> driver);
 */
public class ScreenshotOnFailure implements AfterTestExecutionCallback {

    private final Supplier<WebDriver> driver;

    public ScreenshotOnFailure(Supplier<WebDriver> driver) {
        this.driver = driver;
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        if (context.getExecutionException().isEmpty()) {
            return;
        }
        WebDriver d = driver.get();
        if (d instanceof TakesScreenshot ts) {
            try {
                byte[] png = ts.getScreenshotAs(OutputType.BYTES);
                Allure.addAttachment("Screenshot on failure", "image/png", new ByteArrayInputStream(png), "png");
                Allure.addAttachment("URL on failure", d.getCurrentUrl());
            } catch (RuntimeException ignored) {
                // the browser may already be gone; never hide the original failure
            }
        }
    }
}
