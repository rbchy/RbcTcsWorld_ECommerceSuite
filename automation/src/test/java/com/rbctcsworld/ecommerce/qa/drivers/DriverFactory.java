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

/** Creates the browser from -Dbrowser=chrome|firefox|edge and -Dheadless=true|false. Selenium Manager downloads drivers. */
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
        boolean headless = TestConfig.headless();
        return switch (TestConfig.browser().toLowerCase()) {
            case "firefox" -> {
                FirefoxOptions o = new FirefoxOptions();
                if (headless) o.addArguments("-headless");
                yield new FirefoxDriver(o);
            }
            case "edge" -> {
                EdgeOptions o = new EdgeOptions();
                if (headless) o.addArguments("--headless=new");
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
