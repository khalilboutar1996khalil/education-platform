package com.example.education_platform.storage;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * @param location            root directory files are written under ({@code local} storage)
 * @param maxFileSize         rejected above this, before anything touches the disk
 * @param allowedContentTypes an allow-list, never a deny-list: anything unlisted is refused
 * @param type                {@code local} (default: the server's disk) or {@code s3} (an object store
 *                            such as Cloudflare R2, which survives restarts and redeploys)
 * @param s3                  connection to the object store, used only when {@code type} is {@code s3}
 */
@ConfigurationProperties("app.storage")
public record StorageProperties(String location, DataSize maxFileSize, List<String> allowedContentTypes,
                                String type, S3 s3) {

    /**
     * @param endpoint  e.g. {@code https://<account-id>.r2.cloudflarestorage.com}; empty for AWS itself
     * @param region    {@code auto} for Cloudflare R2
     * @param bucket    where every upload goes
     * @param accessKey from the R2 API token (never committed: environment variable)
     * @param secretKey from the R2 API token (never committed: environment variable)
     */
    public record S3(String endpoint, String region, String bucket, String accessKey, String secretKey) {
    }
}
