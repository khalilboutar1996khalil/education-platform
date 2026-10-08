package com.example.education_platform.storage.service.impl;

import com.example.education_platform.security.CurrentUser;
import com.example.education_platform.storage.StorageProperties;
import com.example.education_platform.storage.entity.StoredFile;
import com.example.education_platform.storage.repository.StoredFileRepository;
import com.example.education_platform.storage.service.StorageService;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * Keeps uploads in an S3-compatible bucket — Cloudflare R2 in production, because the free hosting
 * plan's disk is wiped on every deploy. Active as soon as {@code app.storage.s3.bucket} is set.
 *
 * <p>Keys are the same UUID-based names the disk store uses, so nothing about a file's address
 * comes from the client.
 */
@Service
@ConditionalOnExpression("!'${app.storage.s3.bucket:}'.isBlank()")
public class S3StorageService implements StorageService {

    private final StorageProperties properties;
    private final StoredFileRepository storedFiles;
    private final CurrentUser currentUser;
    private final S3Client s3;
    private final String bucket;

    public S3StorageService(StorageProperties properties, StoredFileRepository storedFiles, CurrentUser currentUser) {
        this(properties, storedFiles, currentUser, buildClient(properties.s3()));
    }

    S3StorageService(StorageProperties properties, StoredFileRepository storedFiles, CurrentUser currentUser,
                     S3Client s3) {
        this.properties = properties;
        this.storedFiles = storedFiles;
        this.currentUser = currentUser;
        this.s3 = s3;
        this.bucket = properties.s3().bucket();
    }

    private static S3Client buildClient(StorageProperties.S3 config) {
        return S3Client.builder()
                .endpointOverride(URI.create(config.endpoint()))
                .region(Region.of(config.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.accessKey(), config.secretKey())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                // R2 rejects the CRC checksums recent SDKs add to every request by default
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }

    @PreDestroy
    void close() {
        s3.close();
    }

    @Override
    @Transactional
    public StoredFile store(MultipartFile file) {
        UploadRules.validate(file, properties);

        String originalFilename = UploadRules.sanitize(file.getOriginalFilename());
        String storageKey = UUID.randomUUID() + UploadRules.extensionOf(originalFilename);
        long size = file.getSize();

        // The SDK may reopen the stream to retry, so the digest is taken from the last pass
        AtomicReference<MessageDigest> digest = new AtomicReference<>();
        RequestBody body = RequestBody.fromContentProvider(() -> {
            MessageDigest pass = UploadRules.sha256();
            digest.set(pass);
            return new DigestInputStream(open(file), pass);
        }, size, file.getContentType());

        s3.putObject(put -> put.bucket(bucket).key(storageKey).contentType(file.getContentType()), body);

        StoredFile stored = new StoredFile(originalFilename, file.getContentType(), size,
                storageKey, HexFormat.of().formatHex(digest.get().digest()), currentUser.get());
        return storedFiles.save(stored);
    }

    @Override
    public Resource loadAsResource(StoredFile file) {
        return new ObjectResource(file);
    }

    @Override
    public void delete(StoredFile file) {
        s3.deleteObject(del -> del.bucket(bucket).key(file.getStorageKey()));
    }

    private static InputStream open(MultipartFile file) {
        try {
            return file.getInputStream();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the uploaded file", e);
        }
    }

    /** Opens the object only when the response is written, so a failed request never downloads it. */
    private final class ObjectResource extends AbstractResource {

        private final StoredFile file;

        private ObjectResource(StoredFile file) {
            this.file = file;
        }

        @Override
        public InputStream getInputStream() {
            return s3.getObject(get -> get.bucket(bucket).key(file.getStorageKey()));
        }

        @Override
        public long contentLength() {
            return file.getSizeBytes();
        }

        @Override
        public String getFilename() {
            return file.getOriginalFilename();
        }

        @Override
        public String getDescription() {
            return "object " + file.getStorageKey() + " in bucket " + bucket;
        }
    }
}
