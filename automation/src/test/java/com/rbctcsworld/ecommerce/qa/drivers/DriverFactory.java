package com.rbctcsworld.ecommerce.qa.drivers;

import com.rbctcsworld.ecommerce.qa.config.TestConfig;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Creates the browser from -Dbrowser=chrome|firefox|edge|safari and -Dheadless=true|false. Selenium Manager downloads drivers. */
public final class DriverFactory {

    /*
     * Selenium warns "Unable to find CDP implementation matching 154" when Chrome is newer than the
     * DevTools bindings bundled with this Selenium version. The tests do not use DevTools (CDP), so the
     * warning is noise. Strong references keep the JUL loggers (and their level) from being garbage collected.
     */
    private static final Logger CDP_FINDER = Logger.getLogger("org.openqa.selenium.devtools.CdpVersionFinder");
    private static final Logger CHROMIUM = Logger.getLogger("org.openqa.selenium.chromium.ChromiumDriver");

    static {
        CDP_FINDER.setLevel(Level.SEVERE);
        CHROMIUM.setLevel(Level.SEVERE);
    }

    private DriverFactory() {
    }

    public static WebDriver create() {
        WebDriver driver = newDriver();
        // same viewport in every browser, so layouts (and screenshots) are comparable
        driver.manage().window().setSize(new org.openqa.selenium.Dimension(1366, 900));
        return driver;
    }

    private static WebDriver newDriver() {
        boolean headless = TestConfig.headless();
        return switch (TestConfig.browser().toLowerCase()) {
            case "firefox" -> {
                FirefoxOptions o = new FirefoxOptions();
                if (headless) o.addArguments("-headless");
                yield new FirefoxDriver(o);
            }
            // Safari (macOS only): no headless mode; enable once with "sudo safaridriver --enable"
            case "safari" -> new org.openqa.selenium.safari.SafariDriver();
            case "edge" -> {
                EdgeOptions o = new EdgeOptions();
                if (headless) o.addArguments("--headless=new");
                // DEF-019: on the Ubuntu 24.04 CI runners AppArmor blocks Edge's sandbox (Chrome ships an AppArmor
                // profile, Edge does not), so Edge exits at start. Only in CI, never on a developer machine.
                if (System.getenv("CI") != null) o.addArguments("--no-sandbox", "--disable-dev-shm-usage");
                yield new EdgeDriver(o);
            }
            default -> {
                ChromeOptions o = new ChromeOptions();
                if (headless) o.addArguments("--headless=new");
                o.addArguments("--window-size=1366,900");
                yield new ChromeDriver(o);
            }
        };
    }
}
