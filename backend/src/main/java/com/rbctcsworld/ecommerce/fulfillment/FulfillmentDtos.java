package com.rbctcsworld.ecommerce.fulfillment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class FulfillmentDtos {

    private FulfillmentDtos() {
    }

    public record ShipRequest(
            @NotBlank @Pattern(regexp = "UPS|FEDEX|USPS", message = "must be UPS, FEDEX or USPS") String carrier) {
    }
}
