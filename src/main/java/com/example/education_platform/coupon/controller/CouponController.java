package com.example.education_platform.coupon.controller;

import com.example.education_platform.auth.dto.response.LoginResponse;
import com.example.education_platform.common.PageResponse;
import com.example.education_platform.coupon.dto.request.CreateCouponBatchRequest;
import com.example.education_platform.coupon.dto.request.RedeemCouponRequest;
import com.example.education_platform.coupon.dto.response.CouponBatchResponse;
import com.example.education_platform.coupon.dto.response.CouponResponse;
import com.example.education_platform.coupon.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons", description = "Book coupons: printing them, and students activating their book")
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/batches")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generate one coupon per printed copy of a book, for one level")
    CouponBatchResponse createBatch(@Valid @RequestBody CreateCouponBatchRequest request) {
        return couponService.createBatch(request);
    }

    @GetMapping("/batches")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Print runs, latest first, with how many coupons each has had used")
    PageResponse<CouponBatchResponse> listBatches(@ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return couponService.listBatches(pageable);
    }

    @GetMapping("/batches/{id}/coupons")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Every coupon of a print run, secret codes included, to print or follow up")
    List<CouponResponse> listCoupons(@PathVariable Long id) {
        return couponService.listCoupons(id);
    }

    @PostMapping("/redeem")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements // public: the student has no account until this call creates it
    @Operation(summary = "Activate a book: creates the student account for its level and signs it in")
    LoginResponse redeem(@Valid @RequestBody RedeemCouponRequest request, HttpServletRequest http) {
        return couponService.redeem(request, http.getHeader("User-Agent"), http.getRemoteAddr());
    }
}
