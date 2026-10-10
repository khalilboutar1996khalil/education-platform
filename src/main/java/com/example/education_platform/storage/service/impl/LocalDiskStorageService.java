package com.example.education_platform.storage.service.impl;

import com.example.education_platform.common.exception.BusinessException;
import com.example.education_platform.security.CurrentUser;
import com.example.education_platform.storage.StorageProperties;
import com.example.education_platform.storage.entity.StoredFile;
import com.example.education_platform.storage.repository.StoredFileRepository;
import com.example.education_platform.storage.service.StorageService;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Writes uploads under a configured root. Used whenever no object store is configured (local
 * development, tests).
 *
 * <p>The client's filename never reaches the filesystem: the path is built from a generated UUID,
 * which makes traversal impossible by construction rather than by escaping. The original name is
 * kept only as metadata, and every resolved path is checked to still sit inside the root.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("'${app.storage.s3.bucket:}'.isBlank()")
public class LocalDiskStorageService implements StorageService {

    private final StorageProperties properties;
    private final StoredFileRepository storedFiles;
    private final CurrentUser currentUser;

    private Path root;

    @PostConstruct
    void prepareRoot() {
        root = Paths.get(properties.location()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create the storage directory at " + root, e);
        }
    }

    @Override
    @Transactional
    public StoredFile store(MultipartFile file) {
        UploadRules.validate(file, properties);

        String originalFilename = UploadRules.sanitize(file.getOriginalFilename());
        String storageKey = UUID.randomUUID() + UploadRules.extensionOf(originalFilename);
        Path target = resolve(storageKey);

        MessageDigest digest = UploadRules.sha256();
        long written = write(file, target, digest);

        // The declared size can lie; this is the byte count actually written
        if (written > properties.maxFileSize().toBytes()) {
            deleteQuietly(target);
            throw UploadRules.tooLarge(properties);
        }

        StoredFile stored = new StoredFile(originalFilename, file.getContentType(), written,
                storageKey, HexFormat.of().formatHex(digest.digest()), currentUser.get());
        return storedFiles.save(stored);
    }

    @Override
    public Resource loadAsResource(StoredFile file) {
        Path path = resolve(file.getStorageKey());
        if (!Files.isReadable(path)) {
            throw new BusinessException("The stored file is missing from disk: " + file.getStorageKey());
        }
        return new PathResource(path);
    }

    @Override
    public void delete(StoredFile file) {
        deleteQuietly(resolve(file.getStorageKey()));
    }

    // ---------- paths ----------

    /** Two levels of sharding, so one directory never ends up holding every upload. */
    private Path resolve(String storageKey) {
        Path target = root
                .resolve(storageKey.substring(0, 2))
                .resolve(storageKey.substring(2, 4))
                .resolve(storageKey)
                .normalize();
        if (!target.startsWith(root)) {
            // Unreachable while keys are generated here, but a cheap guard against a future caller
            throw new IllegalStateException("Resolved path escaped the storage root");
        }
        return target;
    }

    private long write(MultipartFile file, Path target, MessageDigest digest) {
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream();
                    DigestInputStream digesting = new DigestInputStream(in, digest)) {
                return Files.copy(digesting, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            deleteQuietly(target);
            throw new UncheckedIOException("Could not write the uploaded file", e);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete " + path, e);
        }
    }

}
