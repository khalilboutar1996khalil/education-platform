package com.example.education_platform.storage;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * @param location            root directory files are written under
 * @param maxFileSize         rejected above this, before anything touches the disk
 * @param allowedContentTypes an allow-list, never a deny-list: anything unlisted is refused
 */
@ConfigurationProperties("app.storage")
public record StorageProperties(String location, DataSize maxFileSize, List<String> allowedContentTypes) {
}
