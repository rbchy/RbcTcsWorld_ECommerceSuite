package com.rbctcsworld.ecommerce.qa.stepdefinitions;

import com.rbctcsworld.ecommerce.qa.api.ProductClient;
import com.rbctcsworld.ecommerce.qa.context.ScenarioContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

public class CatalogSteps {

    private final ScenarioContext ctx;
    private final ProductClient products = new ProductClient();

    public CatalogSteps(ScenarioContext ctx) {
        this.ctx = ctx;
    }

    @When("I request the product catalog")
    public void requestCatalog() {
        ctx.lastResponse(products.list());
    }

    @When("I search products for {string}")
    public void search(String q) {
        ctx.lastResponse(products.search(q));
    }

    @When("I request product id {long}")
    public void getById(long id) {
        ctx.lastResponse(products.get(id));
    }

    @Then("the catalog should contain at least {int} products")
    public void atLeast(int n) {
        ctx.lastResponse().then().body("size()", greaterThanOrEqualTo(n));
    }

    @Then("every product name should contain {string}")
    public void everyName(String text) {
        ctx.lastResponse().then().body("name", everyItem(containsStringIgnoringCase(text)));
    }
}
