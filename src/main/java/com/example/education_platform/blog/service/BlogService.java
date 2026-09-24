package com.example.education_platform.blog.service;

import com.example.education_platform.blog.dto.request.BlogCategoryRequest;
import com.example.education_platform.blog.dto.request.BlogPostRequest;
import com.example.education_platform.blog.dto.response.BlogCategoryResponse;
import com.example.education_platform.blog.dto.response.BlogPostResponse;
import com.example.education_platform.common.PageResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface BlogService {

    // --- articles ---

    /** Readers see published articles; an admin sees drafts too. Bodies are omitted from the list. */
    PageResponse<BlogPostResponse> list(Long categoryId, String search, Pageable pageable);

    /** Reading by slug counts a view; reading by id, for the editor, does not. */
    BlogPostResponse readBySlug(String slug);

    BlogPostResponse getById(Long id);

    BlogPostResponse create(BlogPostRequest request);

    BlogPostResponse update(Long id, BlogPostRequest request);

    BlogPostResponse setCover(Long id, MultipartFile file);

    BlogPostResponse publish(Long id);

    BlogPostResponse unpublish(Long id);

    void delete(Long id);

    // --- categories ---

    List<BlogCategoryResponse> listCategories();

    BlogCategoryResponse createCategory(BlogCategoryRequest request);

    void deleteCategory(Long id);
}
