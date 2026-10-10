package com.example.education_platform.storage.service.impl;

import com.example.education_platform.common.exception.BusinessException;
import com.example.education_platform.storage.StorageProperties;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import org.springframework.web.multipart.MultipartFile;

/**
 * What every storage backend checks and derives before writing, so a file accepted on local disk
 * is accepted on the object store too.
 *
 * <p>The client's filename never becomes a path or a key: keys are built from a generated UUID,
 * and the original name is kept only as metadata.
 */
final class UploadRules {

    private static final int MAX_FILENAME_LENGTH = 255;
    private static final int MAX_EXTENSION_LENGTH = 10;
    private static final String FALLBACK_FILENAME = "fichier";

    private UploadRules() {
    }

    static void validate(MultipartFile file, StorageProperties properties) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("The file is empty");
        }
        if (file.getSize() > properties.maxFileSize().toBytes()) {
            throw tooLarge(properties);
        }
        String contentType = file.getContentType();
        if (contentType == null || !properties.allowedContentTypes().contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("Files of type " + contentType + " are not accepted");
        }
    }

    static BusinessException tooLarge(StorageProperties properties) {
        return new BusinessException("The file exceeds the maximum size of "
                + properties.maxFileSize().toMegabytes() + " MB");
    }

    /** Keeps the leaf name only, so "../../etc/passwd" survives as "passwd" and nothing more. */
    static String sanitize(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return FALLBACK_FILENAME;
        }
        String name = originalFilename.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1)
                .replaceAll("\\p{Cntrl}", "")
                .trim();
        if (name.isBlank() || ".".equals(name) || "..".equals(name)) {
            return FALLBACK_FILENAME;
        }
        return name.length() > MAX_FILENAME_LENGTH ? name.substring(0, MAX_FILENAME_LENGTH) : name;
    }

    static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        String extension = filename.substring(dot + 1).replaceAll("[^A-Za-z0-9]", "");
        if (extension.isEmpty()) {
            return "";
        }
        return "." + extension.substring(0, Math.min(extension.length(), MAX_EXTENSION_LENGTH))
                .toLowerCase(Locale.ROOT);
    }

    static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every JVM", e);
        }
    }
}
