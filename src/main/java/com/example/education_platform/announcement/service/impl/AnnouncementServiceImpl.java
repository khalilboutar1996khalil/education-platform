package com.example.education_platform.announcement.service.impl;

import com.example.education_platform.announcement.dto.request.AnnouncementRequest;
import com.example.education_platform.announcement.dto.response.AnnouncementResponse;
import com.example.education_platform.announcement.entity.Announcement;
import com.example.education_platform.announcement.event.AnnouncementPublished;
import com.example.education_platform.announcement.mapper.AnnouncementMapper;
import com.example.education_platform.announcement.repository.AnnouncementRepository;
import com.example.education_platform.announcement.service.AnnouncementService;
import com.example.education_platform.common.PageResponse;
import com.example.education_platform.common.exception.BusinessException;
import com.example.education_platform.common.exception.ResourceNotFoundException;
import com.example.education_platform.course.entity.Course;
import com.example.education_platform.course.repository.CourseRepository;
import com.example.education_platform.security.CurrentUser;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.entity.UserStatus;
import com.example.education_platform.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcements;
    private final CourseRepository courses;
    private final UserRepository users;
    private final AnnouncementMapper announcementMapper;
    private final CurrentUser currentUser;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AnnouncementResponse> list(Long courseId, Pageable pageable) {
        User me = currentUser.get();
        Page<Announcement> page = announcements.findVisible(
                me.isAdmin() ? null : me.getLevel(), courseId, pageable);
        List<AnnouncementResponse> content = page.getContent().stream()
                .map(announcementMapper::toResponse).toList();
        return PageResponse.from(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public AnnouncementResponse getById(Long id) {
        Announcement announcement = announcement(id);
        User me = currentUser.get();
        if (!announcement.isVisibleTo(me.isAdmin() ? null : me.getLevel())) {
            throw new AccessDeniedException("This announcement is not addressed to you");
        }
        return announcementMapper.toResponse(announcement);
    }

    @Override
    public AnnouncementResponse create(AnnouncementRequest request) {
        Announcement announcement = new Announcement(request.title(), request.body(),
                currentUser.get(), courseOf(request.courseId()), request.level());
        announcement.setPinned(request.pinnedOrDefault());
        Announcement saved = announcements.save(announcement);
        announcements.flush();
        return announcementMapper.toResponse(saved);
    }

    @Override
    public AnnouncementResponse update(Long id, AnnouncementRequest request) {
        Announcement announcement = announcement(id);
        // Rewriting a notice people have already read would rewrite history, not fix it
        if (announcement.isPublished()) {
            throw new BusinessException("A published announcement can no longer be edited");
        }
        announcement.setTitle(request.title());
        announcement.setBody(request.body());
        announcement.setCourse(courseOf(request.courseId()));
        announcement.setLevel(request.level());
        announcement.setPinned(request.pinnedOrDefault());
        return announcementMapper.toResponse(announcement);
    }

    @Override
    public AnnouncementResponse publish(Long id) {
        Announcement announcement = announcement(id);
        if (announcement.isPublished()) {
            throw new BusinessException("This announcement is already published");
        }
        long recipients = users.countStudents(Role.STUDENT, UserStatus.ACTIVE, announcement.targetLevel());
        announcement.publish(Instant.now(), Math.toIntExact(recipients));
        announcements.flush();
        // Notifications listen for this; this feature does not know they exist
        events.publishEvent(new AnnouncementPublished(announcement.getId()));
        return announcementMapper.toResponse(announcement);
    }

    @Override
    public void delete(Long id) {
        announcements.delete(announcement(id));
    }

    private Course courseOf(Long courseId) {
        if (courseId == null) {
            return null;
        }
        return courses.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
    }

    private Announcement announcement(Long id) {
        return announcements.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", id));
    }
}
