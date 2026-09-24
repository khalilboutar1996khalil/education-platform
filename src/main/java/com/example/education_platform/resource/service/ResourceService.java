package com.example.education_platform.resource.service;

import com.example.education_platform.common.PageResponse;
import com.example.education_platform.resource.dto.request.ResourceRequest;
import com.example.education_platform.resource.dto.response.ResourceResponse;
import com.example.education_platform.resource.entity.ResourceType;
import com.example.education_platform.storage.service.DownloadableFile;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface ResourceService {

    /** A student only ever sees what neither scope excludes them from. */
    PageResponse<ResourceResponse> list(Long courseId, ResourceType type, String search, Pageable pageable);

    ResourceResponse getById(Long id);

    /** {@code file} is required unless the resource is a LINK, where it must be absent. */
    ResourceResponse create(ResourceRequest request, MultipartFile file);

    ResourceResponse update(Long id, ResourceRequest request);

    void delete(Long id);

    /** Serves the bytes and counts the download. A LINK has no bytes to serve. */
    DownloadableFile download(Long id);
}
