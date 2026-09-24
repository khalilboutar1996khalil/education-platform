package com.example.education_platform.quiz.repository;

import com.example.education_platform.quiz.entity.Choice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChoiceRepository extends JpaRepository<Choice, Long> {

    List<Choice> findByIdInAndQuestionId(List<Long> ids, Long questionId);
}
