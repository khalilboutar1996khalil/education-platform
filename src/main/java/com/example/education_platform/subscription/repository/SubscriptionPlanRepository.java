package com.example.education_platform.subscription.repository;

import com.example.education_platform.subscription.entity.SubscriptionPlan;
import com.example.education_platform.user.entity.Level;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

    Optional<SubscriptionPlan> findByLevel(Level level);

    List<SubscriptionPlan> findByActiveTrue();
}
