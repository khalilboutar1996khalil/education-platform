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
import com.example.education_platform.user.dto.request.InviteStudentRequest;
import com.example.education_platform.user.dto.response.InviteStudentResponse;
import com.example.education_platform.user.repository.UserRepository;
import com.example.education_platform.user.service.UserService;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
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

    @Override
    public void submit(SubmitAccessRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        // Both of these are dropped silently rather than reported. The caller is anonymous, and
        // an answer that varied would turn this form into a way of testing which addresses exist.
        if (users.existsByEmail(email)) {
            LOG.info("Access request ignored: an account already exists for this address");
            return;
        }
        if (requests.existsByEmailIgnoreCaseAndStatus(email, AccessRequestStatus.PENDING)) {
            LOG.info("Access request ignored: one is already pending for this address");
            return;
        }

        requests.save(new AccessRequest(request.fullName().trim(), email,
                request.level(), request.message()));
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

        // Reuses the invite path rather than creating the account here: one place decides what a
        // new student account looks like, and one place sends the mail
        InviteStudentResponse invited = userService.inviteStudent(new InviteStudentRequest(
                request.getFullName(), request.getEmail(), request.getLevel()));

        request.approve(currentUser.get(),
                users.findById(invited.user().id()).orElseThrow(
                        () -> new ResourceNotFoundException("User", invited.user().id())),
                Instant.now());
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

    private AccessRequest pending(Long id) {
        AccessRequest request = requests.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Access request", id));
        if (!request.isPending()) {
            throw new BusinessException("This request has already been decided");
        }
        return request;
    }
}
