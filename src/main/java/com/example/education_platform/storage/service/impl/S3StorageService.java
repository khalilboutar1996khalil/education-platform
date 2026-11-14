package com.example.education_platform.storage.service.impl;

import com.example.education_platform.common.exception.BusinessException;
import com.example.education_platform.security.CurrentUser;
import com.example.education_platform.storage.StorageProperties;
import com.example.education_platform.storage.entity.StoredFile;
import com.example.education_platform.storage.repository.StoredFileRepository;
import com.example.education_platform.storage.service.StorageService;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Keeps uploads in an S3-compatible object store (Cloudflare R2 in production), so they survive
 * restarts and redeploys on hosts whose disk is wiped, such as Render's free plan.
 *
 * <p>Same rules as {@link LocalDiskStorageService} ({@link UploadRules}): the object key is a
 * generated UUID, never the client's filename. The upload is first copied to a temporary file so
 * the real byte count and checksum are known before anything is sent, then the temp file is removed.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "s3")
public class S3StorageService implements StorageService {

    private final StorageProperties properties;
    private final StoredFileRepository storedFiles;
    private final CurrentUser currentUser;
    private final S3Client s3;

    @Override
    @Transactional
    public StoredFile store(MultipartFile file) {
        UploadRules.validate(file, properties);

        String originalFilename = UploadRules.sanitize(file.getOriginalFilename());
        String storageKey = UUID.randomUUID() + UploadRules.extensionOf(originalFilename);
        MessageDigest digest = UploadRules.sha256();

        Path temp = null;
        try {
            temp = Files.createTempFile("eduflow-upload-", ".tmp");
            long written;
            try (InputStream in = file.getInputStream();
                    DigestInputStream digesting = new DigestInputStream(in, digest)) {
                written = Files.copy(digesting, temp, StandardCopyOption.REPLACE_EXISTING);
            }
            // The declared size can lie; this is the byte count actually received
            if (written > properties.maxFileSize().toBytes()) {
                throw UploadRules.tooLarge(properties);
            }
            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucket())
                            .key(storageKey)
                            .contentType(file.getContentType())
                            .contentLength(written)
                            .build(),
                    RequestBody.fromFile(temp));

            StoredFile stored = new StoredFile(originalFilename, file.getContentType(), written,
                    storageKey, HexFormat.of().formatHex(digest.digest()), currentUser.find().orElse(null));
            return storedFiles.save(stored);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the uploaded file", e);
        } finally {
            deleteTemp(temp);
        }
    }

    @Override
    public Resource loadAsResource(StoredFile file) {
        return new S3ObjectResource(s3, bucket(), file.getStorageKey(), file.getSizeBytes());
    }

    @Override
    public void delete(StoredFile file) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket()).key(file.getStorageKey()).build());
    }

    private String bucket() {
        return properties.s3().bucket();
    }

    private static void deleteTemp(Path temp) {
        if (temp == null) {
            return;
        }
        try {
            Files.deleteIfExists(temp);
        } catch (IOException e) {
            // A leftover temp file is harmless; try once more when the JVM exits
            temp.toFile().deleteOnExit();
        }
    }

    /** Opens a fresh stream from the bucket each time it is read, so it streams instead of buffering. */
    static final class S3ObjectResource extends AbstractResource {

        private final S3Client s3;
        private final String bucket;
        private final String key;
        private final long size;

        S3ObjectResource(S3Client s3, String bucket, String key, long size) {
            this.s3 = s3;
            this.bucket = bucket;
            this.key = key;
            this.size = size;
        }

        @Override
        public InputStream getInputStream() {
            try {
                return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
            } catch (NoSuchKeyException e) {
                throw new BusinessException("The stored file is missing from the bucket: " + key);
            }
        }

        @Override
        public long contentLength() {
            return size;
        }

        @Override
        public String getFilename() {
            return key;
        }

        @Override
        public String getDescription() {
            return "S3 object [" + bucket + "/" + key + "]";
        }
    }
}
