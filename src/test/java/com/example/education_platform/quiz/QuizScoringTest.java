package com.example.education_platform.quiz;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.education_platform.quiz.entity.Choice;
import com.example.education_platform.quiz.entity.Question;
import com.example.education_platform.quiz.entity.QuestionType;
import com.example.education_platform.quiz.entity.Quiz;
import com.example.education_platform.quiz.entity.QuizAttempt;
import com.example.education_platform.quiz.entity.QuizStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The grading rules, exercised without a database — this is where a mistake costs students marks. */
class QuizScoringTest {

    private static final Instant NOW = Instant.parse("2026-03-10T09:00:00Z");

    @Test
    void aSingleChoiceQuestionScoresOnlyForTheRightChoice() {
        Question question = questionWith(QuestionType.SINGLE_CHOICE, "2", 5);

        assertThat(question.scoreFor(Set.of(2L))).isEqualByComparingTo("5");
        assertThat(question.scoreFor(Set.of(1L))).isEqualByComparingTo("0");
        assertThat(question.scoreFor(Set.of())).isEqualByComparingTo("0");
    }

    @Test
    void aMultipleChoiceQuestionIsAllOrNothing() {
        Question question = questionWith(QuestionType.MULTIPLE_CHOICE, "1,3", 4);

        assertThat(question.scoreFor(Set.of(1L, 3L))).isEqualByComparingTo("4");
        assertThat(question.scoreFor(Set.of(1L)))
                .describedAs("a partial answer earns nothing").isEqualByComparingTo("0");
        assertThat(question.scoreFor(Set.of(1L, 2L, 3L)))
                .describedAs("ticking every box must not earn the points").isEqualByComparingTo("0");
    }

    @Test
    void anOpenQuestionIsNeverScoredAutomatically() {
        Question question = questionWith(QuestionType.OPEN, "", 10);

        assertThat(question.scoreFor(Set.of())).isEqualByComparingTo("0");
        assertThat(question.getType().isAutoGradable()).isFalse();
    }

    @Test
    void totalPointsAddUpAcrossQuestions() {
        Quiz quiz = new Quiz(null, "Contrôle", null);
        quiz.addQuestion(questionWith(QuestionType.SINGLE_CHOICE, "1", 3), null);
        quiz.addQuestion(questionWith(QuestionType.MULTIPLE_CHOICE, "1,2", 7), null);

        assertThat(quiz.totalPoints()).isEqualByComparingTo("10");
        assertThat(quiz.isFullyAutoGradable()).isTrue();
    }

    @Test
    void oneOpenQuestionMakesTheWholeQuizManual() {
        Quiz quiz = new Quiz(null, "Contrôle", null);
        quiz.addQuestion(questionWith(QuestionType.SINGLE_CHOICE, "1", 3), null);
        quiz.addQuestion(questionWith(QuestionType.OPEN, "", 5), null);

        assertThat(quiz.isFullyAutoGradable()).isFalse();
    }

    @Test
    void questionsKeepContiguousPositions() {
        Quiz quiz = new Quiz(null, "Contrôle", null);
        quiz.addQuestion(questionWith(QuestionType.SINGLE_CHOICE, "1", 1), null);
        Question second = quiz.addQuestion(questionWith(QuestionType.SINGLE_CHOICE, "1", 1), null);
        quiz.addQuestion(questionWith(QuestionType.SINGLE_CHOICE, "1", 1), 1);

        assertThat(quiz.getQuestions().stream().map(Question::getPosition)).containsExactly(1, 2, 3);

        quiz.removeQuestion(second);
        assertThat(quiz.getQuestions().stream().map(Question::getPosition)).containsExactly(1, 2);
    }

    // ---------- the clock ----------

    @Test
    void anUntimedAttemptNeverExpiresOnTheClock() {
        Quiz quiz = openQuiz(null, null);
        QuizAttempt attempt = new QuizAttempt(quiz, null, 1, NOW);

        assertThat(attempt.expiresAt()).isNull();
        assertThat(attempt.hasRunOutOfTime(NOW.plus(10, ChronoUnit.DAYS))).isFalse();
    }

    @Test
    void aTimedAttemptExpiresAfterItsDuration() {
        Quiz quiz = openQuiz(30, null);
        QuizAttempt attempt = new QuizAttempt(quiz, null, 1, NOW);

        assertThat(attempt.expiresAt()).isEqualTo(NOW.plus(30, ChronoUnit.MINUTES));
        assertThat(attempt.hasRunOutOfTime(NOW.plus(29, ChronoUnit.MINUTES))).isFalse();
        assertThat(attempt.hasRunOutOfTime(NOW.plus(30, ChronoUnit.MINUTES))).isTrue();
    }

    @Test
    void theDeadlineCutsAnAttemptShortEvenWithTimeLeftOnTheClock() {
        Instant deadline = NOW.plus(5, ChronoUnit.MINUTES);
        Quiz quiz = openQuiz(60, deadline);
        QuizAttempt attempt = new QuizAttempt(quiz, null, 1, NOW);

        assertThat(attempt.hasRunOutOfTime(NOW.plus(4, ChronoUnit.MINUTES))).isFalse();
        assertThat(attempt.hasRunOutOfTime(NOW.plus(6, ChronoUnit.MINUTES)))
                .describedAs("the deadline wins over the remaining duration").isTrue();
    }

    @Test
    void aQuizIsOnlyOpenBetweenItsDatesAndWhileInProgress() {
        Quiz quiz = openQuiz(null, NOW.plus(1, ChronoUnit.HOURS));
        quiz.setOpensAt(NOW.minus(1, ChronoUnit.HOURS));

        assertThat(quiz.isOpenAt(NOW)).isTrue();
        assertThat(quiz.isOpenAt(NOW.minus(2, ChronoUnit.HOURS)))
                .describedAs("before it opens").isFalse();
        assertThat(quiz.isOpenAt(NOW.plus(2, ChronoUnit.HOURS)))
                .describedAs("after the deadline").isFalse();

        quiz.setStatus(QuizStatus.DRAFT);
        assertThat(quiz.isOpenAt(NOW)).describedAs("a draft is never open").isFalse();
    }

    // ---------- helpers ----------

    /** Builds a question whose choices have ids 1..3; {@code correctIds} lists the right ones. */
    private static Question questionWith(QuestionType type, String correctIds, int points) {
        Question question = new Question("Question ?", type, BigDecimal.valueOf(points));
        Set<String> correct = Set.of(correctIds.split(","));
        for (long id = 1; id <= 3; id++) {
            Choice choice = new Choice("Choix " + id, correct.contains(String.valueOf(id)));
            setId(choice, id);
            question.addChoice(choice, null);
        }
        return question;
    }

    private static Quiz openQuiz(Integer durationMinutes, Instant deadline) {
        Quiz quiz = new Quiz(null, "Contrôle", null);
        quiz.setStatus(QuizStatus.IN_PROGRESS);
        quiz.setDurationMinutes(durationMinutes);
        quiz.setDeadline(deadline);
        return quiz;
    }

    /** Ids are database-generated, so an unsaved choice needs one planted for scoring to compare. */
    private static void setId(Choice choice, Long id) {
        try {
            var field = Class.forName("com.example.education_platform.common.BaseEntity")
                    .getDeclaredField("id");
            field.setAccessible(true);
            field.set(choice, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("could not plant an id on a test choice", e);
        }
    }
}
