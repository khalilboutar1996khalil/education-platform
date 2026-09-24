package com.example.education_platform.blog.repository;

import com.example.education_platform.blog.entity.BlogPost;
import com.example.education_platform.blog.entity.PostStatus;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    @EntityGraph(attributePaths = {"category", "author", "cover"})
    Optional<BlogPost> findDetailById(Long id);

    @EntityGraph(attributePaths = {"category", "author", "cover"})
    Optional<BlogPost> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    /** Readers pass PUBLISHED only; an admin passes every status and sees drafts too. */
    @EntityGraph(attributePaths = {"category", "author", "cover"})
    @Query("""
            select p from BlogPost p
            where p.status in :statuses
              and (:categoryId is null or p.category.id = :categoryId)
              and (:search is null or lower(p.title) like :search)
            """)
    Page<BlogPost> findVisible(@Param("statuses") Collection<PostStatus> statuses,
                               @Param("categoryId") Long categoryId,
                               @Param("search") String search,
                               Pageable pageable);

    boolean existsByCategoryId(Long categoryId);

    /**
     * Counted in the database so two readers cannot lose one another's view. The flags matter
     * because a bulk update otherwise leaves the loaded entity reporting the old total.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update BlogPost p set p.viewCount = p.viewCount + 1 where p.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
