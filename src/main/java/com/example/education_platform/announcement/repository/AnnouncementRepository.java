package com.example.education_platform.announcement.repository;

import com.example.education_platform.announcement.entity.Announcement;
import com.example.education_platform.user.entity.Level;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    @EntityGraph(attributePaths = {"author", "course"})
    Optional<Announcement> findDetailById(Long id);

    /**
     * A null level is an admin: every notice, drafts included. A student sees only published ones
     * that neither scope excludes them from.
     *
     * <p>The join is explicit and LEFT: writing {@code a.course.level} would add an inner join and
     * quietly drop every section-wide notice.
     */
    @EntityGraph(attributePaths = {"author", "course"})
    @Query("""
            select a from Announcement a
            left join a.course c
            where (:level is null
                   or (a.publishedAt is not null
                       and (c is null or c.level = :level)
                       and (a.level is null or a.level = :level)))
              and (:courseId is null or c.id = :courseId)
            """)
    Page<Announcement> findVisible(@Param("level") Level level,
                                   @Param("courseId") Long courseId,
                                   Pageable pageable);
}
