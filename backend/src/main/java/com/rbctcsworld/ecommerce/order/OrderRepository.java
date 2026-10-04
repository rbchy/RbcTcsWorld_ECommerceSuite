package com.rbctcsworld.ecommerce.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    List<CustomerOrder> findByUserIdOrderByIdDesc(Long userId);

    /** Owner check built into the query: another customer's order id behaves as "not found" (IDOR). */
    Optional<CustomerOrder> findByIdAndUserId(Long id, Long userId);

    List<CustomerOrder> findAllByOrderByIdDesc();

    Optional<CustomerOrder> findByTrackingNumber(String trackingNumber);

    boolean existsByTrackingNumber(String trackingNumber);

    /** Verified purchase: did this user receive (delivered, possibly returned later) an order containing the product? */
    @Query("select count(o) > 0 from CustomerOrder o join o.items i "
            + "where o.userId = :userId and i.productId = :productId and o.status in :statuses")
    boolean hasReceived(@Param("userId") Long userId, @Param("productId") Long productId,
                        @Param("statuses") Collection<String> statuses);
}
