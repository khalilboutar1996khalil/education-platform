package com.example.education_platform.blog.repository;

import com.example.education_platform.blog.entity.BlogCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogCategoryRepository extends JpaRepository<BlogCategory, Long> {

    Optional<BlogCategory> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<BlogCategory> findAllByOrderByNameAsc();
}
