package com.rbctcsworld.ecommerce.qa.stepdefinitions;

import com.rbctcsworld.ecommerce.qa.api.CartClient;
import com.rbctcsworld.ecommerce.qa.api.ProductClient;
import com.rbctcsworld.ecommerce.qa.context.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class CartSteps {

    private final ScenarioContext ctx;
    private final CartClient cart = new CartClient();
    private final ProductClient products = new ProductClient();

    public CartSteps(ScenarioContext ctx) {
        this.ctx = ctx;
    }

    @Given("the customer has {int} of product {string} in the cart")
    @When("the customer adds {int} of product {string} to the cart")
    public void add(int qty, String sku) {
        ctx.lastResponse(cart.add(ctx.token(), products.idBySku(sku), qty));
    }

    @When("the customer changes the quantity of the first cart line to {int}")
    public void updateFirst(int qty) {
        int itemId = cart.get(ctx.token()).then().statusCode(200).extract().path("items[0].itemId");
        ctx.lastResponse(cart.update(ctx.token(), itemId, qty));
    }

    @When("the customer opens the cart")
    public void open() {
        ctx.lastResponse(cart.get(ctx.token()));
    }

    @When("the customer empties the cart")
    public void clear() {
        ctx.lastResponse(cart.clear(ctx.token()));
    }

    @Then("the cart should have {int} line(s)")
    public void lines(int n) {
        cart.get(ctx.token()).then().statusCode(200).body("items", hasSize(n));
    }

    @Then("the cart line for {string} should have quantity {int}")
    public void lineQuantity(String sku, int qty) {
        cart.get(ctx.token()).then().statusCode(200)
                .body("items.find { it.sku == '" + sku + "' }.quantity", equalTo(qty));
    }
}
