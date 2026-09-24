package com.example.education_platform.blog.entity;

import com.example.education_platform.common.BaseEntity;
import com.example.education_platform.storage.entity.StoredFile;
import com.example.education_platform.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "blog_posts")
public class BlogPost extends BaseEntity {

    /** Average adult reading speed, near enough for a "5 min read" badge. */
    private static final int WORDS_PER_MINUTE = 200;

    @Column(nullable = false)
    private String title;

    /** The public URL; stable once published, which is why editing the title does not change it. */
    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    @Column(length = 500)
    private String excerpt;

    /**
     * Mapped to {@code text} explicitly. {@code @Lob} on a String makes Hibernate expect
     * PostgreSQL's {@code oid} large-object type instead, which fails schema validation.
     */
    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private BlogCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_file_id")
    private StoredFile cover;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status = PostStatus.DRAFT;

    @Column(nullable = false)
    private int readMinutes = 1;

    @Column(nullable = false)
    private long viewCount;

    private Instant publishedAt;

    public BlogPost(String title, String slug, String content, BlogCategory category, User author) {
        this.title = title;
        this.slug = slug;
        this.content = content;
        this.category = category;
        this.author = author;
        this.readMinutes = estimateReadMinutes(content);
    }

    public boolean isPublished() {
        return status == PostStatus.PUBLISHED;
    }

    public void publish(Instant now) {
        this.status = PostStatus.PUBLISHED;
        this.publishedAt = now;
    }

    /** Back to a draft: it disappears from the public list but keeps its slug and its views. */
    public void unpublish() {
        this.status = PostStatus.DRAFT;
    }

    public void setContent(String content) {
        this.content = content;
        this.readMinutes = estimateReadMinutes(content);
    }

    /** Always at least a minute: "0 min read" reads like a bug, not a short article. */
    private static int estimateReadMinutes(String content) {
        if (content == null || content.isBlank()) {
            return 1;
        }
        int words = content.trim().split("\\s+").length;
        return Math.max(1, (int) Math.ceil((double) words / WORDS_PER_MINUTE));
    }

    /** Lower-case, accents folded, everything else collapsed to single hyphens. */
    public static String slugify(String title) {
        String normalised = java.text.Normalizer.normalize(title, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return normalised.isBlank() ? "article" : normalised;
    }
}
