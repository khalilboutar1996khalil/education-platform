package com.example.education_platform.announcement.controller;

import com.example.education_platform.announcement.dto.request.AnnouncementRequest;
import com.example.education_platform.announcement.dto.response.AnnouncementResponse;
import com.example.education_platform.announcement.service.AnnouncementService;
import com.example.education_platform.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
@Tag(name = "Annonces", description = "Notices to the section or to one module")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping
    @Operation(summary = "List notices; pinned first, then newest. Drafts are admin-only")
    PageResponse<AnnouncementResponse> list(
            @RequestParam(required = false) Long courseId,
            @ParameterObject
            @PageableDefault(size = 20, sort = {"pinned", "publishedAt"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return announcementService.list(courseId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "One notice")
    AnnouncementResponse getById(@PathVariable Long id) {
        return announcementService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Write a notice; it stays a draft until published")
    AnnouncementResponse create(@Valid @RequestBody AnnouncementRequest request) {
        return announcementService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Edit a draft; a published notice can no longer be changed")
    AnnouncementResponse update(@PathVariable Long id, @Valid @RequestBody AnnouncementRequest request) {
        return announcementService.update(id, request);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Publish it and freeze how many students it reached")
    AnnouncementResponse publish(@PathVariable Long id) {
        return announcementService.publish(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a notice")
    void delete(@PathVariable Long id) {
        announcementService.delete(id);
    }
}
