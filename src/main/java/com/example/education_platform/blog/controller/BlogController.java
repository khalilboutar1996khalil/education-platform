package com.example.education_platform.blog.controller;

import com.example.education_platform.blog.dto.request.BlogCategoryRequest;
import com.example.education_platform.blog.dto.request.BlogPostRequest;
import com.example.education_platform.blog.dto.response.BlogCategoryResponse;
import com.example.education_platform.blog.dto.response.BlogPostResponse;
import com.example.education_platform.blog.service.BlogService;
import com.example.education_platform.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@RequestMapping("/api/v1/blog")
@RequiredArgsConstructor
@Tag(name = "Blog", description = "Articles and their categories")
public class BlogController {

    private final BlogService blogService;

    // --- reading ---

    @GetMapping("/posts")
    @Operation(summary = "List articles without their bodies; drafts are admin-only")
    PageResponse<BlogPostResponse> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(size = 10, sort = "publishedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return blogService.list(categoryId, search, pageable);
    }

    @GetMapping("/posts/by-slug/{slug}")
    @Operation(summary = "Read an article by its public URL; this counts a view")
    BlogPostResponse readBySlug(@PathVariable String slug) {
        return blogService.readBySlug(slug);
    }

    @GetMapping("/posts/{id}")
    @Operation(summary = "Read an article by id, for the editor; this does not count a view")
    BlogPostResponse getById(@PathVariable Long id) {
        return blogService.getById(id);
    }

    @GetMapping("/categories")
    @Operation(summary = "List categories")
    List<BlogCategoryResponse> listCategories() {
        return blogService.listCategories();
    }

    // --- writing ---

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Write an article; the slug comes from the title and never moves")
    BlogPostResponse create(@Valid @RequestBody BlogPostRequest request) {
        return blogService.create(request);
    }

    @PutMapping("/posts/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an article; the reading time is recomputed")
    BlogPostResponse update(@PathVariable Long id, @Valid @RequestBody BlogPostRequest request) {
        return blogService.update(id, request);
    }

    @PostMapping(value = "/posts/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Attach or replace the cover image")
    BlogPostResponse setCover(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return blogService.setCover(id, file);
    }

    @PostMapping("/posts/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Publish an article")
    BlogPostResponse publish(@PathVariable Long id) {
        return blogService.publish(id);
    }

    @PostMapping("/posts/{id}/unpublish")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Send an article back to draft; it keeps its slug and its views")
    BlogPostResponse unpublish(@PathVariable Long id) {
        return blogService.unpublish(id);
    }

    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an article and its cover")
    void delete(@PathVariable Long id) {
        blogService.delete(id);
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a category")
    BlogCategoryResponse createCategory(@Valid @RequestBody BlogCategoryRequest request) {
        return blogService.createCategory(request);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a category that holds no articles")
    void deleteCategory(@PathVariable Long id) {
        blogService.deleteCategory(id);
    }
}
