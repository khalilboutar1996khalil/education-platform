package com.example.education_platform.announcement.service;

import com.example.education_platform.announcement.dto.request.AnnouncementRequest;
import com.example.education_platform.announcement.dto.response.AnnouncementResponse;
import com.example.education_platform.common.PageResponse;
import org.springframework.data.domain.Pageable;

public interface AnnouncementService {

    /** A student sees published notices addressed to them; an admin sees drafts too. */
    PageResponse<AnnouncementResponse> list(Long courseId, Pageable pageable);

    AnnouncementResponse getById(Long id);

    AnnouncementResponse create(AnnouncementRequest request);

    AnnouncementResponse update(Long id, AnnouncementRequest request);

    /** Stamps the date and freezes the audience size. Publishing twice changes nothing. */
    AnnouncementResponse publish(Long id);

    void delete(Long id);
}
