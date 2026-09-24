package com.example.education_platform.storage.service;

import com.example.education_platform.storage.entity.StoredFile;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Where uploads live. Local disk today; an object store later needs only another implementation,
 * because nothing outside this package knows how a file is addressed.
 */
public interface StorageService {

    /** Validates, writes and records the file. Rejects anything outside the allow-list. */
    StoredFile store(MultipartFile file);

    Resource loadAsResource(StoredFile file);

    /** Removes the bytes; the caller is responsible for the row that points at them. */
    void delete(StoredFile file);
}
