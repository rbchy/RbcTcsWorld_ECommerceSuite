package com.rbctcsworld.ecommerce.wishlist;

import com.rbctcsworld.ecommerce.cart.CartDtos.CartResponse;
import com.rbctcsworld.ecommerce.wishlist.WishlistDtos.AddRequest;
import com.rbctcsworld.ecommerce.wishlist.WishlistDtos.AddResult;
import com.rbctcsworld.ecommerce.wishlist.WishlistDtos.WishlistResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * All endpoints need a JWT.
 * GET    /api/wishlist                             200
 * POST   /api/wishlist            {productId}      201 added | 200 already there | 400 full | 404 product
 * DELETE /api/wishlist/{productId}                 200 wishlist | 404 not on wishlist
 * POST   /api/wishlist/{productId}/move-to-cart    200 cart | 404 | 409 out of stock (stays on wishlist)
 */
@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService service;

    public WishlistController(WishlistService service) {
        this.service = service;
    }

    @GetMapping
    public WishlistResponse get(Authentication auth) {
        return service.get(auth.getName());
    }

    @PostMapping
    public ResponseEntity<WishlistResponse> add(Authentication auth, @Valid @RequestBody AddRequest r) {
        AddResult result = service.add(auth.getName(), r.productId());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.wishlist());
    }

    @DeleteMapping("/{productId}")
    public WishlistResponse remove(Authentication auth, @PathVariable Long productId) {
        return service.remove(auth.getName(), productId);
    }

    @PostMapping("/{productId}/move-to-cart")
    public CartResponse moveToCart(Authentication auth, @PathVariable Long productId) {
        return service.moveToCart(auth.getName(), productId);
    }
}
