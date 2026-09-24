package com.example.education_platform.blog.service.impl;

import com.example.education_platform.blog.dto.request.BlogCategoryRequest;
import com.example.education_platform.blog.dto.request.BlogPostRequest;
import com.example.education_platform.blog.dto.response.BlogCategoryResponse;
import com.example.education_platform.blog.dto.response.BlogPostResponse;
import com.example.education_platform.blog.entity.BlogCategory;
import com.example.education_platform.blog.entity.BlogPost;
import com.example.education_platform.blog.entity.PostStatus;
import com.example.education_platform.blog.mapper.BlogMapper;
import com.example.education_platform.blog.repository.BlogCategoryRepository;
import com.example.education_platform.blog.repository.BlogPostRepository;
import com.example.education_platform.blog.service.BlogService;
import com.example.education_platform.common.PageResponse;
import com.example.education_platform.common.exception.BusinessException;
import com.example.education_platform.common.exception.ConflictException;
import com.example.education_platform.common.exception.ResourceNotFoundException;
import com.example.education_platform.security.CurrentUser;
import com.example.education_platform.storage.entity.StoredFile;
import com.example.education_platform.storage.service.StorageService;
import com.example.education_platform.user.entity.User;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class BlogServiceImpl implements BlogService {

    private final BlogPostRepository posts;
    private final BlogCategoryRepository categories;
    private final StorageService storage;
    private final BlogMapper blogMapper;
    private final CurrentUser currentUser;

    // ---------- articles ----------

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BlogPostResponse> list(Long categoryId, String search, Pageable pageable) {
        var statuses = currentUser.get().isAdmin()
                ? EnumSet.allOf(PostStatus.class)
                : EnumSet.of(PostStatus.PUBLISHED);
        Page<BlogPost> page = posts.findVisible(statuses, categoryId, likePattern(search), pageable);

        List<BlogPostResponse> content = page.getContent().stream()
                .map(blogMapper::toResponse)
                .map(BlogPostResponse::withoutContent)
                .toList();
        return PageResponse.from(page, content);
    }

    @Override
    public BlogPostResponse readBySlug(String slug) {
        BlogPost post = posts.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article", slug));
        requireReadable(post);

        BlogPostResponse response = blogMapper.toResponse(post);
        // Mapped first: the counter's bulk update detaches everything behind it
        posts.incrementViewCount(post.getId());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public BlogPostResponse getById(Long id) {
        BlogPost post = post(id);
        requireReadable(post);
        return blogMapper.toResponse(post);
    }

    @Override
    public BlogPostResponse create(BlogPostRequest request) {
        BlogCategory category = category(request.categoryId());
        User author = currentUser.get();
        String slug = uniqueSlug(BlogPost.slugify(request.title()));

        BlogPost post = new BlogPost(request.title(), slug, request.content(), category, author);
        post.setExcerpt(request.excerpt());
        BlogPost saved = posts.save(post);
        posts.flush();
        return blogMapper.toResponse(saved);
    }

    @Override
    public BlogPostResponse update(Long id, BlogPostRequest request) {
        BlogPost post = post(id);
        post.setTitle(request.title());
        post.setExcerpt(request.excerpt());
        // Recomputes the reading time; the slug deliberately stays put so links keep working
        post.setContent(request.content());
        post.setCategory(category(request.categoryId()));
        return blogMapper.toResponse(post);
    }

    @Override
    public BlogPostResponse setCover(Long id, MultipartFile file) {
        BlogPost post = post(id);
        StoredFile previous = post.getCover();
        post.setCover(storage.store(file));
        if (previous != null) {
            storage.delete(previous);
        }
        posts.flush();
        return blogMapper.toResponse(post);
    }

    @Override
    public BlogPostResponse publish(Long id) {
        BlogPost post = post(id);
        if (post.isPublished()) {
            throw new BusinessException("This article is already published");
        }
        post.publish(Instant.now());
        return blogMapper.toResponse(post);
    }

    @Override
    public BlogPostResponse unpublish(Long id) {
        BlogPost post = post(id);
        post.unpublish();
        return blogMapper.toResponse(post);
    }

    @Override
    public void delete(Long id) {
        BlogPost post = post(id);
        StoredFile cover = post.getCover();
        posts.delete(post);
        posts.flush();
        if (cover != null) {
            storage.delete(cover);
        }
    }

    // ---------- categories ----------

    @Override
    @Transactional(readOnly = true)
    public List<BlogCategoryResponse> listCategories() {
        return blogMapper.toCategories(categories.findAllByOrderByNameAsc());
    }

    @Override
    public BlogCategoryResponse createCategory(BlogCategoryRequest request) {
        String slug = BlogPost.slugify(request.name());
        if (categories.existsBySlug(slug)) {
            throw new ConflictException("A category already uses the name " + request.name());
        }
        return blogMapper.toCategory(categories.save(new BlogCategory(request.name(), slug, request.color())));
    }

    @Override
    public void deleteCategory(Long id) {
        BlogCategory category = categories.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        // Articles require a category, so removing one in use would leave them with nothing
        if (posts.existsByCategoryId(id)) {
            throw new BusinessException("This category still holds articles");
        }
        categories.delete(category);
    }

    // ---------- helpers ----------

    private void requireReadable(BlogPost post) {
        if (!post.isPublished() && !currentUser.get().isAdmin()) {
            throw new AccessDeniedException("This article is not published");
        }
    }

    /** Two articles can share a title; they cannot share a URL. */
    private String uniqueSlug(String base) {
        if (!posts.existsBySlug(base)) {
            return base;
        }
        int suffix = 2;
        while (posts.existsBySlug(base + "-" + suffix)) {
            suffix++;
        }
        return base + "-" + suffix;
    }

    private BlogPost post(Long id) {
        return posts.findDetailById(id).orElseThrow(() -> new ResourceNotFoundException("Article", id));
    }

    private BlogCategory category(Long id) {
        return categories.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    private static String likePattern(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
