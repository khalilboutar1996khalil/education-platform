package com.example.education_platform.classcode.repository;

import com.example.education_platform.classcode.entity.ClassCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassCodeRepository extends JpaRepository<ClassCode, Long> {

    Optional<ClassCode> findByCodeIgnoreCaseAndActiveTrue(String code);

    boolean existsByCodeIgnoreCase(String code);
}
