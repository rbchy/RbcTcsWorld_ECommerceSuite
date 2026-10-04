package com.rbctcsworld.ecommerce.qa.stepdefinitions;

import com.rbctcsworld.ecommerce.qa.api.CheckoutClient;
import com.rbctcsworld.ecommerce.qa.api.OrderClient;
import com.rbctcsworld.ecommerce.qa.context.ScenarioContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.hamcrest.Matchers.equalTo;

public class CheckoutSteps {

    private final ScenarioContext ctx;
    private final CheckoutClient checkout = new CheckoutClient();
    private final OrderClient orders = new OrderClient();

    public CheckoutSteps(ScenarioContext ctx) {
        this.ctx = ctx;
    }

    @When("the customer asks for a price quote")
    public void quote() {
        ctx.lastResponse(checkout.quote(ctx.token(), null));
    }

    @When("the customer asks for a price quote with coupon {string}")
    public void quoteWithCoupon(String coupon) {
        ctx.lastResponse(checkout.quote(ctx.token(), coupon));
    }

    @When("the customer pays with card {string}")
    public void pay(String card) {
        long orderId = ctx.get("orderId");
        ctx.lastResponse(orders.pay(ctx.token(), orderId, card));
    }

    @Then("the quote should show discount {string}, shipping {string}, tax {string} and total {string}")
    public void quoteShows(String discount, String shipping, String tax, String total) {
        ctx.lastResponse().then()
                .body("discount", equalTo(Float.parseFloat(discount)))
                .body("shippingFee", equalTo(Float.parseFloat(shipping)))
                .body("tax", equalTo(Float.parseFloat(tax)))
                .body("total", equalTo(Float.parseFloat(total)));
    }

    @Then("the order should now be {string}")
    public void orderNowIs(String status) {
        long orderId = ctx.get("orderId");
        orders.get(ctx.token(), orderId).then().statusCode(200).body("status", equalTo(status));
    }
}
