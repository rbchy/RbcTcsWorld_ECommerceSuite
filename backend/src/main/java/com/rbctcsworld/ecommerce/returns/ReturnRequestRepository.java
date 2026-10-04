package com.rbctcsworld.ecommerce.returns;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    boolean existsByOrderId(Long orderId);

    Optional<ReturnRequest> findByOrderId(Long orderId);

    List<ReturnRequest> findByStatusOrderByIdAsc(String status);

    List<ReturnRequest> findAllByOrderByIdAsc();
}
