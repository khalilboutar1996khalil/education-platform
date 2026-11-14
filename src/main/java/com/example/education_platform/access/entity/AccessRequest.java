package com.example.education_platform.access.entity;

import com.example.education_platform.common.BaseEntity;
import com.example.education_platform.storage.entity.StoredFile;
import com.example.education_platform.subscription.entity.PaymentMethod;
import com.example.education_platform.subscription.entity.SubscriptionPlan;
import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Somebody asking to join, from the public "demander un accès" form.
 *
 * <p>It holds no {@code user_id} until an admin approves it, which is the whole reason this is a
 * table of its own rather than a User with a PENDING status: the person has no account yet, and a
 * half-real account that cannot log in is worse than a request that plainly is not one.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "access_requests")
public class AccessRequest extends BaseEntity {

    @Column(nullable = false)
    private String fullName;

    /** Not unique: somebody refused once may reasonably ask again. */
    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 20)
    private Level level;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccessRequestStatus status = AccessRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    private Instant reviewedAt;

    /** Set on approval: the account this request turned into. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_user_id")
    private User createdUser;

    @Column(length = 1000)
    private String decisionNote;

    /** The plan paid for; null for a level that has no plan, whose request stays free. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private SubscriptionPlan plan;

    /** Copied from the plan when asking, so a later price change does not rewrite this request. */
    @Column(name = "price_dt", precision = 10, scale = 3)
    private BigDecimal price;

    private Integer durationDays;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod paymentMethod;

    /** The D17 transaction number or transfer reference the admin checks against their account. */
    @Column(length = 100)
    private String paymentReference;

    /** The receipt of the payment, when the student attached one. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_file_id")
    private StoredFile receipt;

    /** Set when an existing student renews: accepting extends this account instead of creating one. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "renewal_of_id")
    private User renewalOf;

    public AccessRequest(String fullName, String email, Level level, String message) {
        this.fullName = fullName;
        this.email = email;
        this.level = level;
        this.message = message;
    }

    /** Records the plan chosen and how it was paid, at today's price. */
    public void subscribe(SubscriptionPlan chosen, PaymentMethod method, String reference) {
        this.plan = chosen;
        this.price = chosen.getPrice();
        this.durationDays = chosen.getDurationDays();
        this.paymentMethod = method;
        this.paymentReference = reference;
    }

    public boolean isRenewal() {
        return renewalOf != null;
    }

    public boolean isPending() {
        return status == AccessRequestStatus.PENDING;
    }

    public void approve(User reviewer, User createdUser, Instant now) {
        this.status = AccessRequestStatus.APPROVED;
        this.reviewedBy = reviewer;
        this.createdUser = createdUser;
        this.reviewedAt = now;
    }

    public void reject(User reviewer, String note, Instant now) {
        this.status = AccessRequestStatus.REJECTED;
        this.reviewedBy = reviewer;
        this.decisionNote = note;
        this.reviewedAt = now;
    }
}
