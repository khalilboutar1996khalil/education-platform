package com.example.education_platform.dashboard.service.impl;

import com.example.education_platform.dashboard.dto.response.AdminDashboardResponse;
import com.example.education_platform.dashboard.dto.response.StudentDashboardResponse;
import com.example.education_platform.dashboard.service.DashboardService;
import com.example.education_platform.security.CurrentUser;
import com.example.education_platform.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Resolves who is asking, then hands off to {@link DashboardQueries}, which is a separate bean so
 * its caching actually applies — a cached method called from within the same class is not proxied.
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DashboardQueries queries;
    private final CurrentUser currentUser;

    @Override
    public AdminDashboardResponse adminDashboard() {
        return queries.forAdmin(currentUser.get().getId());
    }

    @Override
    public StudentDashboardResponse myDashboard() {
        User me = currentUser.get();
        if (me.isAdmin()) {
            throw new AccessDeniedException("An admin has no student dashboard; use /dashboard/admin");
        }
        return queries.forStudent(me.getId(), me.getLevel());
    }
}
