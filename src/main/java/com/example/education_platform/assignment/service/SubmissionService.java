package com.example.education_platform.assignment.service;

import com.example.education_platform.assignment.dto.request.GradeRequest;
import com.example.education_platform.assignment.dto.request.SubmissionDraftRequest;
import com.example.education_platform.assignment.dto.response.SubmissionResponse;
import com.example.education_platform.assignment.service.AssignmentService.DownloadableFile;
import com.example.education_platform.common.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface SubmissionService {

    // --- the student's side ---

    /** Creates the draft on first call and updates it afterwards, so the client needs one verb. */
    SubmissionResponse saveDraft(Long assignmentId, SubmissionDraftRequest request);

    SubmissionResponse addFile(Long assignmentId, MultipartFile file);

    SubmissionResponse removeFile(Long submissionId, Long fileId);

    SubmissionResponse submit(Long assignmentId);

    SubmissionResponse getMine(Long assignmentId);

    PageResponse<SubmissionResponse> listMine(Pageable pageable);

    // --- the teacher's side ---

    /** The grading queue for one assignment. */
    PageResponse<SubmissionResponse> listForAssignment(Long assignmentId, boolean onlyPending, Pageable pageable);

    SubmissionResponse grade(Long submissionId, GradeRequest request);

    /** Readable by the pair who handed it in, or by an admin. */
    DownloadableFile downloadFile(Long submissionId, Long fileId);
}
