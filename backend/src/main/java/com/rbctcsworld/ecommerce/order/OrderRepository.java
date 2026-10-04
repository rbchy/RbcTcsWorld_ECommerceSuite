package com.rbctcsworld.ecommerce.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    List<CustomerOrder> findByUserIdOrderByIdDesc(Long userId);

    /** Owner check built into the query: another customer's order id behaves as "not found" (IDOR). */
    Optional<CustomerOrder> findByIdAndUserId(Long id, Long userId);

    List<CustomerOrder> findAllByOrderByIdDesc();

    Optional<CustomerOrder> findByTrackingNumber(String trackingNumber);

    boolean existsByTrackingNumber(String trackingNumber);
}
