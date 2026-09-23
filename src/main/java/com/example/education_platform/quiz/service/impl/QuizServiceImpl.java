package com.example.education_platform.quiz.service.impl;

import com.example.education_platform.common.PageResponse;
import com.example.education_platform.common.exception.BusinessException;
import com.example.education_platform.common.exception.ResourceNotFoundException;
import com.example.education_platform.course.entity.Course;
import com.example.education_platform.course.repository.CourseRepository;
import com.example.education_platform.quiz.dto.request.ChoiceRequest;
import com.example.education_platform.quiz.dto.request.QuestionRequest;
import com.example.education_platform.quiz.dto.request.QuizRequest;
import com.example.education_platform.quiz.dto.response.QuizDetailResponse;
import com.example.education_platform.quiz.dto.response.QuizSummaryResponse;
import com.example.education_platform.quiz.entity.AttemptStatus;
import com.example.education_platform.quiz.entity.Choice;
import com.example.education_platform.quiz.entity.Question;
import com.example.education_platform.quiz.entity.QuestionType;
import com.example.education_platform.quiz.entity.Quiz;
import com.example.education_platform.quiz.entity.QuizStatus;
import com.example.education_platform.quiz.mapper.QuizMapper;
import com.example.education_platform.quiz.repository.QuestionRepository;
import com.example.education_platform.quiz.repository.QuizAttemptRepository;
import com.example.education_platform.quiz.repository.QuizRepository;
import com.example.education_platform.quiz.repository.QuizStats;
import com.example.education_platform.quiz.service.QuizService;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizServiceImpl implements QuizService {

    private static final EnumSet<AttemptStatus> HANDED_IN =
            EnumSet.of(AttemptStatus.SUBMITTED, AttemptStatus.GRADED);

    private final QuizRepository quizzes;
    private final QuestionRepository questions;
    private final QuizAttemptRepository attempts;
    private final CourseRepository courses;
    private final QuizMapper quizMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<QuizSummaryResponse> list(Long courseId, Pageable pageable) {
        // An admin sees drafts too, so every status is in scope
        Page<Quiz> page = quizzes.findVisible(null, courseId, EnumSet.allOf(QuizStatus.class), pageable);
        List<QuizSummaryResponse> content = page.getContent().stream().map(quizMapper::toSummary).toList();
        return PageResponse.from(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public QuizDetailResponse getDetail(Long id) {
        return detailOf(quiz(id));
    }

    @Override
    public QuizDetailResponse create(Long courseId, QuizRequest request) {
        Course course = courses.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
        Quiz quiz = new Quiz(course, request.title(), request.description());
        applySettings(quiz, request);
        Quiz saved = quizzes.save(quiz);
        // The stats query below needs the generated id
        quizzes.flush();
        return detailOf(saved);
    }

    @Override
    public QuizDetailResponse update(Long id, QuizRequest request) {
        Quiz quiz = quiz(id);
        quiz.setTitle(request.title());
        quiz.setDescription(request.description());
        applySettings(quiz, request);
        return detailOf(quiz);
    }

    @Override
    public QuizDetailResponse changeStatus(Long id, QuizStatus status) {
        Quiz quiz = quiz(id);
        if (status == QuizStatus.IN_PROGRESS && quiz.getQuestions().isEmpty()) {
            throw new BusinessException("A quiz cannot be opened before it has at least one question");
        }
        quiz.setStatus(status);
        return detailOf(quiz);
    }

    @Override
    public void delete(Long id) {
        quizzes.delete(quiz(id));
    }

    @Override
    public QuizDetailResponse addQuestion(Long quizId, QuestionRequest request) {
        Quiz quiz = quiz(quizId);
        requireDraft(quiz);
        validate(request);

        Question question = new Question(request.text(), request.type(), request.points());
        quiz.addQuestion(question, request.position());
        replaceChoices(question, request.choices());
        quizzes.flush();
        return detailOf(quiz);
    }

    @Override
    public QuizDetailResponse updateQuestion(Long questionId, QuestionRequest request) {
        Question question = questions.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", questionId));
        Quiz quiz = question.getQuiz();
        requireDraft(quiz);
        validate(request);

        question.setText(request.text());
        question.setType(request.type());
        question.setPoints(request.points());
        replaceChoices(question, request.choices());
        if (request.position() != null && request.position() != question.getPosition()) {
            quiz.moveQuestion(question, request.position());
        }
        quizzes.flush();
        return detailOf(quiz);
    }

    @Override
    public QuizDetailResponse deleteQuestion(Long questionId) {
        Question question = questions.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", questionId));
        Quiz quiz = question.getQuiz();
        requireDraft(quiz);
        quiz.removeQuestion(question);
        return detailOf(quiz);
    }

    private static void applySettings(Quiz quiz, QuizRequest request) {
        if (request.opensAt() != null && request.deadline() != null
                && !request.deadline().isAfter(request.opensAt())) {
            throw new BusinessException("The deadline must come after the opening date");
        }
        quiz.setDurationMinutes(request.durationMinutes());
        quiz.setOpensAt(request.opensAt());
        quiz.setDeadline(request.deadline());
        quiz.setMaxAttempts(request.maxAttemptsOrDefault());
        quiz.setShuffleQuestions(request.shuffleQuestionsOrDefault());
    }

    /**
     * Questions are frozen once a quiz leaves DRAFT: changing them afterwards would silently
     * rescore attempts that were sat against a different paper.
     */
    private static void requireDraft(Quiz quiz) {
        if (quiz.getStatus() != QuizStatus.DRAFT) {
            throw new BusinessException("Questions can only be edited while the quiz is a draft");
        }
    }

    private static void validate(QuestionRequest request) {
        List<ChoiceRequest> choices = request.choices() == null ? List.of() : request.choices();
        long correct = choices.stream().filter(ChoiceRequest::correct).count();

        if (request.type() == QuestionType.OPEN) {
            if (!choices.isEmpty()) {
                throw new BusinessException("An open question cannot have choices");
            }
            return;
        }
        if (request.type() == QuestionType.TRUE_FALSE && choices.size() != 2) {
            throw new BusinessException("A true/false question needs exactly two choices");
        }
        if (choices.size() < 2) {
            throw new BusinessException("A choice question needs at least two choices");
        }
        if (request.type() == QuestionType.MULTIPLE_CHOICE ? correct < 1 : correct != 1) {
            throw new BusinessException(request.type() == QuestionType.MULTIPLE_CHOICE
                    ? "A multiple-choice question needs at least one correct choice"
                    : "This question needs exactly one correct choice");
        }
    }

    private static void replaceChoices(Question question, List<ChoiceRequest> requested) {
        question.getChoices().clear();
        if (requested == null) {
            return;
        }
        requested.forEach(choice -> question.addChoice(new Choice(choice.text(), choice.correct()), null));
    }

    private Quiz quiz(Long id) {
        return quizzes.findDetailById(id).orElseThrow(() -> new ResourceNotFoundException("Quiz", id));
    }

    private QuizDetailResponse detailOf(Quiz quiz) {
        QuizStats stats = attempts.findStats(quiz.getId(), HANDED_IN);
        return quizMapper.toDetail(quiz, stats.getSubmissions(), stats.getAverageScore());
    }
}
