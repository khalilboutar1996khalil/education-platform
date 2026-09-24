package com.example.education_platform.quiz.mapper;

import com.example.education_platform.quiz.dto.response.ChoiceResponse;
import com.example.education_platform.quiz.dto.response.QuestionResponse;
import com.example.education_platform.quiz.dto.response.QuizDetailResponse;
import com.example.education_platform.quiz.dto.response.QuizSummaryResponse;
import com.example.education_platform.quiz.entity.Choice;
import com.example.education_platform.quiz.entity.Question;
import com.example.education_platform.quiz.entity.Quiz;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface QuizMapper {

    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseCode", source = "course.code")
    @Mapping(target = "questionCount", expression = "java(quiz.getQuestions().size())")
    @Mapping(target = "totalPoints", expression = "java(quiz.totalPoints())")
    QuizSummaryResponse toSummary(Quiz quiz);

    @Mapping(target = "courseId", source = "quiz.course.id")
    @Mapping(target = "courseCode", source = "quiz.course.code")
    @Mapping(target = "totalPoints", expression = "java(quiz.totalPoints())")
    QuizDetailResponse toDetail(Quiz quiz, long submissions, Double averageScore);

    QuestionResponse toQuestion(Question question);

    ChoiceResponse toChoice(Choice choice);
}
