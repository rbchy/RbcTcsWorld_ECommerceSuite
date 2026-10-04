package com.rbctcsworld.ecommerce.payment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Stand-in for a real payment provider (Stripe/Adyen style test cards). Deterministic, so tests can
 * trigger every outcome on purpose:
 *
 *   4000 0000 0000 0002  -> declined            ("Card declined")
 *   4000 0000 0000 9995  -> declined            ("Insufficient funds")
 *   any other Luhn-valid -> approved            (e.g. 4242 4242 4242 4242)
 *
 * Card format, Luhn and expiry are validated BEFORE the gateway is called (PaymentService).
 */
@Component
public class MockPaymentGateway {

    public static final String DECLINED_CARD = "4000000000000002";
    public static final String INSUFFICIENT_FUNDS_CARD = "4000000000009995";

    public GatewayResult charge(String cardNumber, BigDecimal amount) {
        return switch (cardNumber) {
            case DECLINED_CARD -> GatewayResult.decline("Card declined");
            case INSUFFICIENT_FUNDS_CARD -> GatewayResult.decline("Insufficient funds");
            default -> GatewayResult.ok();
        };
    }

    public GatewayResult refund(BigDecimal amount) {
        return GatewayResult.ok();
    }

    /** Luhn (mod 10) checksum used by every real card number. */
    public static boolean luhnValid(String digits) {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (doubleIt) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }

    public record GatewayResult(boolean approved, String reason) {
        static GatewayResult ok() { return new GatewayResult(true, null); }
        static GatewayResult decline(String reason) { return new GatewayResult(false, reason); }
    }
}
