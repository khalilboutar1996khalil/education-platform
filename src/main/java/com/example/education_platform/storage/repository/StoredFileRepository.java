package com.example.education_platform.storage.repository;

import com.example.education_platform.storage.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
}
