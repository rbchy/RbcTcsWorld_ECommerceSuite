package com.rbctcsworld.ecommerce.wishlist;

import com.rbctcsworld.ecommerce.auth.AppUser;
import com.rbctcsworld.ecommerce.auth.UserRepository;
import com.rbctcsworld.ecommerce.cart.CartService;
import com.rbctcsworld.ecommerce.common.exception.BusinessRuleException;
import com.rbctcsworld.ecommerce.common.exception.ConflictException;
import com.rbctcsworld.ecommerce.common.exception.NotFoundException;
import com.rbctcsworld.ecommerce.product.Product;
import com.rbctcsworld.ecommerce.product.ProductRepository;
import com.rbctcsworld.ecommerce.wishlist.WishlistDtos.AddResult;
import com.rbctcsworld.ecommerce.wishlist.WishlistDtos.WishlistLine;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    private static final String EMAIL = "w@test.com";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-01T10:00:00Z"), ZoneOffset.UTC);

    @Mock WishlistItemRepository items;
    @Mock ProductRepository products;
    @Mock UserRepository users;
    @Mock CartService cart;
    private WishlistService service;
    private Product product;

    @BeforeEach
    void setUp() {
        service = new WishlistService(items, products, users, cart, CLOCK);
        AppUser user = new AppUser(EMAIL, "x");
        ReflectionTestUtils.setField(user, "id", 7L);
        lenient().when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        product = new Product("Lamp", "SKU-L", "home", new BigDecimal("40.00"), 3);
        ReflectionTestUtils.setField(product, "id", 1L);
    }

    @Test
    void addingTwiceIsIdempotent() {
        when(products.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(product));
        when(items.findByUserIdAndProductId(7L, 1L)).thenReturn(Optional.of(new WishlistItem(7L, product, now())));

        AddResult r = service.add(EMAIL, 1L);

        assertThat(r.created()).isFalse();
        verify(items, never()).save(any());
    }

    @Test
    void fullWishlistRejectsTheNextProduct() {
        when(products.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(product));
        when(items.findByUserIdAndProductId(7L, 1L)).thenReturn(Optional.empty());
        when(items.countByUserId(7L)).thenReturn((long) WishlistService.MAX_ITEMS);

        assertThatThrownBy(() -> service.add(EMAIL, 1L)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("50");
    }

    @Test
    void inactiveProductCannotBeAdded() {
        when(products.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.add(EMAIL, 1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void priceDropIsShownOnlyWhenThePriceWentDown() {
        WishlistItem item = new WishlistItem(7L, product, now());     // added at 40.00
        product.update("Lamp", "SKU-L", "home", new BigDecimal("31.50"), 3);
        WishlistLine down = WishlistService.toLine(item);
        assertThat(down.priceDrop()).isEqualByComparingTo("8.50");
        assertThat(down.priceWhenAdded()).isEqualByComparingTo("40.00");

        product.update("Lamp", "SKU-L", "home", new BigDecimal("45.00"), 3);
        assertThat(WishlistService.toLine(item).priceDrop()).isEqualByComparingTo("0.00");
    }

    @Test
    void outOfStockOrInactiveIsNotAvailable() {
        WishlistItem item = new WishlistItem(7L, product, now());
        product.setStock(0);
        assertThat(WishlistService.toLine(item).available()).isFalse();
        product.setStock(2);
        product.setActive(false);
        assertThat(WishlistService.toLine(item).available()).isFalse();
    }

    @Test
    void moveToCartRemovesFromWishlist() {
        WishlistItem item = new WishlistItem(7L, product, now());
        when(items.findByUserIdAndProductId(7L, 1L)).thenReturn(Optional.of(item));

        service.moveToCart(EMAIL, 1L);

        verify(cart).addItem(EMAIL, 1L, 1);
        verify(items).delete(item);
    }

    @Test
    void whenTheCartRefusesTheProductStaysOnTheWishlist() {
        WishlistItem item = new WishlistItem(7L, product, now());
        when(items.findByUserIdAndProductId(7L, 1L)).thenReturn(Optional.of(item));
        when(cart.addItem(EMAIL, 1L, 1)).thenThrow(new ConflictException("Insufficient stock"));

        assertThatThrownBy(() -> service.moveToCart(EMAIL, 1L)).isInstanceOf(ConflictException.class);
        verify(items, never()).delete(any());
    }

    @Test
    void removingSomethingNotOnTheWishlistIs404() {
        when(items.findByUserIdAndProductId(7L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.remove(EMAIL, 1L)).isInstanceOf(NotFoundException.class);
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(CLOCK);
    }
}
