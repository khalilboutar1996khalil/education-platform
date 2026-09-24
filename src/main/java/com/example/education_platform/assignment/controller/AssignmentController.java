package com.example.education_platform.assignment.controller;

import com.example.education_platform.assignment.dto.request.AssignmentRequest;
import com.example.education_platform.assignment.dto.request.AssignmentStatusRequest;
import com.example.education_platform.assignment.dto.response.AssignmentDetailResponse;
import com.example.education_platform.assignment.dto.response.AssignmentSummaryResponse;
import com.example.education_platform.assignment.service.AssignmentService;
import com.example.education_platform.assignment.service.AssignmentService.DownloadableFile;
import com.example.education_platform.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "TP & devoirs", description = "Assignments and their subject sheets")
public class AssignmentController {

    private final AssignmentService assignmentService;

    @GetMapping("/assignments")
    @Operation(summary = "List assignments; a student sees only published work at their own level")
    PageResponse<AssignmentSummaryResponse> list(
            @RequestParam(required = false) Long courseId,
            @ParameterObject @PageableDefault(size = 20, sort = "deadline", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return assignmentService.list(courseId, pageable);
    }

    @GetMapping("/assignments/{id}")
    @Operation(summary = "One assignment with its instructions")
    AssignmentDetailResponse getDetail(@PathVariable Long id) {
        return assignmentService.getDetail(id);
    }

    @GetMapping("/assignments/{id}/brief")
    @Operation(summary = "Download the subject sheet")
    ResponseEntity<Resource> downloadBrief(@PathVariable Long id) {
        return asDownload(assignmentService.downloadBrief(id));
    }

    @PostMapping("/courses/{courseId}/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create an assignment as a draft")
    AssignmentDetailResponse create(@PathVariable Long courseId, @Valid @RequestBody AssignmentRequest request) {
        return assignmentService.create(courseId, request);
    }

    @PutMapping("/assignments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an assignment")
    AssignmentDetailResponse update(@PathVariable Long id, @Valid @RequestBody AssignmentRequest request) {
        return assignmentService.update(id, request);
    }

    @PatchMapping("/assignments/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Publish or close an assignment")
    AssignmentDetailResponse changeStatus(@PathVariable Long id,
                                          @Valid @RequestBody AssignmentStatusRequest request) {
        return assignmentService.changeStatus(id, request.status());
    }

    @PostMapping(value = "/assignments/{id}/brief", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Attach or replace the subject sheet")
    AssignmentDetailResponse attachBrief(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return assignmentService.attachBrief(id, file);
    }

    @DeleteMapping("/assignments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an assignment with its submissions")
    void delete(@PathVariable Long id) {
        assignmentService.delete(id);
    }

    /** Attachment, never inline: an uploaded file must not be rendered in the browser's origin. */
    static ResponseEntity<Resource> asDownload(DownloadableFile file) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.filename()).build().toString())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.sizeBytes())
                .body(file.resource());
    }
}
