package com.example.education_platform.coupon.repository;

import com.example.education_platform.coupon.entity.CouponBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CouponBatchRepository extends JpaRepository<CouponBatch, Long> {

    @Query("select b from CouponBatch b order by b.createdAt desc, b.id desc")
    Page<CouponBatch> findLatestFirst(Pageable pageable);
}
