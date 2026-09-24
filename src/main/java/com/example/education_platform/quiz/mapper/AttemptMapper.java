package com.example.education_platform.quiz.mapper;

import com.example.education_platform.quiz.dto.response.AttemptChoiceResponse;
import com.example.education_platform.quiz.dto.response.AttemptQuestionResponse;
import com.example.education_platform.quiz.entity.Choice;
import com.example.education_platform.quiz.entity.Question;
import java.util.List;
import org.mapstruct.Mapper;

/**
 * The student's view of a paper. Separate from {@link QuizMapper} on purpose: its target types
 * have no {@code correct} field, so no future edit here can expose the answer key.
 */
@Mapper
public interface AttemptMapper {

    List<AttemptQuestionResponse> toQuestions(List<Question> questions);

    AttemptQuestionResponse toQuestion(Question question);

    AttemptChoiceResponse toChoice(Choice choice);
}
