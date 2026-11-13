package com.example.education_platform.subscription.service.impl;

import com.example.education_platform.common.exception.BusinessException;
import com.example.education_platform.common.exception.ResourceNotFoundException;
import com.example.education_platform.level.entity.SchoolLevel;
import com.example.education_platform.level.repository.SchoolLevelRepository;
import com.example.education_platform.subscription.dto.PaymentSettingsDto;
import com.example.education_platform.subscription.dto.request.PlanRequest;
import com.example.education_platform.subscription.dto.response.OfferResponse;
import com.example.education_platform.subscription.dto.response.PlanResponse;
import com.example.education_platform.subscription.entity.PaymentSettings;
import com.example.education_platform.subscription.entity.SubscriptionPlan;
import com.example.education_platform.subscription.repository.PaymentSettingsRepository;
import com.example.education_platform.subscription.repository.SubscriptionPlanRepository;
import com.example.education_platform.subscription.service.SubscriptionService;
import com.example.education_platform.user.dto.response.UserResponse;
import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.mapper.UserMapper;
import com.example.education_platform.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionPlanRepository plans;
    private final PaymentSettingsRepository paymentSettings;
    private final SchoolLevelRepository levels;
    private final UserRepository users;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public OfferResponse offer() {
        Map<String, SchoolLevel> byCode = levelsByCode();
        // Only levels that are open: a plan for a level that was switched off is not on sale
        List<PlanResponse> open = plans.findByActiveTrue().stream()
                .filter(plan -> isOpen(byCode.get(plan.getLevel().code())))
                .sorted(byPosition(byCode))
                .map(plan -> toResponse(plan, byCode))
                .toList();
        return new OfferResponse(open, paymentSettings());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanResponse> plans() {
        Map<String, SchoolLevel> byCode = levelsByCode();
        return plans.findAll().stream()
                .sorted(byPosition(byCode))
                .map(plan -> toResponse(plan, byCode))
                .toList();
    }

    @Override
    public PlanResponse savePlan(Level level, PlanRequest request) {
        SchoolLevel schoolLevel = levels.findByCode(level.code())
                .orElseThrow(() -> new ResourceNotFoundException("Level", level.code()));
        int duration = request.durationDays() == null ? SubscriptionPlan.ONE_YEAR : request.durationDays();

        SubscriptionPlan plan = plans.findByLevel(level)
                .orElseGet(() -> new SubscriptionPlan(level, request.price(), duration, request.active()));
        plan.setPrice(request.price());
        plan.setDurationDays(duration);
        plan.setActive(request.active());
        plans.save(plan);
        return toResponse(plan, Map.of(schoolLevel.getCode(), schoolLevel));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentSettingsDto paymentSettings() {
        return paymentSettings.findFirstByOrderByIdAsc()
                .map(s -> new PaymentSettingsDto(s.getD17Number(), s.getBankName(), s.getRib(),
                        s.getAccountHolder(), s.getInstructions()))
                .orElse(new PaymentSettingsDto(null, null, null, null, null));
    }

    @Override
    public PaymentSettingsDto savePaymentSettings(PaymentSettingsDto request) {
        PaymentSettings settings = paymentSettings.findFirstByOrderByIdAsc().orElseGet(PaymentSettings::new);
        settings.setD17Number(blankToNull(request.d17Number()));
        settings.setBankName(blankToNull(request.bankName()));
        settings.setRib(blankToNull(request.rib()));
        settings.setAccountHolder(blankToNull(request.accountHolder()));
        settings.setInstructions(blankToNull(request.instructions()));
        paymentSettings.save(settings);
        return paymentSettings();
    }

    @Override
    public UserResponse setAccessUntil(Long studentId, LocalDate accessUntil) {
        User student = users.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", studentId));
        if (student.getRole() != Role.STUDENT) {
            throw new BusinessException("Seuls les comptes élèves ont une date de fin d'accès");
        }
        student.setAccessUntil(accessUntil);
        return userMapper.toResponse(student);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubscriptionPlan> activePlan(Level level) {
        return plans.findByLevel(level).filter(SubscriptionPlan::isActive);
    }

    private Map<String, SchoolLevel> levelsByCode() {
        return levels.findAllByOrderByPositionAscCodeAsc().stream()
                .collect(Collectors.toMap(SchoolLevel::getCode, Function.identity()));
    }

    private static boolean isOpen(SchoolLevel level) {
        return level != null && level.isActive();
    }

    private static Comparator<SubscriptionPlan> byPosition(Map<String, SchoolLevel> byCode) {
        return Comparator.comparingInt(plan -> {
            SchoolLevel level = byCode.get(plan.getLevel().code());
            return level == null ? Integer.MAX_VALUE : level.getPosition();
        });
    }

    private static PlanResponse toResponse(SubscriptionPlan plan, Map<String, SchoolLevel> byCode) {
        SchoolLevel level = byCode.get(plan.getLevel().code());
        return new PlanResponse(plan.getId(), plan.getLevel(), level == null ? plan.getLevel().code() : level.getName(),
                plan.getPrice(), plan.getDurationDays(), plan.isActive());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
