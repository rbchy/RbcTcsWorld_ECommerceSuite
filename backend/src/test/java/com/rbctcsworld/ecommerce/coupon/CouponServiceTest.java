package com.rbctcsworld.ecommerce.coupon;

import com.rbctcsworld.ecommerce.common.exception.BusinessRuleException;
import com.rbctcsworld.ecommerce.common.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    /** Fixed "now" = 2026-06-15 12:00 so validity windows are deterministic. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock CouponRepository coupons;
    @Mock CouponRedemptionRepository redemptions;
    private CouponService service;

    @BeforeEach
    void setUp() {
        service = new CouponService(coupons, redemptions, CLOCK);
    }

    private Coupon coupon(String code, String type, String value, String min, Integer maxUses,
                          LocalDateTime from, LocalDateTime until) {
        Coupon c = new Coupon(code, type, new BigDecimal(value), new BigDecimal(min), maxUses, from, until);
        ReflectionTestUtils.setField(c, "id", 1L);
        return c;
    }

    @Test
    void codeIsCaseInsensitiveAndTrimmed() {
        Coupon c = coupon("SAVE5", Coupon.FIXED, "5.00", "0", null, null, null);
        when(coupons.findByCode("SAVE5")).thenReturn(Optional.of(c));

        assertThat(service.validate("  save5 ", 9L, new BigDecimal("10.00"))).isSameAs(c);
    }

    @Test
    void unknownOrInactiveCodeIsInvalid() {
        when(coupons.findByCode("NOPE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.validate("nope", 9L, BigDecimal.TEN))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Invalid coupon code");

        Coupon off = coupon("OFF", Coupon.FIXED, "5.00", "0", null, null, null);
        off.setActive(false);
        when(coupons.findByCode("OFF")).thenReturn(Optional.of(off));
        assertThatThrownBy(() -> service.validate("OFF", 9L, BigDecimal.TEN))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Invalid coupon code");
    }

    @Test
    void validityWindowBoundaries() {
        Coupon expired = coupon("OLD", Coupon.PERCENT, "10", "0", null, null, NOW.minusSeconds(1));
        Coupon future = coupon("NEW", Coupon.PERCENT, "10", "0", null, NOW.plusSeconds(1), null);
        Coupon lastSecond = coupon("EDGE", Coupon.PERCENT, "10", "0", null, null, NOW);
        when(coupons.findByCode("OLD")).thenReturn(Optional.of(expired));
        when(coupons.findByCode("NEW")).thenReturn(Optional.of(future));
        when(coupons.findByCode("EDGE")).thenReturn(Optional.of(lastSecond));

        assertThatThrownBy(() -> service.validate("OLD", 9L, BigDecimal.TEN)).hasMessageContaining("expired");
        assertThatThrownBy(() -> service.validate("NEW", 9L, BigDecimal.TEN)).hasMessageContaining("not active yet");
        assertThat(service.validate("EDGE", 9L, BigDecimal.TEN)).isSameAs(lastSecond); // valid until == now is OK
    }

    @Test
    void minimumOrderAmountBoundary() {
        Coupon c = coupon("SAVE5", Coupon.FIXED, "5.00", "25.00", null, null, null);
        when(coupons.findByCode("SAVE5")).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.validate("SAVE5", 9L, new BigDecimal("24.99")))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Minimum order amount");
        assertThat(service.validate("SAVE5", 9L, new BigDecimal("25.00"))).isSameAs(c);
    }

    @Test
    void secondUseBySameCustomerIsConflict() {
        Coupon c = coupon("WELCOME10", Coupon.PERCENT, "10", "0", null, null, null);
        when(coupons.findByCode("WELCOME10")).thenReturn(Optional.of(c));
        when(redemptions.existsByCouponIdAndUserId(1L, 9L)).thenReturn(true);

        assertThatThrownBy(() -> service.validate("WELCOME10", 9L, BigDecimal.TEN))
                .isInstanceOf(ConflictException.class).hasMessageContaining("already used");
    }

    @Test
    void redeemFailsWhenAtomicCounterSaysLimitReached() {
        Coupon c = coupon("LIMITED", Coupon.FIXED, "1.00", "0", 1, null, null);
        when(coupons.incrementUsage(1L)).thenReturn(0);

        assertThatThrownBy(() -> service.redeem(c, 9L, 100L))
                .isInstanceOf(ConflictException.class).hasMessageContaining("usage limit");
        verify(redemptions, never()).save(any());
    }

    @Test
    void discountMaths() {
        assertThat(coupon("P", Coupon.PERCENT, "10", "0", null, null, null).discountFor(new BigDecimal("33.35")))
                .isEqualByComparingTo("3.34");                       // 3.335 rounds HALF_UP
        assertThat(coupon("F", Coupon.FIXED, "5.00", "0", null, null, null).discountFor(new BigDecimal("3.00")))
                .isEqualByComparingTo("3.00");                       // capped at subtotal
    }

    @Test
    void percentAbove100IsRejectedOnCreate() {
        when(coupons.existsByCode("BIG")).thenReturn(false);
        assertThatThrownBy(() -> service.create(new CouponDtos.CreateCouponRequest(
                "big", Coupon.PERCENT, new BigDecimal("150"), null, null, null, null)))
                .isInstanceOf(BusinessRuleException.class);
    }
}
