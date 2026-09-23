package com.example.education_platform.quiz.repository;

import com.example.education_platform.quiz.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
}
