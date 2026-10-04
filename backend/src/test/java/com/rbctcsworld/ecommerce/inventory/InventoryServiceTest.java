package com.rbctcsworld.ecommerce.inventory;

import com.rbctcsworld.ecommerce.common.exception.ConflictException;
import com.rbctcsworld.ecommerce.product.Product;
import com.rbctcsworld.ecommerce.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock ProductRepository products;
    @Mock StockMovementRepository movements;
    @InjectMocks InventoryService inventory;

    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product("Mouse", "SKU-M", "electronics", new BigDecimal("20.00"), 5);
        ReflectionTestUtils.setField(product, "id", 1L);
    }

    @Test
    void reserveRecordsNegativeMovementWithStockAfter() {
        when(products.decrementStock(1L, 2)).thenReturn(1);
        when(products.currentStock(1L)).thenReturn(3);

        inventory.reserve(product, 2, 100L);

        ArgumentCaptor<StockMovement> m = ArgumentCaptor.forClass(StockMovement.class);
        verify(movements).save(m.capture());
        assertThat(m.getValue().getChangeQty()).isEqualTo(-2);
        assertThat(m.getValue().getStockAfter()).isEqualTo(3);
        assertThat(m.getValue().getReason()).isEqualTo(StockMovement.ORDER_PLACED);
        assertThat(m.getValue().getOrderId()).isEqualTo(100L);
    }

    @Test
    void reserveFailsWithAvailableQuantityInMessage() {
        when(products.decrementStock(1L, 9)).thenReturn(0);
        when(products.currentStock(1L)).thenReturn(5);

        assertThatThrownBy(() -> inventory.reserve(product, 9, 100L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("requested 9, available 5");
        verify(movements, never()).save(any());
    }

    @Test
    void reserveOfDeactivatedProductSaysNoLongerAvailable() {
        product.setActive(false);
        when(products.decrementStock(1L, 1)).thenReturn(0);

        assertThatThrownBy(() -> inventory.reserve(product, 1, 100L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("no longer available");
    }

    @Test
    void releaseRecordsPositiveMovement() {
        when(products.currentStock(1L)).thenReturn(7);

        inventory.release(1L, 2, 100L);

        verify(products).incrementStock(1L, 2);
        ArgumentCaptor<StockMovement> m = ArgumentCaptor.forClass(StockMovement.class);
        verify(movements).save(m.capture());
        assertThat(m.getValue().getChangeQty()).isEqualTo(2);
        assertThat(m.getValue().getReason()).isEqualTo(StockMovement.ORDER_CANCELLED);
        assertThat(m.getValue().getStockAfter()).isEqualTo(7);
    }
}
