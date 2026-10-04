package com.rbctcsworld.ecommerce.qa.stepdefinitions;

import com.rbctcsworld.ecommerce.qa.api.ProductClient;
import com.rbctcsworld.ecommerce.qa.api.ReviewClient;
import com.rbctcsworld.ecommerce.qa.api.WishlistClient;
import com.rbctcsworld.ecommerce.qa.context.ScenarioContext;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;

import java.util.Arrays;

import static org.hamcrest.Matchers.equalTo;

/** Module 5 steps: reviews, ratings, moderation, wishlist. */
public class EngagementSteps {

    private final ScenarioContext ctx;
    private final ReviewClient reviews = new ReviewClient();
    private final WishlistClient wishlist = new WishlistClient();
    private final ProductClient products = new ProductClient();

    public EngagementSteps(ScenarioContext ctx) {
        this.ctx = ctx;
    }

    private long productId() {
        return ctx.get("productId");
    }

    @Given("the customer has received that product")
    public void received() {
        Fixtures.deliveredOrder(new Fixtures.Customer(null, ctx.token()), productId(), 1);
    }

    @Given("other verified buyers rated that product {string}")
    public void otherBuyers(String csv) {
        Arrays.stream(csv.split(",")).map(String::trim).mapToInt(Integer::parseInt).forEach(stars ->
                reviews.create(Fixtures.verifiedBuyer(productId()).token(), productId(), stars).then().statusCode(201));
    }

    @When("the customer reviews that product with {int} stars")
    public void review(int stars) {
        review(stars, null);
    }

    @When("the customer reviews that product with {int} stars and title {string}")
    public void review(int stars, String title) {
        Response r = reviews.create(ctx.token(), productId(), stars, title, null);
        ctx.lastResponse(r);
        if (r.statusCode() == 201) ctx.put("reviewId", ((Number) r.path("id")).longValue());
    }

    @When("an admin hides the customer's review")
    public void hide() {
        long id = ctx.get("reviewId");
        ctx.lastResponse(reviews.hide(Fixtures.adminToken(), id));
    }

    @Then("the product rating should be {string} from {int} review(s)")
    public void rating(String average, int count) {
        float avg = Float.parseFloat(average);
        products.get(productId()).then().statusCode(200)
                .body("ratingAverage", equalTo(avg)).body("ratingCount", equalTo(count));
        reviews.forProduct(productId()).then().statusCode(200)
                .body("averageRating", equalTo(avg)).body("reviewCount", equalTo(count));
    }

    @When("the customer adds that product to the wishlist")
    public void addToWishlist() {
        ctx.lastResponse(wishlist.add(ctx.token(), productId()));
    }

    @When("the customer moves that product from the wishlist to the cart")
    public void moveToCart() {
        ctx.lastResponse(wishlist.moveToCart(ctx.token(), productId()));
    }

    @Then("the wishlist should contain {int} product(s)")
    public void wishlistCount(int n) {
        wishlist.get(ctx.token()).then().statusCode(200).body("count", equalTo(n));
    }
}
