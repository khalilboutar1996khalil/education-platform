package com.example.education_platform.resource.controller;

import com.example.education_platform.common.PageResponse;
import com.example.education_platform.resource.dto.request.ResourceRequest;
import com.example.education_platform.resource.dto.response.ResourceResponse;
import com.example.education_platform.resource.entity.ResourceType;
import com.example.education_platform.resource.service.ResourceService;
import com.example.education_platform.storage.service.DownloadableFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
@Tag(name = "Ressources", description = "The shared library of documents and links")
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    @Operation(summary = "List resources; a student sees only what is shared with their level")
    PageResponse<ResourceResponse> list(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) ResourceType type,
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(size = 20, sort = "title", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return resourceService.list(courseId, type, search, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "One resource")
    ResourceResponse getById(@PathVariable Long id) {
        return resourceService.getById(id);
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download the file and count the download")
    ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable Long id) {
        DownloadableFile file = resourceService.download(id);
        return ResponseEntity.ok()
                // Attachment, never inline: a library file must not run in the browser's origin
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.filename()).build().toString())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.sizeBytes())
                .body(file.resource());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add a resource; send a file for everything but a LINK")
    ResourceResponse create(@Valid @RequestPart("resource") ResourceRequest request,
                            @RequestPart(value = "file", required = false) MultipartFile file) {
        return resourceService.create(request, file);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a resource's details; the file itself cannot be swapped")
    ResourceResponse update(@PathVariable Long id, @Valid @RequestBody ResourceRequest request) {
        return resourceService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a resource and its file")
    void delete(@PathVariable Long id) {
        resourceService.delete(id);
    }
}
