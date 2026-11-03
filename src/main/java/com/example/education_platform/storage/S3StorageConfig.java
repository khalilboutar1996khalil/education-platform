package com.example.education_platform.storage;

import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * The object-store client, created only when {@code app.storage.type=s3}. Works with Supabase Storage
 * (endpoint {@code https://<project>.storage.supabase.co/storage/v1/s3}) and Cloudflare R2.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "s3")
public class S3StorageConfig {

    @Bean(destroyMethod = "close")
    S3Client s3Client(StorageProperties properties) {
        StorageProperties.S3 s3 = properties.s3();
        if (s3 == null || !StringUtils.hasText(s3.bucket()) || !StringUtils.hasText(s3.accessKey())
                || !StringUtils.hasText(s3.secretKey())) {
            throw new IllegalStateException("app.storage.type=s3 needs S3_BUCKET, S3_ACCESS_KEY and S3_SECRET_KEY");
        }
        var builder = S3Client.builder()
                .region(Region.of(StringUtils.hasText(s3.region()) ? s3.region() : "auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3.accessKey(), s3.secretKey())))
                // R2 and most S3-compatible stores expect bucket-in-path addressing
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                // Recent SDKs add CRC checksums to every request by default; S3-compatible stores
                // (Supabase Storage, R2) do not all accept them, so only send them when required
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED);
        if (StringUtils.hasText(s3.endpoint())) {
            builder.endpointOverride(URI.create(s3.endpoint()));
        }
        return builder.build();
    }
}
