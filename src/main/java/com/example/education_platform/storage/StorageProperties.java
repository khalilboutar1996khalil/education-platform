package com.example.education_platform.storage;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * @param location            root directory files are written under, when no object store is set
 * @param maxFileSize         rejected above this, before anything is written
 * @param allowedContentTypes an allow-list, never a deny-list: anything unlisted is refused
 * @param s3                  an S3-compatible bucket (Cloudflare R2 in production); unused while
 *                            its bucket is blank
 */
@ConfigurationProperties("app.storage")
public record StorageProperties(String location, DataSize maxFileSize, List<String> allowedContentTypes, S3 s3) {

    /**
     * @param endpoint e.g. {@code https://<account-id>.r2.cloudflarestorage.com}
     * @param region   R2 ignores it but the client requires one; "auto" is what R2 documents
     */
    public record S3(String endpoint, String region, String bucket, String accessKey, String secretKey) {
    }
}
