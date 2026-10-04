package com.rbctcsworld.ecommerce.coupon;

import com.rbctcsworld.ecommerce.common.exception.BusinessRuleException;
import com.rbctcsworld.ecommerce.common.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Coupon rules, checked in this order:
 *  1. code exists and is active                 -> else 400 "Invalid coupon code"
 *  2. now is inside valid_from .. valid_until    -> else 400 "Coupon has expired" / "Coupon is not active yet"
 *  3. subtotal >= min_order_amount              -> else 400 "Minimum order amount ..."
 *  4. this customer has not used it before       -> else 409 "Coupon already used"
 *  5. global usage limit not reached (atomic)   -> else 409 "Coupon usage limit reached"
 */
@Service
@Transactional
public class CouponService {

    private final CouponRepository coupons;
    private final CouponRedemptionRepository redemptions;
    private final Clock clock;

    public CouponService(CouponRepository coupons, CouponRedemptionRepository redemptions, Clock clock) {
        this.coupons = coupons;
        this.redemptions = redemptions;
        this.clock = clock;
    }

    /** Validates rules 1-4 (and a non-atomic look at rule 5) and returns the coupon. Used by quote and order. */
    @Transactional(readOnly = true)
    public Coupon validate(String rawCode, Long userId, BigDecimal subtotal) {
        String code = normalize(rawCode);
        Coupon c = coupons.findByCode(code)
                .filter(Coupon::isActive)
                .orElseThrow(() -> new BusinessRuleException("Invalid coupon code: " + code));

        LocalDateTime now = LocalDateTime.now(clock);
        if (c.getValidFrom() != null && now.isBefore(c.getValidFrom())) {
            throw new BusinessRuleException("Coupon is not active yet: " + code);
        }
        if (c.getValidUntil() != null && now.isAfter(c.getValidUntil())) {
            throw new BusinessRuleException("Coupon has expired: " + code);
        }
        if (subtotal.compareTo(c.getMinOrderAmount()) < 0) {
            throw new BusinessRuleException("Minimum order amount for " + code + " is " + c.getMinOrderAmount());
        }
        if (redemptions.existsByCouponIdAndUserId(c.getId(), userId)) {
            throw new ConflictException("Coupon already used: " + code);
        }
        if (c.usageLimitReached()) {
            throw new ConflictException("Coupon usage limit reached: " + code);
        }
        return c;
    }

    /** Records the redemption inside the order transaction; the atomic counter protects the global limit. */
    public void redeem(Coupon coupon, Long userId, Long orderId) {
        if (coupons.incrementUsage(coupon.getId()) == 0) {
            throw new ConflictException("Coupon usage limit reached: " + coupon.getCode());
        }
        redemptions.save(new CouponRedemption(coupon.getId(), userId, orderId));
    }

    public Coupon create(CouponDtos.CreateCouponRequest r) {
        String code = normalize(r.code());
        if (coupons.existsByCode(code)) {
            throw new ConflictException("Coupon code already exists: " + code);
        }
        if (Coupon.PERCENT.equals(r.type()) && r.value().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessRuleException("PERCENT coupon value must be between 0 and 100");
        }
        if (r.validFrom() != null && r.validUntil() != null && r.validUntil().isBefore(r.validFrom())) {
            throw new BusinessRuleException("validUntil must be after validFrom");
        }
        return coupons.save(new Coupon(code, r.type(), r.value(), r.minOrderAmount(), r.maxUses(),
                r.validFrom(), r.validUntil()));
    }

    @Transactional(readOnly = true)
    public List<Coupon> all() {
        return coupons.findAllByOrderByIdAsc();
    }

    public static String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }
}
