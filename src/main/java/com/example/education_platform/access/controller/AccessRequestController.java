package com.example.education_platform.access.controller;

import com.example.education_platform.access.dto.request.RejectAccessRequest;
import com.example.education_platform.access.dto.request.SubmitAccessRequest;
import com.example.education_platform.access.dto.response.AccessRequestResponse;
import com.example.education_platform.access.dto.response.ApprovedAccessResponse;
import com.example.education_platform.access.entity.AccessRequestStatus;
import com.example.education_platform.access.service.AccessRequestService;
import com.example.education_platform.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/access-requests")
@RequiredArgsConstructor
@Tag(name = "Demandes d'accès", description = "Asking to join, and deciding who does")
public class AccessRequestController {

    private final AccessRequestService accessRequestService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @SecurityRequirements // public: the person asking has no account yet, by definition
    @Operation(summary = "Ask for access. Always answers 202, whatever the address turns out to be")
    void submit(@Valid @RequestBody SubmitAccessRequest request) {
        accessRequestService.submit(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "The queue; filter by status to see only what is still waiting")
    PageResponse<AccessRequestResponse> list(
            @RequestParam(required = false) AccessRequestStatus status,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return accessRequestService.list(status, pageable);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve and create the student's account, returning its one-time password")
    ApprovedAccessResponse approve(@PathVariable Long id) {
        return accessRequestService.approve(id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Refuse the request, with a note for your own record")
    AccessRequestResponse reject(@PathVariable Long id, @Valid @RequestBody RejectAccessRequest request) {
        return accessRequestService.reject(id, request.note());
    }
}
