package com.example.education_platform.assignment.service;

import com.example.education_platform.assignment.dto.request.AssignmentRequest;
import com.example.education_platform.assignment.dto.response.AssignmentDetailResponse;
import com.example.education_platform.assignment.dto.response.AssignmentSummaryResponse;
import com.example.education_platform.assignment.entity.AssignmentStatus;
import com.example.education_platform.common.PageResponse;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface AssignmentService {

    /** A student sees only open work at their own level, with their own submission state on it. */
    PageResponse<AssignmentSummaryResponse> list(Long courseId, Pageable pageable);

    AssignmentDetailResponse getDetail(Long id);

    AssignmentDetailResponse create(Long courseId, AssignmentRequest request);

    AssignmentDetailResponse update(Long id, AssignmentRequest request);

    AssignmentDetailResponse changeStatus(Long id, AssignmentStatus status);

    AssignmentDetailResponse attachBrief(Long id, MultipartFile file);

    void delete(Long id);

    /** The subject sheet, readable by an admin or by a student of the right level. */
    DownloadableFile downloadBrief(Long id);

    record DownloadableFile(Resource resource, String filename, String contentType, long sizeBytes) {
    }
}
