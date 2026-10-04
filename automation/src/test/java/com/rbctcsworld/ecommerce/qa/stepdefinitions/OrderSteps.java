package com.rbctcsworld.ecommerce.qa.stepdefinitions;

import com.rbctcsworld.ecommerce.qa.api.CartClient;
import com.rbctcsworld.ecommerce.qa.api.OrderClient;
import com.rbctcsworld.ecommerce.qa.api.ProductClient;
import com.rbctcsworld.ecommerce.qa.context.ScenarioContext;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;

import static org.hamcrest.Matchers.equalTo;

/** Every scenario creates its own product, so stock assertions never depend on earlier runs. */
public class OrderSteps {

    private final ScenarioContext ctx;
    private final CartClient cart = new CartClient();
    private final OrderClient orders = new OrderClient();
    private final ProductClient products = new ProductClient();

    public OrderSteps(ScenarioContext ctx) {
        this.ctx = ctx;
    }

    @Given("a product priced {string} with stock {int} exists")
    public void productExists(String price, int stock) {
        ctx.put("productId", Fixtures.product(price, stock));
    }

    @Given("the customer has {int} of that product in the cart")
    public void addThatProduct(int qty) {
        long productId = ctx.get("productId");
        cart.add(ctx.token(), productId, qty).then().statusCode(201);
    }

    @Given("the customer has placed the order")
    @When("the customer places the order")
    public void placeOrder() {
        Response r = orders.place(ctx.token());
        ctx.lastResponse(r);
        if (r.statusCode() == 201) {
            ctx.put("orderId", ((Number) r.path("id")).longValue());
        }
    }

    @When("the customer places the order with coupon {string}")
    public void placeOrderWithCoupon(String coupon) {
        Response r = orders.place(ctx.token(), coupon);
        ctx.lastResponse(r);
        if (r.statusCode() == 201) {
            ctx.put("orderId", ((Number) r.path("id")).longValue());
        }
    }

    @When("the customer cancels the order")
    public void cancelOrder() {
        long orderId = ctx.get("orderId");
        ctx.lastResponse(orders.cancel(ctx.token(), orderId));
    }

    @Then("the order status should be {string}")
    public void orderStatus(String status) {
        ctx.lastResponse().then().body("status", equalTo(status));
    }

    @Then("the order subtotal should be {string}")
    public void orderSubtotal(String subtotal) {
        ctx.lastResponse().then().body("subtotal", equalTo(Float.parseFloat(subtotal)));
    }

    /** total = subtotal - discount + shipping + tax */
    @Then("the order total should be {string}")
    public void orderTotal(String total) {
        ctx.lastResponse().then().body("total", equalTo(Float.parseFloat(total)));
    }

    @Then("the product stock should be {int}")
    public void productStock(int expected) {
        long productId = ctx.get("productId");
        products.get(productId).then().statusCode(200).body("stock", equalTo(expected));
    }
}
