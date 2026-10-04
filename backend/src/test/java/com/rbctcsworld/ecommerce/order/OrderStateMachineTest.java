package com.rbctcsworld.ecommerce.order;

import com.rbctcsworld.ecommerce.common.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * State-transition testing: EVERY (from, to) pair of the 7 statuses = 49 cases.
 * Allowed pairs must succeed and add a timeline event; all others must throw 409 and change nothing.
 */
class OrderStateMachineTest {

    static Stream<Arguments> allPairs() {
        return OrderStatus.ALL.stream()
                .flatMap(from -> OrderStatus.ALL.stream().map(to -> Arguments.of(from, to, OrderStatus.canMove(from, to))));
    }

    private static CustomerOrder orderIn(String status) {
        CustomerOrder o = new CustomerOrder("ORD-T", 1L);
        ReflectionTestUtils.setField(o, "status", status);
        return o;
    }

    @ParameterizedTest(name = "{0} -> {1} allowed={2}")
    @MethodSource("allPairs")
    void transitionTable(String from, String to, boolean allowed) {
        CustomerOrder o = orderIn(from);
        int eventsBefore = o.getEvents().size();

        if (allowed) {
            o.transitionTo(to, "test");
            assertThat(o.getStatus()).isEqualTo(to);
            assertThat(o.getEvents()).hasSize(eventsBefore + 1);
            assertThat(o.getEvents().get(eventsBefore).getStatus()).isEqualTo(to);
        } else {
            assertThatThrownBy(() -> o.transitionTo(to, "test"))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining(from).hasMessageContaining(to);
            assertThat(o.getStatus()).isEqualTo(from);
            assertThat(o.getEvents()).hasSize(eventsBefore);
        }
    }

    @Test
    void terminalStatesHaveNoExit() {
        assertThat(OrderStatus.ALLOWED.get(OrderStatus.CANCELLED)).isEmpty();
        assertThat(OrderStatus.ALLOWED.get(OrderStatus.RETURNED)).isEmpty();
    }

    @Test
    void everyStatusIsInTheTable() {
        assertThat(OrderStatus.ALLOWED.keySet()).containsExactlyInAnyOrderElementsOf(OrderStatus.ALL);
    }

    @Test
    void newOrderStartsPlacedWithOneEvent() {
        CustomerOrder o = new CustomerOrder("ORD-T", 1L);
        assertThat(o.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(o.getEvents()).singleElement().satisfies(e -> assertThat(e.getStatus()).isEqualTo(OrderStatus.PLACED));
    }

    @Test
    void shipAndDeliverSetTimestampsAndTracking() {
        CustomerOrder o = orderIn(OrderStatus.PAID);
        o.ship("UPS", "UPS-123456789012");
        assertThat(o.getTrackingNumber()).isEqualTo("UPS-123456789012");
        assertThat(o.getShippedAt()).isNotNull();
        o.deliver();
        assertThat(o.getDeliveredAt()).isNotNull();
        assertThat(o.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }
}
