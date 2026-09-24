package com.example.education_platform.user.service;

import com.example.education_platform.common.PageResponse;
import com.example.education_platform.user.dto.request.ChangePasswordRequest;
import com.example.education_platform.user.dto.request.InviteStudentRequest;
import com.example.education_platform.user.dto.request.UpdateProfileRequest;
import com.example.education_platform.user.dto.response.InviteStudentResponse;
import com.example.education_platform.user.dto.response.UserResponse;
import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.UserStatus;
import org.springframework.data.domain.Pageable;

public interface UserService {

    InviteStudentResponse inviteStudent(InviteStudentRequest request);

    PageResponse<UserResponse> search(Role role, Level level, UserStatus status, String search, Pageable pageable);

    UserResponse getById(Long id);

    UserResponse updateStatus(Long id, UserStatus status);

    UserResponse getCurrentProfile();

    UserResponse updateCurrentProfile(UpdateProfileRequest request);

    void changePassword(ChangePasswordRequest request);
}
