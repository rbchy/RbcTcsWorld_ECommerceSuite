package com.rbctcsworld.ecommerce.coupon;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    boolean existsByCode(String code);

    List<Coupon> findAllByOrderByIdAsc();

    /** Atomic usage counter: returns 0 when the global usage limit is already reached (same idea as stock). */
    @Modifying(flushAutomatically = true)
    @Query("update Coupon c set c.usedCount = c.usedCount + 1 where c.id = :id and (c.maxUses is null or c.usedCount < c.maxUses)")
    int incrementUsage(@Param("id") Long id);
}
