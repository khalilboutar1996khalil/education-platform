package com.example.education_platform.quiz.repository;

import com.example.education_platform.quiz.entity.Quiz;
import com.example.education_platform.quiz.entity.QuizStatus;
import com.example.education_platform.user.entity.Level;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    /**
     * Questions are join-fetched and their choices arrive through {@code @BatchSize} — the same
     * two-bag limitation as the course tree.
     */
    @EntityGraph(attributePaths = "questions")
    Optional<Quiz> findDetailById(Long id);

    /**
     * A student passes their own level and the statuses they are allowed to see. The course is
     * fetched because every row renders its module code.
     */
    @EntityGraph(attributePaths = "course")
    @Query("""
            select q from Quiz q
            where (:level is null or q.course.level = :level)
              and (:courseId is null or q.course.id = :courseId)
              and q.status in :statuses
            """)
    Page<Quiz> findVisible(@Param("level") Level level,
                           @Param("courseId") Long courseId,
                           @Param("statuses") Collection<QuizStatus> statuses,
                           Pageable pageable);

    /** What is coming up, soonest first. */
    @EntityGraph(attributePaths = "course")
    @Query("""
            select q from Quiz q
            where q.status = :status and q.deadline is not null and q.deadline > :now
              and (:level is null or q.course.level = :level)
            order by q.deadline
            """)
    List<Quiz> findUpcoming(@Param("status") QuizStatus status,
                            @Param("level") Level level,
                            @Param("now") Instant now,
                            Pageable pageable);

    long countByStatus(QuizStatus status);
}
