package com.rbctcsworld.ecommerce.qa.tests.ui;

import com.rbctcsworld.ecommerce.qa.api.ProductClient;
import com.rbctcsworld.ecommerce.qa.api.ReviewClient;
import com.rbctcsworld.ecommerce.qa.drivers.DriverFactory;
import com.rbctcsworld.ecommerce.qa.pages.BasePage;
import com.rbctcsworld.ecommerce.qa.pages.CartPage;
import com.rbctcsworld.ecommerce.qa.pages.HomePage;
import com.rbctcsworld.ecommerce.qa.pages.ProductPage;
import com.rbctcsworld.ecommerce.qa.pages.WishlistPage;
import com.rbctcsworld.ecommerce.qa.reporting.ScreenshotOnFailure;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures.Customer;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.WebDriver;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Module 5 in the browser. Preconditions (delivered orders, other reviews, price change) are made through the API. */
@Tag("ui")
@Epic("Storefront UI")
@Feature("Reviews and wishlist")
class UiReviewWishlistTest {

    private WebDriver driver;
    private final ReviewClient reviews = new ReviewClient();

    @RegisterExtension
    ScreenshotOnFailure screenshots = new ScreenshotOnFailure(() -> driver);

    @BeforeEach
    void start() {
        driver = DriverFactory.create();
    }

    @AfterEach
    void stop() {
        if (driver != null) driver.quit();
    }

    @Test
    @Tag("smoke")
    @DisplayName("Verified buyer writes a review; summary, distribution, sorting and masked name update")
    void verifiedBuyerReviews() {
        Fixtures.Product product = Fixtures.namedProduct("18.00", 20);
        reviews.create(Fixtures.verifiedBuyer(product.id()).token(), product.id(), 5).then().statusCode(201);
        Customer me = Fixtures.verifiedBuyer(product.id());
        BasePage.loginWithToken(driver, me.token(), me.email());

        ProductPage page = new ProductPage(driver).open(product.id());
        assertEquals("5.0", page.averageRating());

        page.writeReview(2, "Broke after a week", "Stopped working on day 8.");
        assertAll("after my review",
                () -> assertTrue(page.notice().contains("published"), page.notice()),
                () -> assertEquals("3.5", page.averageRating()),
                () -> assertEquals(2, page.reviewCount()),
                () -> assertEquals(1, page.distribution(5)),
                () -> assertEquals(1, page.distribution(2)),
                () -> assertTrue(page.reviewers().stream().allMatch(r -> r.matches("^qa\\*\\*\\*$")), "masked: " + page.reviewers()));

        assertEquals(List.of("2", "5"), page.sortBy("lowest").ratingsShown());
        assertEquals(List.of("5", "2"), page.sortBy("highest").ratingsShown());

        HomePage home = new HomePage(driver).open().search(product.sku());
        assertEquals("3.5", home.ratingOf(product.sku()), "rating on the product card");
    }

    @Test
    @DisplayName("Customer who never received the product sees an error when submitting a review")
    void notVerifiedBuyer() {
        Fixtures.Product product = Fixtures.namedProduct("18.00", 20);
        Customer c = Fixtures.newCustomer();
        BasePage.loginWithToken(driver, c.token(), c.email());

        ProductPage page = new ProductPage(driver).open(product.id()).writeReview(5, "Fake", null);
        assertAll(
                () -> assertTrue(page.error().contains("received"), page.error()),
                () -> assertEquals(0, page.reviewCount()));
    }

    @Test
    @Tag("smoke")
    @DisplayName("Wishlist: save from the grid, see a price drop, sold-out item cannot move, move the other to the cart")
    void wishlistJourney() {
        Fixtures.Product lamp = Fixtures.namedProduct("40.00", 5);
        Fixtures.Product soldOut = Fixtures.namedProduct("9.99", 0);
        Customer c = Fixtures.newCustomer();
        BasePage.loginWithToken(driver, c.token(), c.email());

        HomePage home = new HomePage(driver).open();
        home.search(lamp.sku()).addToWishlist(lamp.sku());
        assertTrue(home.flash().contains("Saved to wishlist"), home.flash());
        home.search(soldOut.sku()).addToWishlist(soldOut.sku());

        new ProductClient().update(Fixtures.adminToken(), lamp.id(),
                ProductClient.body(lamp.name(), lamp.sku(), "qa", new BigDecimal("31.50"), 5)).then().statusCode(200);

        WishlistPage wishlist = new WishlistPage(driver).open();
        assertAll("wishlist",
                () -> assertEquals(2, wishlist.count()),
                () -> assertTrue(wishlist.priceDrop(lamp.sku()).contains("$8.50"), wishlist.priceDrop(lamp.sku())),
                () -> assertTrue(wishlist.isUnavailable(soldOut.sku())),
                () -> assertFalse(wishlist.canMoveToCart(soldOut.sku()), "sold-out button disabled"));

        wishlist.moveToCart(lamp.sku());
        assertAll("after move",
                () -> assertEquals("Moved to cart", wishlist.notice()),
                () -> assertEquals(1, wishlist.count()),
                () -> assertFalse(wishlist.contains(lamp.sku())));

        CartPage cart = new CartPage(driver).open();
        assertAll("cart",
                () -> assertEquals(1, cart.quantityOf(lamp.sku())),
                () -> assertEquals("$31.50", cart.subtotal()));
    }
}
