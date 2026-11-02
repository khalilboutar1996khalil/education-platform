package com.example.education_platform.coupon.service;

import com.example.education_platform.common.PageResponse;
import com.example.education_platform.coupon.dto.request.CreateCouponBatchRequest;
import com.example.education_platform.coupon.dto.request.RedeemCouponRequest;
import com.example.education_platform.coupon.dto.response.CouponBatchResponse;
import com.example.education_platform.coupon.dto.response.CouponResponse;
import com.example.education_platform.coupon.dto.response.PendingActivationResponse;
import com.example.education_platform.coupon.dto.response.RedeemCouponResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface CouponService {

    /** Generates one coupon per printed copy, each with its own serial and secret code. */
    CouponBatchResponse createBatch(CreateCouponBatchRequest request);

    PageResponse<CouponBatchResponse> listBatches(Pageable pageable);

    /** Every coupon of a batch, secrets included, in serial order: the print run. */
    List<CouponResponse> listCoupons(Long batchId);

    /**
     * Creates the student account for the book's level, waiting for the admin's approval: it cannot
     * sign in yet. Wrong serial/code pairs all get the same answer, and repeated failures on one
     * serial are throttled.
     */
    RedeemCouponResponse redeem(RedeemCouponRequest request);

    /** Book activations waiting for a decision, oldest first. */
    List<PendingActivationResponse> pending();

    /** The student can sign in from now on. */
    void approve(Long couponId);

    /** The waiting account is removed and the book's code works again. */
    void reject(Long couponId);
}
