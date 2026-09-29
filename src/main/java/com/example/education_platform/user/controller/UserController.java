package com.example.education_platform.user.controller;

import com.example.education_platform.common.PageResponse;
import com.example.education_platform.user.dto.request.InviteStudentRequest;
import com.example.education_platform.user.dto.request.UpdateUserStatusRequest;
import com.example.education_platform.user.dto.response.InviteStudentResponse;
import com.example.education_platform.user.dto.response.PasswordResetResponse;
import com.example.education_platform.user.dto.response.UserResponse;
import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.UserStatus;
import com.example.education_platform.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Administration of student accounts")
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a student account and return its one-time temporary password")
    InviteStudentResponse inviteStudent(@Valid @RequestBody InviteStudentRequest request) {
        return userService.inviteStudent(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List users, filtered and paginated; every filter is optional")
    PageResponse<UserResponse> list(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Level level,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(size = 20, sort = "fullName", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return userService.search(role, level, status, search, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Read one user — an admin, or the student themselves")
    UserResponse getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change a user's status; disabling also revokes their sessions")
    UserResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateUserStatusRequest request) {
        return userService.updateStatus(id, request.status());
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Give a student a new one-time temporary password; this signs them out everywhere")
    PasswordResetResponse resetPassword(@PathVariable Long id) {
        return userService.resetPassword(id);
    }
}
