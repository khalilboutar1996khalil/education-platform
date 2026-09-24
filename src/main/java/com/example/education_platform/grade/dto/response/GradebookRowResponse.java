package com.example.education_platform.grade.dto.response;

import com.example.education_platform.user.dto.response.UserResponse;
import java.math.BigDecimal;
import java.util.List;

/** One student's line in a module's gradebook: every mark, plus what they average to. */
public record GradebookRowResponse(
        UserResponse student,
        List<GradeResponse> grades,
        BigDecimal average) {
}
