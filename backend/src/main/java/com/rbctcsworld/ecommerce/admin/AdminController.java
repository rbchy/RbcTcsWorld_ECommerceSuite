package com.rbctcsworld.ecommerce.admin;

import com.rbctcsworld.ecommerce.coupon.Coupon;
import com.rbctcsworld.ecommerce.coupon.CouponDtos.CreateCouponRequest;
import com.rbctcsworld.ecommerce.coupon.CouponService;
import com.rbctcsworld.ecommerce.inventory.StockMovement;
import com.rbctcsworld.ecommerce.inventory.StockMovementRepository;
import com.rbctcsworld.ecommerce.order.OrderDtos.OrderResponse;
import com.rbctcsworld.ecommerce.order.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Back-office API. SecurityConfig restricts /api/admin/** to ROLE_ADMIN:
 * customer -> 403, anonymous -> 401.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final OrderService orders;
    private final StockMovementRepository movements;
    private final CouponService coupons;

    public AdminController(OrderService orders, StockMovementRepository movements, CouponService coupons) {
        this.orders = orders;
        this.movements = movements;
        this.coupons = coupons;
    }

    @GetMapping("/orders")
    public List<OrderResponse> allOrders() {
        return orders.allOrders();
    }

    /** 201 | 400 validation (e.g. PERCENT above 100) | 409 duplicate code */
    @PostMapping("/coupons")
    @ResponseStatus(HttpStatus.CREATED)
    public Coupon createCoupon(@Valid @RequestBody CreateCouponRequest r) {
        return coupons.create(r);
    }

    @GetMapping("/coupons")
    public List<Coupon> allCoupons() {
        return coupons.all();
    }

    @GetMapping("/products/{id}/stock-movements")
    public List<StockMovement> stockMovements(@PathVariable Long id) {
        return movements.findByProductIdOrderByIdAsc(id);
    }
}
