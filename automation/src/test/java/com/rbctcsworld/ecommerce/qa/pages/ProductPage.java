package com.rbctcsworld.ecommerce.qa.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

/** Product detail with rating summary, reviews and review form (frontend/src/pages/ProductDetail.jsx). */
public class ProductPage extends BasePage {

    public ProductPage(WebDriver driver) {
        super(driver);
    }

    public ProductPage open(long productId) {
        openRoute("/products/" + productId);
        driver.navigate().refresh();
        visible("detail-name");
        return this;
    }

    public String name() { return text("detail-name"); }

    /** "4.3" - read from data-average so the test does not depend on how stars are drawn. */
    public String averageRating() {
        return wait.until(d -> d.findElement(testId("avg-rating")).getDomAttribute("data-average"));
    }

    public int reviewCount() {
        return Integer.parseInt(wait.until(d -> d.findElement(testId("avg-rating")).getDomAttribute("data-count")));
    }

    public int distribution(int stars) {
        return Integer.parseInt(wait.until(d -> d.findElement(testId("dist-" + stars)).getDomAttribute("data-count")));
    }

    public List<String> ratingsShown() {
        return attributeOfAll("review-item", "data-rating");
    }

    public List<String> reviewers() {
        return wait.until(d -> d.findElements(testId("review-reviewer")).stream().map(e -> e.getText().trim()).toList());
    }

    /** Chooses a sort order and waits until the list really is in that order. */
    public ProductPage sortBy(String value) {
        new Select(visible("review-sort")).selectByValue(value);
        wait.until(d -> {
            List<Integer> r = ratingsShown().stream().map(Integer::valueOf).toList();
            List<Integer> sorted = r.stream().sorted("highest".equals(value)
                    ? java.util.Comparator.reverseOrder() : java.util.Comparator.naturalOrder()).toList();
            return "newest".equals(value) || r.equals(sorted);
        });
        return this;
    }

    /** Submits the review form; waits until a success notice or an error appears. */
    public ProductPage writeReview(int stars, String title, String body) {
        new Select(visible("review-rating")).selectByValue(String.valueOf(stars));
        if (title != null) type("review-title-input", title);
        if (body != null) type("review-body-input", body);
        int before = reviewCount();
        click("review-submit");
        // success = the page reloaded the summary with one more review; failure = an error message
        wait.until(d -> isShown("review-error") || reviewCount() != before);
        return this;
    }

    public boolean canWriteReview() { return isShown("review-form"); }
    public String notice() { return text("review-notice"); }
    public String error() { return text("review-error"); }

    public ProductPage saveToWishlist() {
        click("detail-wishlist");
        wait.until(d -> isShown("review-notice") || isShown("review-error"));
        return this;
    }
}
