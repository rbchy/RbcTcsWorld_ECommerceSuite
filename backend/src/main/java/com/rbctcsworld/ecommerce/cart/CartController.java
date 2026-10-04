package com.rbctcsworld.ecommerce.cart;

import com.rbctcsworld.ecommerce.cart.CartDtos.AddItemRequest;
import com.rbctcsworld.ecommerce.cart.CartDtos.CartResponse;
import com.rbctcsworld.ecommerce.cart.CartDtos.UpdateItemRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * All endpoints need a JWT (401 otherwise). The cart always belongs to the logged-in user.
 *
 * GET    /api/cart                 200 cart
 * POST   /api/cart/items           201 cart | 400 qty rule | 404 product | 409 stock
 * PUT    /api/cart/items/{itemId}  200 cart | 400 | 404 not yours / missing | 409 stock
 * DELETE /api/cart/items/{itemId}  200 cart | 404
 * DELETE /api/cart                 204 (empties the cart)
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    @GetMapping
    public CartResponse get(Authentication auth) {
        return service.getCart(auth.getName());
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse add(Authentication auth, @Valid @RequestBody AddItemRequest r) {
        return service.addItem(auth.getName(), r.productId(), r.quantity());
    }

    @PutMapping("/items/{itemId}")
    public CartResponse update(Authentication auth, @PathVariable Long itemId, @Valid @RequestBody UpdateItemRequest r) {
        return service.updateItem(auth.getName(), itemId, r.quantity());
    }

    @DeleteMapping("/items/{itemId}")
    public CartResponse remove(Authentication auth, @PathVariable Long itemId) {
        return service.removeItem(auth.getName(), itemId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(Authentication auth) {
        service.clear(auth.getName());
    }
}
