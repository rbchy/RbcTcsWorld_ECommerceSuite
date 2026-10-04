package com.rbctcsworld.ecommerce.product;

import com.rbctcsworld.ecommerce.common.exception.ConflictException;
import com.rbctcsworld.ecommerce.common.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository repo;

    @InjectMocks
    ProductService service;

    private static Product product(long id, String sku) {
        Product p = new Product("Old name", sku, "old", new BigDecimal("10.00"), 5);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    @Test
    void updateChangesEveryField_regressionForStockOnlyBug() {
        Product existing = product(1L, "SKU-1");
        when(repo.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(existing));
        when(repo.findBySku("SKU-NEW")).thenReturn(Optional.empty());
        when(repo.save(existing)).thenReturn(existing);

        Product updated = service.update(1L,
                new ProductRequest("New name", "SKU-NEW", "books", new BigDecimal("12.50"), 9));

        assertThat(updated.getName()).isEqualTo("New name");
        assertThat(updated.getSku()).isEqualTo("SKU-NEW");
        assertThat(updated.getCategory()).isEqualTo("books");
        assertThat(updated.getPrice()).isEqualByComparingTo("12.50");
        assertThat(updated.getStock()).isEqualTo(9);
    }

    @Test
    void updateRejectsSkuUsedByAnotherProduct() {
        when(repo.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(product(1L, "SKU-1")));
        when(repo.findBySku("SKU-2")).thenReturn(Optional.of(product(2L, "SKU-2")));

        assertThatThrownBy(() -> service.update(1L,
                new ProductRequest("n", "SKU-2", null, BigDecimal.ONE, 1)))
                .isInstanceOf(ConflictException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void createRejectsDuplicateSku() {
        when(repo.existsBySku("SKU-1")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new ProductRequest("n", "SKU-1", null, BigDecimal.ONE, 1)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("SKU-1");
    }

    @Test
    void getInactiveOrMissingProductIsNotFound() {
        when(repo.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteIsSoftDelete() {
        Product existing = product(1L, "SKU-1");
        when(repo.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(existing));

        service.delete(1L);

        assertThat(existing.isActive()).isFalse();
        verify(repo).save(existing);
    }
}
