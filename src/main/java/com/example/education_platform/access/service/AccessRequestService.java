package com.example.education_platform.access.service;

import com.example.education_platform.access.dto.request.SubmitAccessRequest;
import com.example.education_platform.access.dto.response.AccessRequestResponse;
import com.example.education_platform.access.dto.response.ApprovedAccessResponse;
import com.example.education_platform.access.entity.AccessRequestStatus;
import com.example.education_platform.common.PageResponse;
import org.springframework.data.domain.Pageable;

public interface AccessRequestService {

    /**
     * Public, so it answers the same whatever happens: a form that said "you already have an
     * account" would let anyone test which addresses are registered.
     */
    void submit(SubmitAccessRequest request);

    PageResponse<AccessRequestResponse> list(AccessRequestStatus status, Pageable pageable);

    /** Creates the student's account and returns its one-time password, as an invitation does. */
    ApprovedAccessResponse approve(Long id);

    AccessRequestResponse reject(Long id, String note);
}
