package com.example.education_platform.notification.controller;

import com.example.education_platform.common.PageResponse;
import com.example.education_platform.notification.dto.response.ActivityResponse;
import com.example.education_platform.notification.dto.response.NotificationResponse;
import com.example.education_platform.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Nothing here creates a notification: they are written by listeners, never by a request. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "My notifications and the activity feed")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/my/notifications")
    @Operation(summary = "My notifications, newest first")
    PageResponse<NotificationResponse> mine(
            @RequestParam(defaultValue = "false") boolean onlyUnread,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return notificationService.mine(onlyUnread, pageable);
    }

    @GetMapping("/my/notifications/unread-count")
    @Operation(summary = "How many are unread; this drives the badge")
    Map<String, Long> unreadCount() {
        return Map.of("unread", notificationService.unreadCount());
    }

    @PostMapping("/my/notifications/{id}/read")
    @Operation(summary = "Mark one as read")
    NotificationResponse markRead(@PathVariable Long id) {
        return notificationService.markRead(id);
    }

    @PostMapping("/my/notifications/read-all")
    @Operation(summary = "Mark everything read; returns how many were still unread")
    Map<String, Integer> markAllRead() {
        return Map.of("markedRead", notificationService.markAllRead());
    }

    @GetMapping("/activity")
    @Operation(summary = "Recent activity; a student sees their own level")
    PageResponse<ActivityResponse> feed(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return notificationService.feed(pageable);
    }
}
