package com.example.education_platform.notification.service;

import com.example.education_platform.common.PageResponse;
import com.example.education_platform.course.entity.Course;
import com.example.education_platform.notification.dto.response.ActivityResponse;
import com.example.education_platform.notification.dto.response.NotificationResponse;
import com.example.education_platform.notification.entity.ActivityType;
import com.example.education_platform.notification.entity.NotificationType;
import com.example.education_platform.user.entity.User;
import java.util.Collection;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    // --- writing, called only by listeners ---

    void notify(User recipient, NotificationType type, String title, String body, String link);

    void notifyAll(Collection<User> recipients, NotificationType type, String title,
                   String body, String link);

    void record(User actor, ActivityType type, String summary, String subjectType,
                Long subjectId, Course course);

    // --- reading ---

    PageResponse<NotificationResponse> mine(boolean onlyUnread, Pageable pageable);

    long unreadCount();

    NotificationResponse markRead(Long id);

    /** Returns how many were still unread, so the client can update its badge in one call. */
    int markAllRead();

    /** The dashboard feed: a student sees their level, an admin sees everything. */
    PageResponse<ActivityResponse> feed(Pageable pageable);
}
