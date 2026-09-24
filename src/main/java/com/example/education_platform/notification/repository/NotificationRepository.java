package com.example.education_platform.notification.repository;

import com.example.education_platform.notification.entity.Notification;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    /**
     * "Mark everything read" in one statement rather than a row at a time. The flags matter
     * because a bulk update otherwise leaves loaded rows reporting themselves unread.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Notification n set n.readAt = :now where n.recipient.id = :userId and n.readAt is null")
    int markAllRead(@Param("userId") Long userId, @Param("now") Instant now);
}
