package com.rbctcsworld.ecommerce.qa.stepdefinitions;

import com.rbctcsworld.ecommerce.qa.api.AuthClient;
import com.rbctcsworld.ecommerce.qa.context.ScenarioContext;
import com.rbctcsworld.ecommerce.qa.testdata.TestData;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import static org.hamcrest.Matchers.equalTo;

public class CommonSteps {

    private final ScenarioContext ctx;
    private final AuthClient auth = new AuthClient();

    public CommonSteps(ScenarioContext ctx) {
        this.ctx = ctx;
    }

    @Given("a new customer is registered and logged in")
    public void newCustomer() {
        ctx.token(auth.registerAndGetToken(TestData.uniqueEmail(), TestData.PASSWORD));
    }

    @Given("the customer is not logged in")
    public void anonymous() {
        ctx.token(null);
    }

    @Then("the response status should be {int}")
    public void status(int code) {
        ctx.lastResponse().then().statusCode(code);
    }

    @Then("the error message should contain {string}")
    public void errorMessage(String text) {
        ctx.lastResponse().then().body("message", org.hamcrest.Matchers.containsString(text));
    }

    @Then("the response field {string} should be {string}")
    public void field(String path, String value) {
        ctx.lastResponse().then().body(path, equalTo(value));
    }
}
