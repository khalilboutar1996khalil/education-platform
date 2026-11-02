package com.example.education_platform.coupon.repository;

import com.example.education_platform.coupon.entity.Coupon;
import com.example.education_platform.user.entity.UserStatus;
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

    /** The book a waiting student activated, if their coupon still exists. */
    Optional<Coupon> findByRedeemedById(Long userId);

    @EntityGraph(attributePaths = "redeemedBy")
    List<Coupon> findByBatchIdOrderBySerialAsc(Long batchId);

    /** Activations the admin has accepted; pending ones are counted separately. */
    @Query("""
            select count(c) from Coupon c
            where c.batch.id = :batchId and c.redeemedBy is not null and c.redeemedBy.status <> :pending
            """)
    long countRedeemed(@Param("batchId") Long batchId, @Param("pending") UserStatus pending);

    @Query("select count(c) from Coupon c where c.batch.id = :batchId and c.redeemedBy.status = :pending")
    long countPending(@Param("batchId") Long batchId, @Param("pending") UserStatus pending);

    /** The admin's queue, oldest request first. */
    @Query("""
            select c from Coupon c join fetch c.batch join fetch c.redeemedBy u
            where u.status = :pending
            order by c.redeemedAt asc
            """)
    List<Coupon> findPending(@Param("pending") UserStatus pending);

    @EntityGraph(attributePaths = {"batch", "redeemedBy"})
    Optional<Coupon> findDetailById(Long id);
}
