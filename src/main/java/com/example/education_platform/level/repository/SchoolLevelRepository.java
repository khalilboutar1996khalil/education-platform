package com.example.education_platform.level.repository;

import com.example.education_platform.level.entity.SchoolLevel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolLevelRepository extends JpaRepository<SchoolLevel, Long> {

    List<SchoolLevel> findAllByOrderByPositionAscCodeAsc();

    List<SchoolLevel> findByActiveTrueOrderByPositionAscCodeAsc();

    Optional<SchoolLevel> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndActiveTrue(String code);
}
