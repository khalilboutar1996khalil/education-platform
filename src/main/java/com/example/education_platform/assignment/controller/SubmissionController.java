package com.example.education_platform.assignment.controller;

import com.example.education_platform.assignment.dto.request.GradeRequest;
import com.example.education_platform.assignment.dto.request.SubmissionDraftRequest;
import com.example.education_platform.assignment.dto.response.SubmissionResponse;
import com.example.education_platform.assignment.service.SubmissionService;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Submissions", description = "Handing work in, and marking it")
public class SubmissionController {

    private final SubmissionService submissionService;

    // --- the student's side ---

    @PutMapping("/assignments/{assignmentId}/submission")
    @Operation(summary = "Create or update my draft for this assignment")
    SubmissionResponse saveDraft(@PathVariable Long assignmentId,
                                 @Valid @RequestBody SubmissionDraftRequest request) {
        return submissionService.saveDraft(assignmentId, request);
    }

    @GetMapping("/assignments/{assignmentId}/submission")
    @Operation(summary = "My submission for this assignment")
    SubmissionResponse getMine(@PathVariable Long assignmentId) {
        return submissionService.getMine(assignmentId);
    }

    @PostMapping(value = "/assignments/{assignmentId}/submission/files",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Attach a file to my draft")
    SubmissionResponse addFile(@PathVariable Long assignmentId, @RequestPart("file") MultipartFile file) {
        return submissionService.addFile(assignmentId, file);
    }

    @DeleteMapping("/submissions/{submissionId}/files/{fileId}")
    @Operation(summary = "Remove a file from my draft")
    SubmissionResponse removeFile(@PathVariable Long submissionId, @PathVariable Long fileId) {
        return submissionService.removeFile(submissionId, fileId);
    }

    @PostMapping("/assignments/{assignmentId}/submission/submit")
    @Operation(summary = "Hand in my work; after this it can no longer be changed")
    SubmissionResponse submit(@PathVariable Long assignmentId) {
        return submissionService.submit(assignmentId);
    }

    @GetMapping("/my/submissions")
    @Operation(summary = "Everything I have handed in, from either side of a pair")
    PageResponse<SubmissionResponse> listMine(
            @ParameterObject @PageableDefault(size = 20, sort = "submittedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return submissionService.listMine(pageable);
    }

    // --- shared: the pair who handed it in, or an admin ---

    @GetMapping("/submissions/{submissionId}/files/{fileId}")
    @Operation(summary = "Download a submitted file")
    ResponseEntity<Resource> downloadFile(@PathVariable Long submissionId, @PathVariable Long fileId) {
        return AssignmentController.asDownload(submissionService.downloadFile(submissionId, fileId));
    }

    // --- the teacher's side ---

    @GetMapping("/assignments/{assignmentId}/submissions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "The submissions for one assignment; pending ones first with onlyPending")
    PageResponse<SubmissionResponse> listForAssignment(
            @PathVariable Long assignmentId,
            @RequestParam(defaultValue = "false") boolean onlyPending,
            @ParameterObject @PageableDefault(size = 20, sort = "submittedAt", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return submissionService.listForAssignment(assignmentId, onlyPending, pageable);
    }

    @PostMapping("/submissions/{submissionId}/grade")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mark a submission and leave feedback")
    SubmissionResponse grade(@PathVariable Long submissionId, @Valid @RequestBody GradeRequest request) {
        return submissionService.grade(submissionId, request);
    }
}
