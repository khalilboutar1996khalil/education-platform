package com.example.education_platform.access.service;

import com.example.education_platform.access.dto.request.SubmitAccessRequest;
import com.example.education_platform.access.dto.response.AccessRequestResponse;
import com.example.education_platform.access.dto.response.ApprovedAccessResponse;
import com.example.education_platform.access.entity.AccessRequestStatus;
import com.example.education_platform.common.PageResponse;
import com.example.education_platform.storage.service.DownloadableFile;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface AccessRequestService {

    /**
     * Public, so it answers the same whatever happens: a form that said "you already have an
     * account" would let anyone test which addresses are registered.
     */
    void submit(SubmitAccessRequest request);

    /** The same, with the receipt of the payment (a photo or a PDF); {@code receipt} may be null. */
    void submit(SubmitAccessRequest request, MultipartFile receipt);

    /** Admin: the receipt attached to a request. */
    DownloadableFile receipt(Long id);

    PageResponse<AccessRequestResponse> list(AccessRequestStatus status, Pageable pageable);

    /** Creates the student's account and returns its one-time password, as an invitation does. */
    ApprovedAccessResponse approve(Long id);

    AccessRequestResponse reject(Long id, String note);
}
