package com.example.education_platform.resource.dto.response;

import com.example.education_platform.resource.entity.ResourceType;
import com.example.education_platform.storage.dto.response.StoredFileResponse;
import com.example.education_platform.user.entity.Level;

/**
 * @param courseCode null when the resource applies to every module
 * @param file       null for a LINK, where {@code externalUrl} carries the address instead
 */
public record ResourceResponse(
        Long id,
        String title,
        String description,
        ResourceType type,
        Long courseId,
        String courseCode,
        Level level,
        StoredFileResponse file,
        String externalUrl,
        long downloadCount) {
}
