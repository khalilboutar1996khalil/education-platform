package com.example.education_platform.resource.repository;

import com.example.education_platform.resource.entity.Resource;
import com.example.education_platform.resource.entity.ResourceType;
import com.example.education_platform.user.entity.Level;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    @EntityGraph(attributePaths = {"course", "file"})
    Optional<Resource> findDetailById(Long id);

    /**
     * A null level is an admin and matches everything. For a student, neither scope may exclude
     * them: a resource with no course and no level is visible to the whole section.
     */
    @EntityGraph(attributePaths = {"course", "file"})
    @Query("""
            select r from Resource r
            left join r.course c
            where (:level is null
                   or ((c is null or c.level = :level)
                       and (r.level is null or r.level = :level)))
              and (:courseId is null or c.id = :courseId)
              and (:type is null or r.type = :type)
              and (:search is null or lower(r.title) like :search)
            """)
    Page<Resource> findVisible(@Param("level") Level level,
                               @Param("courseId") Long courseId,
                               @Param("type") ResourceType type,
                               @Param("search") String search,
                               Pageable pageable);

    /**
     * Incremented in the database so two simultaneous downloads cannot lose one another's count.
     * A bulk update bypasses the persistence context, hence the flags: without them the loaded
     * entity keeps reporting the old total for the rest of the transaction.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Resource r set r.downloadCount = r.downloadCount + 1 where r.id = :id")
    void incrementDownloadCount(@Param("id") Long id);
}
