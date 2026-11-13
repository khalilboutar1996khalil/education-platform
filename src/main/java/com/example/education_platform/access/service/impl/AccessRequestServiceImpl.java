package com.example.education_platform.access.service.impl;

import com.example.education_platform.access.dto.request.SubmitAccessRequest;
import com.example.education_platform.access.dto.response.AccessRequestResponse;
import com.example.education_platform.access.dto.response.ApprovedAccessResponse;
import com.example.education_platform.access.entity.AccessRequest;
import com.example.education_platform.access.entity.AccessRequestStatus;
import com.example.education_platform.access.mapper.AccessRequestMapper;
import com.example.education_platform.access.repository.AccessRequestRepository;
import com.example.education_platform.access.service.AccessRequestService;
import com.example.education_platform.common.PageResponse;
import com.example.education_platform.common.exception.BusinessException;
import com.example.education_platform.common.exception.ResourceNotFoundException;
import com.example.education_platform.security.CurrentUser;
import com.example.education_platform.mail.service.EmailService;
import com.example.education_platform.subscription.entity.SchoolYear;
import com.example.education_platform.subscription.entity.SubscriptionPlan;
import com.example.education_platform.subscription.service.SubscriptionService;
import com.example.education_platform.user.dto.request.InviteStudentRequest;
import com.example.education_platform.user.dto.response.InviteStudentResponse;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.repository.UserRepository;
import com.example.education_platform.user.service.UserService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AccessRequestServiceImpl implements AccessRequestService {

    private static final Logger LOG = LoggerFactory.getLogger(AccessRequestServiceImpl.class);

    private final AccessRequestRepository requests;
    private final UserRepository users;
    private final UserService userService;
    private final EmailService emailService;
    private final AccessRequestMapper requestMapper;
    private final CurrentUser currentUser;
    private final SubscriptionService subscriptions;

    @Override
    public void submit(SubmitAccessRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        // Checked before anything about the address, so this error says nothing about who exists
        Optional<SubscriptionPlan> plan = subscriptions.activePlan(request.level());
        if (plan.isPresent() && (request.paymentMethod() == null || isBlank(request.paymentReference()))) {
            throw new BusinessException(
                    "Indiquez comment vous avez payé (D17 ou virement) et la référence du paiement");
        }

        // What follows is dropped silently rather than reported. The caller is anonymous, and
        // an answer that varied would turn this form into a way of testing which addresses exist.
        Optional<User> existing = users.findByEmail(email);
        // A student with an account renews through the same form, but only by paying for a plan
        if (existing.isPresent() && (plan.isEmpty() || existing.get().getRole() != Role.STUDENT)) {
            LOG.info("Access request ignored: an account already exists for this address");
            return;
        }
        if (requests.existsByEmailIgnoreCaseAndStatus(email, AccessRequestStatus.PENDING)) {
            LOG.info("Access request ignored: one is already pending for this address");
            return;
        }

        AccessRequest saved = new AccessRequest(request.fullName().trim(), email,
                request.level(), request.message());
        plan.ifPresent(chosen -> saved.subscribe(chosen, request.paymentMethod(),
                request.paymentReference().trim()));
        existing.ifPresent(saved::setRenewalOf);
        requests.save(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AccessRequestResponse> list(AccessRequestStatus status, Pageable pageable) {
        Page<AccessRequest> page = requests.findByStatus(status, pageable);
        List<AccessRequestResponse> content = page.getContent().stream()
                .map(requestMapper::toResponse).toList();
        return PageResponse.from(page, content);
    }

    @Override
    public ApprovedAccessResponse approve(Long id) {
        AccessRequest request = pending(id);
        LocalDate today = SchoolYear.today();

        if (request.isRenewal()) {
            User student = request.getRenewalOf();
            // Paying before the end adds the year on top of what is left, never loses those days
            LocalDate from = student.accessExpired(today) || student.getAccessUntil() == null
                    ? today : student.getAccessUntil();
            student.setAccessUntil(from.plusDays(request.getDurationDays()));
            // Renewing for the next class year moves the student up with it
            student.setLevel(request.getLevel());
            request.approve(currentUser.get(), student, Instant.now());
            requests.flush();
            return new ApprovedAccessResponse(requestMapper.toResponse(request), null);
        }

        // Reuses the invite path rather than creating the account here: one place decides what a
        // new student account looks like, and one place sends the mail
        InviteStudentResponse invited = userService.inviteStudent(new InviteStudentRequest(
                request.getFullName(), request.getEmail(), request.getLevel()));

        User created = users.findById(invited.user().id()).orElseThrow(
                () -> new ResourceNotFoundException("User", invited.user().id()));
        // A paid plan runs from today; a free request (level without a plan) to the end of the year
        created.setAccessUntil(request.getDurationDays() == null
                ? SchoolYear.endOf(today) : today.plusDays(request.getDurationDays()));
        request.approve(currentUser.get(), created, Instant.now());
        requests.flush();

        return new ApprovedAccessResponse(requestMapper.toResponse(request), invited.temporaryPassword());
    }

    @Override
    public AccessRequestResponse reject(Long id, String note) {
        AccessRequest request = pending(id);
        request.reject(currentUser.get(), note, Instant.now());
        // Without this the person waits on an answer that already came
        emailService.sendAccessRejected(request.getEmail(), request.getFullName(), note);
        return requestMapper.toResponse(request);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private AccessRequest pending(Long id) {
        AccessRequest request = requests.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Access request", id));
        if (!request.isPending()) {
            throw new BusinessException("This request has already been decided");
        }
        return request;
    }
}
