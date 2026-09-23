package com.example.education_platform.quiz.repository;

import com.example.education_platform.quiz.entity.AttemptStatus;
import com.example.education_platform.quiz.entity.QuizAttempt;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    @EntityGraph(attributePaths = "answers")
    Optional<QuizAttempt> findDetailById(Long id);

    List<QuizAttempt> findByQuizIdAndStudentIdOrderByAttemptNumberAsc(Long quizId, Long studentId);

    Optional<QuizAttempt> findFirstByQuizIdAndStudentIdAndStatus(Long quizId, Long studentId, AttemptStatus status);

    long countByQuizIdAndStudentId(Long quizId, Long studentId);

    /** Stats line on the admin's quiz screen: how many handed in, and the average of their scores. */
    @Query("""
            select count(a) as submissions, avg(a.score) as averageScore from QuizAttempt a
            where a.quiz.id = :quizId and a.status in :statuses
            """)
    QuizStats findStats(@Param("quizId") Long quizId, @Param("statuses") Collection<AttemptStatus> statuses);
}
