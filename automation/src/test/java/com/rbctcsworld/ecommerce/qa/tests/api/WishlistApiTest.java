package com.rbctcsworld.ecommerce.qa.tests.api;

import com.rbctcsworld.ecommerce.qa.api.CartClient;
import com.rbctcsworld.ecommerce.qa.api.ProductClient;
import com.rbctcsworld.ecommerce.qa.api.WishlistClient;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures;
import com.rbctcsworld.ecommerce.qa.testdata.Fixtures.Customer;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/** Module 5 - wishlist. */
@Tag("api")
@Tag("wishlist")
@Epic("Customer engagement")
@Feature("Wishlist")
class WishlistApiTest {

    private final WishlistClient wishlist = new WishlistClient();
    private final CartClient cart = new CartClient();
    private final ProductClient products = new ProductClient();
    private Customer customer;
    private long productId;

    @BeforeEach
    void setUp() {
        customer = Fixtures.newCustomer();
        productId = Fixtures.product("40.00", 5);
    }

    @Test
    @Tag("smoke")
    @DisplayName("Add is idempotent: 201 first time, 200 second time, still one line")
    void idempotentAdd() {
        wishlist.add(customer.token(), productId).then().statusCode(201).body("count", equalTo(1));
        wishlist.add(customer.token(), productId).then().statusCode(200).body("count", equalTo(1));
    }

    @Test
    @DisplayName("Price drop: shows how much cheaper the product is than when it was added")
    void priceDrop() {
        wishlist.add(customer.token(), productId).then().statusCode(201);
        products.update(Fixtures.adminToken(), productId,
                ProductClient.body("QA Item", com.rbctcsworld.ecommerce.qa.testdata.TestData.uniqueSku(), "qa", new BigDecimal("31.50"), 5))
                .then().statusCode(200);

        wishlist.get(customer.token()).then().statusCode(200)
                .body("items[0].priceWhenAdded", equalTo(40.0f))
                .body("items[0].currentPrice", equalTo(31.5f))
                .body("items[0].priceDrop", equalTo(8.5f));
    }

    @Test
    @Tag("smoke")
    @DisplayName("Move to cart: product leaves the wishlist and appears in the cart with quantity 1")
    void moveToCart() {
        wishlist.add(customer.token(), productId).then().statusCode(201);
        wishlist.moveToCart(customer.token(), productId).then().statusCode(200)
                .body("items.productId", contains((int) productId))
                .body("items[0].quantity", equalTo(1));
        wishlist.get(customer.token()).then().body("count", equalTo(0));
    }

    @Test
    @DisplayName("Move to cart of a sold-out product: 409 and it STAYS on the wishlist (transaction rolled back)")
    void moveToCartSoldOut() {
        long soldOut = Fixtures.product("9.99", 0);
        wishlist.add(customer.token(), soldOut).then().statusCode(201);

        wishlist.moveToCart(customer.token(), soldOut).then().statusCode(409).body("message", containsString("stock"));

        wishlist.get(customer.token()).then().body("count", equalTo(1)).body("items[0].available", equalTo(false));
        cart.get(customer.token()).then().body("totalQuantity", equalTo(0));
    }

    @Test
    @DisplayName("Deleted (inactive) product cannot be added, and shows as unavailable if already on the list")
    void inactiveProduct() {
        wishlist.add(customer.token(), productId).then().statusCode(201);
        products.delete(Fixtures.adminToken(), productId).then().statusCode(204);

        wishlist.get(customer.token()).then().body("items[0].available", equalTo(false));
        wishlist.add(Fixtures.newCustomer().token(), productId).then().statusCode(404);
    }

    @Test
    @DisplayName("Remove: 200 the first time, 404 when it is not on the wishlist")
    void remove() {
        wishlist.add(customer.token(), productId).then().statusCode(201);
        wishlist.remove(customer.token(), productId).then().statusCode(200).body("count", equalTo(0));
        wishlist.remove(customer.token(), productId).then().statusCode(404);
    }

    @Test
    @Tag("security")
    @DisplayName("Wishlists are private: other customers see their own empty list and cannot remove my items")
    void privacy() {
        wishlist.add(customer.token(), productId).then().statusCode(201);
        Customer other = Fixtures.newCustomer();
        wishlist.get(other.token()).then().body("count", equalTo(0));
        wishlist.remove(other.token(), productId).then().statusCode(404);
        wishlist.get(customer.token()).then().body("count", equalTo(1));
        wishlist.get(null).then().statusCode(401);
    }

    @Test
    @DisplayName("Validation: missing productId 400, unknown product 404")
    void validation() {
        wishlist.add(customer.token(), null).then().statusCode(400);
        wishlist.add(customer.token(), 99_999_999L).then().statusCode(404);
    }

    @Test
    @DisplayName("At most 50 products: the 51st is rejected with 400")
    void limit() {
        for (int i = 0; i < 50; i++) {
            wishlist.add(customer.token(), Fixtures.product("1.00", 1)).then().statusCode(201);
        }
        wishlist.add(customer.token(), productId).then().statusCode(400).body("message", containsString("50"));
        wishlist.get(customer.token()).then().body("count", equalTo(50));
    }

}
