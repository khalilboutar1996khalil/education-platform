package com.example.education_platform.quiz.service.impl;

import com.example.education_platform.common.PageResponse;
import com.example.education_platform.common.exception.BusinessException;
import com.example.education_platform.common.exception.ResourceNotFoundException;
import com.example.education_platform.quiz.dto.request.AnswerRequest;
import com.example.education_platform.quiz.dto.request.SaveAnswersRequest;
import com.example.education_platform.quiz.dto.response.AnswerStateResponse;
import com.example.education_platform.quiz.dto.response.AttemptResponse;
import com.example.education_platform.quiz.dto.response.StudentQuizResponse;
import com.example.education_platform.quiz.entity.Answer;
import com.example.education_platform.quiz.entity.AttemptStatus;
import com.example.education_platform.quiz.entity.Choice;
import com.example.education_platform.quiz.entity.Question;
import com.example.education_platform.quiz.entity.QuestionType;
import com.example.education_platform.quiz.entity.Quiz;
import com.example.education_platform.quiz.entity.QuizAttempt;
import com.example.education_platform.quiz.entity.QuizStatus;
import com.example.education_platform.quiz.mapper.AttemptMapper;
import com.example.education_platform.quiz.repository.ChoiceRepository;
import com.example.education_platform.quiz.repository.QuizAttemptRepository;
import com.example.education_platform.quiz.repository.QuizCount;
import com.example.education_platform.quiz.repository.QuizRepository;
import com.example.education_platform.quiz.service.AttemptService;
import com.example.education_platform.security.CurrentUser;
import com.example.education_platform.user.entity.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AttemptServiceImpl implements AttemptService {

    /** A draft is invisible to students; a closed quiz stays readable so results can be reviewed. */
    private static final EnumSet<QuizStatus> VISIBLE_TO_STUDENTS =
            EnumSet.of(QuizStatus.IN_PROGRESS, QuizStatus.CLOSED);

    private final QuizRepository quizzes;
    private final QuizAttemptRepository attempts;
    private final ChoiceRepository choices;
    private final AttemptMapper attemptMapper;
    private final CurrentUser currentUser;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StudentQuizResponse> listMyQuizzes(Long courseId, Pageable pageable) {
        User student = requireStudent();
        Page<Quiz> page = quizzes.findVisible(student.getLevel(), courseId, VISIBLE_TO_STUDENTS, pageable);

        Map<Long, Long> used = attempts.countPerQuizForStudent(student.getId()).stream()
                .collect(Collectors.toMap(QuizCount::getQuizId, QuizCount::getTotal));

        Instant now = Instant.now();
        List<StudentQuizResponse> content = page.getContent().stream()
                .map(quiz -> new StudentQuizResponse(
                        quiz.getId(), quiz.getCourse().getId(), quiz.getCourse().getCode(),
                        quiz.getTitle(), quiz.getDescription(), quiz.getStatus(),
                        quiz.getDurationMinutes(), quiz.getOpensAt(), quiz.getDeadline(),
                        quiz.getMaxAttempts(), used.getOrDefault(quiz.getId(), 0L),
                        quiz.getQuestions().size(), quiz.totalPoints(), quiz.isOpenAt(now)))
                .toList();
        return PageResponse.from(page, content);
    }

    @Override
    public AttemptResponse start(Long quizId) {
        User student = requireStudent();
        Quiz quiz = quizzes.findDetailById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", quizId));
        requireSameLevel(student, quiz);

        // Resuming beats starting again: a refresh mid-quiz must not burn an attempt
        Optional<QuizAttempt> running = attempts.findFirstByQuizIdAndStudentIdAndStatus(
                quizId, student.getId(), AttemptStatus.IN_PROGRESS);
        if (running.isPresent()) {
            return respond(expireIfOutOfTime(running.get(), Instant.now()));
        }

        Instant now = Instant.now();
        if (!quiz.isOpenAt(now)) {
            throw new BusinessException("This quiz is not open");
        }
        long used = attempts.countByQuizIdAndStudentId(quizId, student.getId());
        if (used >= quiz.getMaxAttempts()) {
            throw new BusinessException("No attempts left for this quiz");
        }

        QuizAttempt attempt = attempts.save(
                new QuizAttempt(quiz, student, (int) used + 1, now));
        attempts.flush();
        return respond(attempt);
    }

    @Override
    @Transactional(readOnly = true)
    public AttemptResponse get(Long attemptId) {
        return respond(myAttempt(attemptId));
    }

    @Override
    public AttemptResponse saveAnswers(Long attemptId, SaveAnswersRequest request) {
        QuizAttempt attempt = requireEditable(myAttempt(attemptId));
        request.answers().forEach(answer -> upsert(attempt, answer));
        attempts.flush();
        return respond(attempt);
    }

    @Override
    public AttemptResponse submit(Long attemptId) {
        QuizAttempt attempt = requireEditable(myAttempt(attemptId));
        Quiz quiz = attempt.getQuiz();

        BigDecimal score = attempt.getAnswers().stream()
                .map(Answer::grade)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        attempt.setScore(score);
        // Snapshot: the quiz may change later, and this result must keep meaning what it meant
        attempt.setMaxScore(quiz.totalPoints());
        attempt.setSubmittedAt(Instant.now());
        attempt.setStatus(quiz.isFullyAutoGradable() ? AttemptStatus.GRADED : AttemptStatus.SUBMITTED);
        attempts.flush();
        return respond(attempt);
    }

    // ---------- answers ----------

    private void upsert(QuizAttempt attempt, AnswerRequest request) {
        Question question = attempt.getQuiz().getQuestions().stream()
                .filter(candidate -> candidate.getId().equals(request.questionId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "Question " + request.questionId() + " is not part of this quiz"));

        List<Long> requestedChoiceIds = request.selectedChoiceIdsOrEmpty();
        Set<Choice> selected = resolveChoices(question, requestedChoiceIds);

        Answer answer = attempt.getAnswers().stream()
                .filter(candidate -> candidate.getQuestion().getId().equals(question.getId()))
                .findFirst()
                .orElseGet(() -> attempt.addAnswer(new Answer(question)));

        answer.setSelectedChoices(selected);
        answer.setTextAnswer(request.textAnswer());
    }

    private Set<Choice> resolveChoices(Question question, List<Long> requestedIds) {
        if (requestedIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        if (question.getType() == QuestionType.OPEN) {
            throw new BusinessException("An open question takes text, not choices");
        }
        if (requestedIds.size() > 1 && question.getType() != QuestionType.MULTIPLE_CHOICE) {
            throw new BusinessException("This question accepts only one choice");
        }

        List<Choice> found = choices.findByIdInAndQuestionId(requestedIds, question.getId());
        if (found.size() != Set.copyOf(requestedIds).size()) {
            // Refusing rather than ignoring: a choice from another question means a broken client
            throw new BusinessException("A selected choice does not belong to this question");
        }
        return new LinkedHashSet<>(found);
    }

    // ---------- guards ----------

    private User requireStudent() {
        User me = currentUser.get();
        if (me.isAdmin()) {
            throw new AccessDeniedException("Admins do not sit quizzes");
        }
        return me;
    }

    private QuizAttempt myAttempt(Long attemptId) {
        User student = requireStudent();
        QuizAttempt attempt = attempts.findDetailById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt", attemptId));
        if (!attempt.getStudent().getId().equals(student.getId())) {
            throw new AccessDeniedException("This attempt belongs to another student");
        }
        return attempt;
    }

    /** Also the place a run-out attempt is finally written off, since no scheduler exists yet. */
    private QuizAttempt requireEditable(QuizAttempt attempt) {
        Instant now = Instant.now();
        expireIfOutOfTime(attempt, now);
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BusinessException("This attempt is no longer open");
        }
        return attempt;
    }

    private QuizAttempt expireIfOutOfTime(QuizAttempt attempt, Instant now) {
        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS && attempt.hasRunOutOfTime(now)) {
            attempt.setStatus(AttemptStatus.EXPIRED);
            attempt.setSubmittedAt(now);
        }
        return attempt;
    }

    private static void requireSameLevel(User student, Quiz quiz) {
        if (quiz.getCourse().getLevel() != student.getLevel()) {
            throw new AccessDeniedException("This quiz belongs to another level");
        }
    }

    // ---------- assembly ----------

    private AttemptResponse respond(QuizAttempt attempt) {
        Quiz quiz = attempt.getQuiz();
        boolean finished = attempt.getStatus() != AttemptStatus.IN_PROGRESS;

        List<AnswerStateResponse> answers = attempt.getAnswers().stream()
                .map(answer -> new AnswerStateResponse(
                        answer.getQuestion().getId(),
                        answer.getSelectedChoices().stream().map(Choice::getId).toList(),
                        answer.getTextAnswer(),
                        // Per-question marks stay hidden until the paper is handed in
                        finished ? answer.getAwardedPoints() : null))
                .toList();

        return new AttemptResponse(
                attempt.getId(), quiz.getId(), quiz.getTitle(), attempt.getAttemptNumber(),
                attempt.getStatus(), attempt.getStartedAt(), attempt.expiresAt(),
                attempt.getSubmittedAt(), attempt.getScore(), attempt.getMaxScore(),
                attemptMapper.toQuestions(quiz.getQuestions()), answers);
    }
}
