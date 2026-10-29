package com.example.education_platform.coupon.service.impl;

import com.example.education_platform.auth.dto.response.LoginResponse;
import com.example.education_platform.auth.service.AuthService;
import com.example.education_platform.common.PageResponse;
import com.example.education_platform.common.exception.BusinessException;
import com.example.education_platform.common.exception.ConflictException;
import com.example.education_platform.common.exception.ResourceNotFoundException;
import com.example.education_platform.coupon.dto.request.CreateCouponBatchRequest;
import com.example.education_platform.coupon.dto.request.RedeemCouponRequest;
import com.example.education_platform.coupon.dto.response.CouponBatchResponse;
import com.example.education_platform.coupon.dto.response.CouponResponse;
import com.example.education_platform.coupon.entity.Coupon;
import com.example.education_platform.coupon.entity.CouponBatch;
import com.example.education_platform.coupon.repository.CouponBatchRepository;
import com.example.education_platform.coupon.repository.CouponRepository;
import com.example.education_platform.coupon.service.CouponService;
import com.example.education_platform.security.LoginRateLimiter;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.repository.UserRepository;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    /** No 0/O, 1/I/L: a child copies the code from paper, and these get misread. */
    static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    static final int CODE_LENGTH = 8;
    private static final int USERNAME_MAX = 40;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CouponBatchRepository batches;
    private final CouponRepository coupons;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final LoginRateLimiter rateLimiter;
    private final AuthService authService;

    @Override
    @Transactional
    public CouponBatchResponse createBatch(CreateCouponBatchRequest request) {
        String label = request.label() == null || request.label().isBlank() ? null : request.label().trim();
        CouponBatch batch = batches.save(new CouponBatch(request.level(), request.quantity(), label));

        Set<String> issued = new HashSet<>();
        List<Coupon> created = new ArrayList<>(request.quantity());
        for (int number = 1; number <= request.quantity(); number++) {
            String serial = batch.getId() + "-" + String.format(Locale.ROOT, "%04d", number);
            created.add(new Coupon(batch, serial, newCode(issued)));
        }
        coupons.saveAll(created);
        return toResponse(batch, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CouponBatchResponse> listBatches(Pageable pageable) {
        Page<CouponBatch> page = batches.findLatestFirst(pageable);
        List<CouponBatchResponse> content = page.getContent().stream()
                .map(batch -> toResponse(batch, coupons.countRedeemed(batch.getId())))
                .toList();
        return PageResponse.from(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> listCoupons(Long batchId) {
        if (!batches.existsById(batchId)) {
            throw new ResourceNotFoundException("CouponBatch", batchId);
        }
        return coupons.findByBatchIdOrderBySerialAsc(batchId).stream().map(CouponServiceImpl::toResponse).toList();
    }

    @Override
    @Transactional
    public LoginResponse redeem(RedeemCouponRequest request, String userAgent, String ipAddress) {
        String serial = request.serial().trim().toUpperCase(Locale.ROOT);
        String code = normaliseCode(request.code());
        // Throttled per serial: guessing the code of one book stops after a few tries
        String throttleKey = "coupon:" + serial;
        rateLimiter.checkAllowed(throttleKey);

        Coupon coupon = coupons.findForRedemption(serial)
                .filter(candidate -> candidate.getCode().equals(code))
                .orElse(null);
        if (coupon == null) {
            rateLimiter.recordFailure(throttleKey);
            throw new BusinessException("Ce code n'est pas valide. Vérifiez le numéro du livre et le code gratté.");
        }
        if (coupon.isRedeemed()) {
            throw new ConflictException(
                    "Ce livre a déjà été activé. Si c'est votre livre, contactez votre professeur.");
        }

        String fullName = clean(request.firstName()) + " " + clean(request.lastName());
        User student = new User(fullName, null, passwordEncoder.encode(request.password()),
                Role.STUDENT, coupon.getBatch().getLevel());
        student.setUsername(uniqueUsername(request.firstName(), request.lastName()));
        users.save(student);

        coupon.redeem(student, Instant.now());
        rateLimiter.recordSuccess(throttleKey);
        return authService.startSession(student, userAgent, ipAddress);
    }

    /** Upper case, without the dash or spaces a student may type: "k7p2 qx9m" matches "K7P2QX9M". */
    static String normaliseCode(String raw) {
        return raw.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    /** "Amina Ben Salem" gives amina.bensalem, then amina.bensalem2 if that one is taken. */
    String uniqueUsername(String firstName, String lastName) {
        String first = slug(firstName);
        String last = slug(lastName);
        String base = first.isEmpty() || last.isEmpty() ? first + last : first + "." + last;
        if (base.isEmpty()) {
            // Names typed in Arabic script leave nothing after transliteration
            base = "eleve";
        }
        if (base.length() > USERNAME_MAX) {
            base = base.substring(0, USERNAME_MAX);
        }
        String candidate = base;
        for (int suffix = 2; users.existsByUsername(candidate); suffix++) {
            candidate = base + suffix;
        }
        return candidate;
    }

    static String slug(String name) {
        String withoutAccents = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static String clean(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

    private String newCode(Set<String> issued) {
        while (true) {
            StringBuilder code = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                code.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            String value = code.toString();
            if (issued.add(value) && !coupons.existsByCode(value)) {
                return value;
            }
        }
    }

    private static CouponBatchResponse toResponse(CouponBatch batch, long redeemed) {
        return new CouponBatchResponse(batch.getId(), batch.getLevel(), batch.getQuantity(), batch.getLabel(),
                redeemed, batch.getCreatedAt());
    }

    private static CouponResponse toResponse(Coupon coupon) {
        User student = coupon.getRedeemedBy();
        return new CouponResponse(coupon.getId(), coupon.getSerial(), coupon.getCode(), coupon.isRedeemed(),
                coupon.getRedeemedAt(),
                student == null ? null : student.getFullName(),
                student == null ? null : student.getUsername());
    }
}
