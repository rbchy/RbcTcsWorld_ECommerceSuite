package com.rbctcsworld.ecommerce.payment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public final class PaymentDtos {

    private PaymentDtos() {
    }

    /** Card details exist only in memory for the duration of the request - never logged, never stored. */
    public record PayRequest(
            @NotBlank @Pattern(regexp = "[0-9 ]{13,23}", message = "must be 13-19 digits") String cardNumber,
            @NotNull @Min(1) @Max(12) Integer expiryMonth,
            @NotNull @Min(2000) @Max(2100) Integer expiryYear,
            @NotBlank @Pattern(regexp = "\\d{3,4}", message = "must be 3 or 4 digits") String cvv) {

        @Override
        public String toString() {   // protects against accidental logging of card data
            return "PayRequest[card=****, expiry=**/****, cvv=***]";
        }
    }
}
