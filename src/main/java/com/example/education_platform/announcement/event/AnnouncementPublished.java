package com.example.education_platform.announcement.event;

/** Raised when a notice goes out. Notifications listen; this feature does not know they exist. */
public record AnnouncementPublished(Long announcementId) {
}
