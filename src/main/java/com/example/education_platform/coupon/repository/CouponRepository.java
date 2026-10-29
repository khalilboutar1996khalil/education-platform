package com.example.education_platform.coupon.repository;

import com.example.education_platform.coupon.entity.Coupon;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /** Locked, so two people typing the same code at the same moment cannot both redeem it. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coupon c join fetch c.batch where c.serial = :serial")
    Optional<Coupon> findForRedemption(@Param("serial") String serial);

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = "redeemedBy")
    List<Coupon> findByBatchIdOrderBySerialAsc(Long batchId);

    @Query("select count(c) from Coupon c where c.batch.id = :batchId and c.redeemedAt is not null")
    long countRedeemed(@Param("batchId") Long batchId);
}
