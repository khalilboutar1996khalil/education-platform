package com.example.education_platform.access.controller;

import com.example.education_platform.access.dto.request.RejectAccessRequest;
import com.example.education_platform.access.dto.request.SubmitAccessRequest;
import com.example.education_platform.access.dto.response.AccessRequestResponse;
import com.example.education_platform.access.dto.response.ApprovedAccessResponse;
import com.example.education_platform.access.entity.AccessRequestStatus;
import com.example.education_platform.access.service.AccessRequestService;
import com.example.education_platform.common.PageResponse;
import com.example.education_platform.storage.service.DownloadableFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/access-requests")
@RequiredArgsConstructor
@Tag(name = "Demandes d'accès", description = "Asking to join, and deciding who does")
public class AccessRequestController {

    private final AccessRequestService accessRequestService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @SecurityRequirements // public: the person asking has no account yet, by definition
    @Operation(summary = "Ask for access. Always answers 202, whatever the address turns out to be")
    void submit(@Valid @RequestBody SubmitAccessRequest request) {
        accessRequestService.submit(request);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @SecurityRequirements // public, like the JSON form
    @Operation(summary = "Ask for access and attach the receipt of the payment (JPG, PNG or PDF, 5 MB)")
    void submitWithReceipt(@Valid @RequestPart("request") SubmitAccessRequest request,
                           @RequestPart(value = "receipt", required = false) MultipartFile receipt) {
        accessRequestService.submit(request, receipt);
    }

    @GetMapping("/{id}/receipt")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "The receipt the student attached, to check the payment before accepting")
    ResponseEntity<Resource> receipt(@PathVariable Long id) {
        DownloadableFile file = accessRequestService.receipt(id);
        return ResponseEntity.ok()
                // Attachment, never inline: an uploaded file must not run in the browser's origin
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.filename()).build().toString())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.sizeBytes())
                .body(file.resource());
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
