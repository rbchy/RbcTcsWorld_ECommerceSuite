package com.rbctcsworld.ecommerce.fulfillment;

import com.rbctcsworld.ecommerce.common.exception.ConflictException;
import com.rbctcsworld.ecommerce.common.exception.NotFoundException;
import com.rbctcsworld.ecommerce.order.CustomerOrder;
import com.rbctcsworld.ecommerce.order.OrderDtos.OrderResponse;
import com.rbctcsworld.ecommerce.order.OrderRepository;
import com.rbctcsworld.ecommerce.order.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FulfillmentServiceTest {

    @Mock OrderRepository orders;
    @InjectMocks FulfillmentService service;

    private CustomerOrder order(String status) {
        CustomerOrder o = new CustomerOrder("ORD-F", 7L);
        ReflectionTestUtils.setField(o, "id", 3L);
        ReflectionTestUtils.setField(o, "status", status);
        when(orders.findById(3L)).thenReturn(Optional.of(o));
        return o;
    }

    @ParameterizedTest(name = "carrier {0}")
    @ValueSource(strings = {"UPS", "FEDEX", "USPS"})
    void shipPaidOrderGeneratesTrackingNumber(String carrier) {
        order(OrderStatus.PAID);
        OrderResponse r = service.ship(3L, carrier);

        assertThat(r.status()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(r.carrier()).isEqualTo(carrier);
        assertThat(r.trackingNumber()).matches(carrier + "-\\d{12}");
        assertThat(r.shippedAt()).isNotNull();
    }

    @ParameterizedTest(name = "cannot ship a {0} order")
    @ValueSource(strings = {"PLACED", "CANCELLED", "SHIPPED", "DELIVERED"})
    void onlyPaidOrdersShip(String status) {
        order(status);
        assertThatThrownBy(() -> service.ship(3L, "UPS")).isInstanceOf(ConflictException.class);
    }

    @Test
    void deliverRequiresShipped() {
        order(OrderStatus.PAID);
        assertThatThrownBy(() -> service.deliver(3L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void unknownTrackingNumberIs404() {
        when(orders.findByTrackingNumber("UPS-000000000000")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.track(" ups-000000000000 ")).isInstanceOf(NotFoundException.class);
    }
}
