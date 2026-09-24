package com.example.education_platform.security;

import com.example.education_platform.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Ownership checks. Role checks belong on the endpoint as {@code @PreAuthorize}; this covers the
 * case roles cannot express — "an admin, or the student this row belongs to".
 */
@Component
@RequiredArgsConstructor
public class AccessGuard {

    private final CurrentUser currentUser;

    public User requireSelfOrAdmin(Long userId) {
        User me = currentUser.get();
        if (!me.isAdmin() && !me.getId().equals(userId)) {
            throw new AccessDeniedException("Not allowed to access another user's data");
        }
        return me;
    }
}
