package com.rbctcsworld.ecommerce.qa.stepdefinitions;

import com.rbctcsworld.ecommerce.qa.api.AdminClient;
import com.rbctcsworld.ecommerce.qa.api.OrderClient;
import com.rbctcsworld.ecommerce.qa.context.ScenarioContext;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.Arrays;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;

/** Module 4 steps: warehouse, tracking, returns, customer service. */
public class LifecycleSteps {

    private final ScenarioContext ctx;
    private final OrderClient orders = new OrderClient();
    private final AdminClient admin = new AdminClient();

    public LifecycleSteps(ScenarioContext ctx) {
        this.ctx = ctx;
    }

    private long orderId() {
        return ctx.get("orderId");
    }

    // Cucumber matches step text regardless of Given/When/Then, so ONE annotation serves every keyword.
    @When("the warehouse ships the order with {string}")
    public void ship(String carrier) {
        String tn = admin.ship(Fixtures.adminToken(), orderId(), carrier).then().statusCode(200)
                .body("trackingNumber", matchesPattern(carrier + "-\\d{12}"))
                .extract().path("trackingNumber");
        ctx.put("trackingNumber", tn);
    }

    @When("the order is delivered")
    public void deliver() {
        admin.deliver(Fixtures.adminToken(), orderId()).then().statusCode(200);
    }

    @Then("the tracking timeline should be {string}")
    public void timeline(String csv) {
        String[] expected = Arrays.stream(csv.split(",")).map(String::trim).toArray(String[]::new);
        orders.tracking(ctx.token(), orderId()).then().statusCode(200).body("events.status", contains(expected));
    }

    @Then("anyone can track the parcel without logging in and sees {string}")
    public void publicTracking(String status) {
        String tn = ctx.get("trackingNumber");
        orders.publicTracking(tn).then().statusCode(200)
                .body("status", equalTo(status))
                .body(not(containsString("customerId")));
    }

    @When("the customer requests a return because {string}")
    public void requestReturn(String reason) {
        var r = orders.requestReturn(ctx.token(), orderId(), reason);
        ctx.lastResponse(r);
        if (r.statusCode() == 201) {
            ctx.put("returnId", ((Number) r.path("id")).longValue());
        }
    }

    @When("customer service approves the return")
    public void approve() {
        long returnId = ctx.get("returnId");
        ctx.lastResponse(admin.approveReturn(Fixtures.adminToken(), returnId));
    }

    @When("customer service rejects the return")
    public void reject() {
        long returnId = ctx.get("returnId");
        ctx.lastResponse(admin.rejectReturn(Fixtures.adminToken(), returnId, "Rejected by customer service"));
    }

    @Then("the refund should be {string} and restocked should be {string}")
    public void refund(String amount, String restocked) {
        ctx.lastResponse().then()
                .body("refundAmount", equalTo(Float.parseFloat(amount)))
                .body("restocked", equalTo(Boolean.parseBoolean(restocked)));
    }
}
