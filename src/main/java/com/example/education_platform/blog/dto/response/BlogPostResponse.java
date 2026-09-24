package com.example.education_platform.blog.dto.response;

import com.example.education_platform.blog.entity.PostStatus;
import com.example.education_platform.storage.dto.response.StoredFileResponse;
import com.example.education_platform.user.dto.response.UserResponse;
import java.time.Instant;

/**
 * @param content null on the list, where only the excerpt is shown — an article's body is not
 *                something a list endpoint should be shipping for every row
 */
public record BlogPostResponse(
        Long id,
        String title,
        String slug,
        String excerpt,
        String content,
        BlogCategoryResponse category,
        UserResponse author,
        StoredFileResponse cover,
        PostStatus status,
        int readMinutes,
        long viewCount,
        Instant publishedAt) {

    /** The same article without its body, for list responses. */
    public BlogPostResponse withoutContent() {
        return new BlogPostResponse(id, title, slug, excerpt, null, category, author, cover,
                status, readMinutes, viewCount, publishedAt);
    }
}
