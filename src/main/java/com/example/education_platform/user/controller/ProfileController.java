package com.example.education_platform.user.controller;

import com.example.education_platform.user.dto.request.ChangePasswordRequest;
import com.example.education_platform.user.dto.request.UpdateProfileRequest;
import com.example.education_platform.user.dto.response.UserResponse;
import com.example.education_platform.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in user's own account. No id in the path — it always comes from the token. */
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "The signed-in user's profile and settings")
public class ProfileController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Read my profile")
    UserResponse me() {
        return userService.getCurrentProfile();
    }

    @PatchMapping
    @Operation(summary = "Update my name and preferences")
    UserResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateCurrentProfile(request);
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Change my password; this signs out every other session")
    void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
    }
}
