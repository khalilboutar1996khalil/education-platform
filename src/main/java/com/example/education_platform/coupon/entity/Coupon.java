package com.example.education_platform.coupon.entity;

import com.example.education_platform.common.BaseEntity;
import com.example.education_platform.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The coupon printed in one copy of a book: a serial in clear next to the QR code, and a secret
 * code under a scratch-off panel. It can be redeemed once.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "coupons")
public class Coupon extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private CouponBatch batch;

    @Column(nullable = false, unique = true, length = 20)
    private String serial;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "redeemed_by_user_id")
    private User redeemedBy;

    private Instant redeemedAt;

    public Coupon(CouponBatch batch, String serial, String code) {
        this.batch = batch;
        this.serial = serial;
        this.code = code;
    }

    public boolean isRedeemed() {
        return redeemedAt != null;
    }

    public void redeem(User student, Instant now) {
        this.redeemedBy = student;
        this.redeemedAt = now;
    }
}
