package com.example.education_platform.subscription.entity;

import com.example.education_platform.common.BaseEntity;
import com.example.education_platform.user.entity.Level;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** The yearly plan of one level: what a student of that level pays, and for how long. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlan extends BaseEntity {

    /** Default length of a plan: one year from the day the admin accepts. */
    public static final int ONE_YEAR = 365;

    @Column(nullable = false, unique = true, length = 20)
    private Level level;

    @Column(name = "price_dt", nullable = false, precision = 10, scale = 3)
    private BigDecimal price;

    @Column(nullable = false)
    private int durationDays = ONE_YEAR;

    @Column(nullable = false)
    private boolean active = true;

    public SubscriptionPlan(Level level, BigDecimal price, int durationDays, boolean active) {
        this.level = level;
        this.price = price;
        this.durationDays = durationDays;
        this.active = active;
    }
}
