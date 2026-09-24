package com.example.education_platform.notification.repository;

import com.example.education_platform.notification.entity.ActivityEvent;
import com.example.education_platform.user.entity.Level;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, Long> {

    /**
     * A student sees activity in their own level; an admin passes null and sees everything.
     * LEFT JOIN, so events with no course still appear.
     */
    @EntityGraph(attributePaths = {"actor", "course"})
    @Query("""
            select e from ActivityEvent e
            left join e.course c
            where :level is null or c is null or c.level = :level
            order by e.occurredAt desc
            """)
    Page<ActivityEvent> findFeed(@Param("level") Level level, Pageable pageable);
}
