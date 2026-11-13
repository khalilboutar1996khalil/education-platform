package com.example.education_platform.subscription.entity;

import com.example.education_platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Where students send the money, shown on the public form. There is only ever one row. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payment_settings")
public class PaymentSettings extends BaseEntity {

    @Column(length = 40)
    private String d17Number;

    @Column(length = 120)
    private String bankName;

    @Column(length = 40)
    private String rib;

    @Column(length = 120)
    private String accountHolder;

    @Column(length = 1000)
    private String instructions;
}
