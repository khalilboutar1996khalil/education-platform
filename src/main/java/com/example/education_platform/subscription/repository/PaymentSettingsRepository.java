package com.example.education_platform.subscription.repository;

import com.example.education_platform.subscription.entity.PaymentSettings;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentSettingsRepository extends JpaRepository<PaymentSettings, Long> {

    Optional<PaymentSettings> findFirstByOrderByIdAsc();
}
