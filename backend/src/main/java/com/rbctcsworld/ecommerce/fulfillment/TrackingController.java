package com.rbctcsworld.ecommerce.fulfillment;

import com.rbctcsworld.ecommerce.order.OrderDtos.TrackingResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** GET /api/tracking/{trackingNumber} - PUBLIC (no login), like a carrier's tracking page. 200 | 404 */
@RestController
@RequestMapping("/api/tracking")
public class TrackingController {

    private final FulfillmentService fulfillment;

    public TrackingController(FulfillmentService fulfillment) {
        this.fulfillment = fulfillment;
    }

    @GetMapping("/{trackingNumber}")
    public TrackingResponse track(@PathVariable String trackingNumber) {
        return fulfillment.track(trackingNumber);
    }
}
