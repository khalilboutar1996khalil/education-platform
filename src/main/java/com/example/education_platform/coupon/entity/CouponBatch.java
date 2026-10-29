package com.example.education_platform.coupon.entity;

import com.example.education_platform.common.BaseEntity;
import com.example.education_platform.user.entity.Level;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One print run: a number of coupons for one level, printed together. */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "coupon_batches")
public class CouponBatch extends BaseEntity {

    @Column(nullable = false, length = 20)
    private Level level;

    @Column(nullable = false)
    private int quantity;

    private String label;

    public CouponBatch(Level level, int quantity, String label) {
        this.level = level;
        this.quantity = quantity;
        this.label = label;
    }
}
