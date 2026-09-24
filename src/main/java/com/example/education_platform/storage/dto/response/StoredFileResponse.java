package com.example.education_platform.storage.dto.response;

/** File metadata. The storage key never leaves the service layer — it is an internal address. */
public record StoredFileResponse(Long id, String originalFilename, String contentType, long sizeBytes) {
}
