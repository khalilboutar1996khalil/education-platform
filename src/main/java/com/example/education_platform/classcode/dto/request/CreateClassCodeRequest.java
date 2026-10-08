package com.example.education_platform.classcode.dto.request;

import com.example.education_platform.level.validation.KnownLevel;
import com.example.education_platform.user.entity.Level;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateClassCodeRequest(
        @NotBlank(message = "{classCode.code.required}")
        @Size(max = 40)
        // Dictated in class and typed by hand, so no characters that are ambiguous out loud
        @Pattern(regexp = "[A-Za-z0-9-]+", message = "{classCode.code.invalid}")
        String code,

        @NotNull(message = "{classCode.level.required}")
        @KnownLevel
        Level level,

        @Size(max = 255)
        String label) {
}
