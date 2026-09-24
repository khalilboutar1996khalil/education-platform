package com.example.education_platform.assignment.dto.request;

import jakarta.validation.constraints.Size;

/** @param partnerId only meaningful for a PAIR assignment; ignored elsewhere and refused if set */
public record SubmissionDraftRequest(
        @Size(max = 2000)
        String comment,

        Long partnerId) {
}
