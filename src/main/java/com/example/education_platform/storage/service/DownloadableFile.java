package com.example.education_platform.storage.service;

import org.springframework.core.io.Resource;

/**
 * A file on its way to a client, with the metadata needed to build the response. Lives here rather
 * than inside one feature because assignments, submissions and the resource library all serve
 * downloads the same way.
 */
public record DownloadableFile(Resource resource, String filename, String contentType, long sizeBytes) {
}
