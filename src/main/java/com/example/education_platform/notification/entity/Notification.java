package com.example.education_platform.notification.entity;

import com.example.education_platform.common.BaseEntity;
import com.example.education_platform.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Addressed to one person and read once. Written by listeners, never by a controller. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String body;

    /** In-app path the client opens on click, e.g. {@code /assignments/12}. */
    @Column(length = 500)
    private String link;

    /** Null means unread; the badge counts these. */
    private Instant readAt;

    public Notification(User recipient, NotificationType type, String title, String body, String link) {
        this.recipient = recipient;
        this.type = type;
        this.title = title;
        this.body = body;
        this.link = link;
    }

    public boolean isRead() {
        return readAt != null;
    }

    public void markRead(Instant now) {
        if (readAt == null) {
            this.readAt = now;
        }
    }
}
