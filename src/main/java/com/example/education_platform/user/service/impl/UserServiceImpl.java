package com.example.education_platform.user.service.impl;

import com.example.education_platform.auth.repository.RefreshTokenRepository;
import com.example.education_platform.common.PageResponse;
import com.example.education_platform.common.exception.BusinessException;
import com.example.education_platform.common.exception.ConflictException;
import com.example.education_platform.common.exception.ResourceNotFoundException;
import com.example.education_platform.mail.service.EmailService;
import com.example.education_platform.security.AccessGuard;
import com.example.education_platform.security.CurrentUser;
import com.example.education_platform.user.dto.request.ChangePasswordRequest;
import com.example.education_platform.user.dto.request.InviteStudentRequest;
import com.example.education_platform.user.dto.request.UpdateProfileRequest;
import com.example.education_platform.user.dto.response.InviteStudentResponse;
import com.example.education_platform.user.dto.response.PasswordResetResponse;
import com.example.education_platform.user.dto.response.UserResponse;
import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.entity.UserStatus;
import com.example.education_platform.user.mapper.UserMapper;
import com.example.education_platform.user.repository.UserRepository;
import com.example.education_platform.user.service.UserService;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final CurrentUser currentUser;
    private final AccessGuard accessGuard;
    private final EmailService emailService;

    @Override
    @Transactional
    public InviteStudentResponse inviteStudent(InviteStudentRequest request) {
        String email = normalise(request.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("An account already exists for " + email);
        }

        String temporaryPassword = temporaryPassword();
        User student = new User(request.fullName().trim(), email,
                passwordEncoder.encode(temporaryPassword), Role.STUDENT, request.level());

        User saved = users.save(student);
        // Still returned as well: without SMTP configured the mail goes nowhere
        emailService.sendInvitation(saved.getEmail(), saved.getFullName(), temporaryPassword);
        return new InviteStudentResponse(userMapper.toResponse(saved), temporaryPassword);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(Role role, Level level, UserStatus status,
                                             String search, Pageable pageable) {
        Page<User> page = users.search(role, level, status, likePattern(search), pageable);
        List<UserResponse> content = page.getContent().stream().map(userMapper::toResponse).toList();
        return PageResponse.from(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        accessGuard.requireSelfOrAdmin(id);
        return users.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Override
    @Transactional
    public UserResponse updateStatus(Long id, UserStatus status) {
        User user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setStatus(status);
        if (status == UserStatus.DISABLED) {
            // Otherwise a disabled account keeps working until its refresh token expires
            refreshTokens.revokeAllForUser(user.getId(), Instant.now());
        }
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public PasswordResetResponse resetPassword(Long id) {
        User user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (user.isAdmin()) {
            // An admin changes their own password from the profile, proving they know the current one
            throw new BusinessException("Only a student's password can be reset this way");
        }
        String temporaryPassword = temporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        // Whoever held the old password may still be signed in somewhere
        refreshTokens.revokeAllForUser(user.getId(), Instant.now());
        return new PasswordResetResponse(userMapper.toResponse(user), temporaryPassword);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentProfile() {
        return userMapper.toResponse(currentUser.get());
    }

    @Override
    @Transactional
    public UserResponse updateCurrentProfile(UpdateProfileRequest request) {
        User me = currentUser.get();
        me.setFullName(request.fullName().trim());
        me.setNotifyByEmail(request.notifyByEmail());
        if (request.locale() != null) {
            me.setLocale(request.locale());
        }
        return userMapper.toResponse(me);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User me = currentUser.get();
        if (!passwordEncoder.matches(request.currentPassword(), me.getPasswordHash())) {
            throw new BadCredentialsException("Current password does not match");
        }
        me.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        // Every other session was opened with the old password, so none of them survive
        refreshTokens.revokeAllForUser(me.getId(), Instant.now());
    }

    private static String normalise(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String likePattern(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private static String temporaryPassword() {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
