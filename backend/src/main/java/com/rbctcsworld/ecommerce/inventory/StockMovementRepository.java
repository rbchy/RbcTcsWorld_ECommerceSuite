package com.rbctcsworld.ecommerce.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByProductIdOrderByIdAsc(Long productId);

    List<StockMovement> findByOrderIdOrderByIdAsc(Long orderId);
}
