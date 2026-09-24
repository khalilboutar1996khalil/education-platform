package com.example.education_platform.quiz.job;

import com.example.education_platform.quiz.entity.AttemptStatus;
import com.example.education_platform.quiz.entity.QuizAttempt;
import com.example.education_platform.quiz.entity.QuizStatus;
import com.example.education_platform.quiz.repository.QuizAttemptRepository;
import com.example.education_platform.quiz.repository.QuizRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Closes quizzes whose deadline has passed and writes off attempts that ran out of time.
 *
 * <p>Until now an expired attempt was only noticed when somebody touched it, so one left open and
 * abandoned stayed IN_PROGRESS forever and kept blocking a new attempt.
 */
@Component
@RequiredArgsConstructor
public class QuizDeadlineJob {

    private static final Logger LOG = LoggerFactory.getLogger(QuizDeadlineJob.class);

    private final QuizRepository quizzes;
    private final QuizAttemptRepository attempts;

    /** Every five minutes: fine grained enough for a deadline, cheap enough to ignore. */
    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
    @Transactional
    public void run() {
        Instant now = Instant.now();
        int closed = closeOverdueQuizzes(now);
        int expired = expireStaleAttempts(now);
        if (closed > 0 || expired > 0) {
            LOG.info("Quiz deadline job: closed {} quizzes, expired {} attempts", closed, expired);
        }
    }

    public int closeOverdueQuizzes(Instant now) {
        List<com.example.education_platform.quiz.entity.Quiz> overdue =
                quizzes.findOverdue(QuizStatus.IN_PROGRESS, now);
        overdue.forEach(quiz -> quiz.setStatus(QuizStatus.CLOSED));
        return overdue.size();
    }

    public int expireStaleAttempts(Instant now) {
        List<QuizAttempt> running = attempts.findByStatus(AttemptStatus.IN_PROGRESS);
        int expired = 0;
        for (QuizAttempt attempt : running) {
            if (attempt.hasRunOutOfTime(now)) {
                attempt.setStatus(AttemptStatus.EXPIRED);
                attempt.setSubmittedAt(now);
                expired++;
            }
        }
        return expired;
    }
}
