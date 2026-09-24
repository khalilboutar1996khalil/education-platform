package com.example.education_platform.notification.service.impl;

import com.example.education_platform.common.PageResponse;
import com.example.education_platform.common.exception.ResourceNotFoundException;
import com.example.education_platform.course.entity.Course;
import com.example.education_platform.notification.dto.response.ActivityResponse;
import com.example.education_platform.notification.dto.response.NotificationResponse;
import com.example.education_platform.notification.entity.ActivityEvent;
import com.example.education_platform.notification.entity.ActivityType;
import com.example.education_platform.notification.entity.Notification;
import com.example.education_platform.notification.entity.NotificationType;
import com.example.education_platform.notification.mapper.NotificationMapper;
import com.example.education_platform.notification.repository.ActivityEventRepository;
import com.example.education_platform.notification.repository.NotificationRepository;
import com.example.education_platform.notification.service.NotificationService;
import com.example.education_platform.security.CurrentUser;
import com.example.education_platform.user.entity.User;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notifications;
    private final ActivityEventRepository activity;
    private final NotificationMapper notificationMapper;
    private final CurrentUser currentUser;

    // ---------- writing ----------

    @Override
    public void notify(User recipient, NotificationType type, String title, String body, String link) {
        notifications.save(new Notification(recipient, type, title, body, link));
    }

    @Override
    public void notifyAll(Collection<User> recipients, NotificationType type, String title,
                          String body, String link) {
        List<Notification> batch = recipients.stream()
                .map(recipient -> new Notification(recipient, type, title, body, link))
                .toList();
        notifications.saveAll(batch);
    }

    @Override
    public void record(User actor, ActivityType type, String summary, String subjectType,
                       Long subjectId, Course course) {
        activity.save(new ActivityEvent(actor, type, summary, subjectType, subjectId,
                course, Instant.now()));
    }

    // ---------- reading ----------

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> mine(boolean onlyUnread, Pageable pageable) {
        Long me = currentUser.get().getId();
        Page<Notification> page = onlyUnread
                ? notifications.findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(me, pageable)
                : notifications.findByRecipientIdOrderByCreatedAtDesc(me, pageable);
        return PageResponse.from(page,
                page.getContent().stream().map(notificationMapper::toResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount() {
        return notifications.countByRecipientIdAndReadAtIsNull(currentUser.get().getId());
    }

    @Override
    public NotificationResponse markRead(Long id) {
        Notification notification = notifications.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        if (!notification.getRecipient().getId().equals(currentUser.get().getId())) {
            throw new AccessDeniedException("This notification is addressed to somebody else");
        }
        notification.markRead(Instant.now());
        return notificationMapper.toResponse(notification);
    }

    @Override
    public int markAllRead() {
        return notifications.markAllRead(currentUser.get().getId(), Instant.now());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ActivityResponse> feed(Pageable pageable) {
        User me = currentUser.get();
        Page<ActivityEvent> page = activity.findFeed(me.isAdmin() ? null : me.getLevel(), pageable);
        return PageResponse.from(page,
                page.getContent().stream().map(notificationMapper::toActivity).toList());
    }
}
