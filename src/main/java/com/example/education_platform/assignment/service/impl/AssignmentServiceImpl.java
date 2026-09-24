package com.example.education_platform.assignment.service.impl;

import com.example.education_platform.assignment.dto.request.AssignmentRequest;
import com.example.education_platform.assignment.dto.response.AssignmentDetailResponse;
import com.example.education_platform.assignment.dto.response.AssignmentSummaryResponse;
import com.example.education_platform.assignment.entity.Assignment;
import com.example.education_platform.assignment.entity.AssignmentStatus;
import com.example.education_platform.assignment.entity.Submission;
import com.example.education_platform.assignment.mapper.AssignmentMapper;
import com.example.education_platform.assignment.repository.AssignmentRepository;
import com.example.education_platform.assignment.repository.SubmissionRepository;
import com.example.education_platform.assignment.service.AssignmentService;
import com.example.education_platform.common.PageResponse;
import com.example.education_platform.common.exception.BusinessException;
import com.example.education_platform.common.exception.ResourceNotFoundException;
import com.example.education_platform.course.entity.Course;
import com.example.education_platform.course.repository.CourseRepository;
import com.example.education_platform.security.CurrentUser;
import com.example.education_platform.storage.entity.StoredFile;
import com.example.education_platform.storage.service.StorageService;
import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.User;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class AssignmentServiceImpl implements AssignmentService {

    /** A draft is invisible to students; closed work stays readable so grades can be reviewed. */
    private static final EnumSet<AssignmentStatus> VISIBLE_TO_STUDENTS =
            EnumSet.of(AssignmentStatus.OPEN, AssignmentStatus.CLOSED);

    private final AssignmentRepository assignments;
    private final SubmissionRepository submissions;
    private final CourseRepository courses;
    private final StorageService storage;
    private final AssignmentMapper assignmentMapper;
    private final CurrentUser currentUser;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AssignmentSummaryResponse> list(Long courseId, Pageable pageable) {
        User me = currentUser.get();
        Level level = me.isAdmin() ? null : me.getLevel();
        var statuses = me.isAdmin() ? EnumSet.allOf(AssignmentStatus.class) : VISIBLE_TO_STUDENTS;
        Page<Assignment> page = assignments.findVisible(level, courseId, statuses, pageable);

        Instant now = Instant.now();
        List<AssignmentSummaryResponse> content = page.getContent().stream()
                .map(assignment -> toSummary(assignment, me, now))
                .toList();
        return PageResponse.from(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentDetailResponse getDetail(Long id) {
        Assignment assignment = requireVisible(assignment(id));
        return assignmentMapper.toDetail(assignment, assignment.acceptsSubmissionsAt(Instant.now()));
    }

    @Override
    public AssignmentDetailResponse create(Long courseId, AssignmentRequest request) {
        Course course = courses.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
        Assignment assignment = new Assignment(course, request.title(), request.type(), request.mode());
        apply(assignment, request);
        Assignment saved = assignments.save(assignment);
        assignments.flush();
        return assignmentMapper.toDetail(saved, saved.acceptsSubmissionsAt(Instant.now()));
    }

    @Override
    public AssignmentDetailResponse update(Long id, AssignmentRequest request) {
        Assignment assignment = assignment(id);
        assignment.setTitle(request.title());
        assignment.setType(request.type());
        // Changing the mode once people have handed in would orphan the partners already recorded
        if (assignment.getMode() != request.mode() && submissions.existsByAssignmentId(id)) {
            throw new BusinessException("The work mode cannot change once submissions exist");
        }
        assignment.setMode(request.mode());
        apply(assignment, request);
        return assignmentMapper.toDetail(assignment, assignment.acceptsSubmissionsAt(Instant.now()));
    }

    @Override
    public AssignmentDetailResponse changeStatus(Long id, AssignmentStatus status) {
        Assignment assignment = assignment(id);
        assignment.setStatus(status);
        return assignmentMapper.toDetail(assignment, assignment.acceptsSubmissionsAt(Instant.now()));
    }

    @Override
    public AssignmentDetailResponse attachBrief(Long id, MultipartFile file) {
        Assignment assignment = assignment(id);
        StoredFile previous = assignment.getBrief();
        assignment.setBrief(storage.store(file));
        if (previous != null) {
            // Replacing the sheet should not leave the old bytes lying around
            storage.delete(previous);
        }
        assignments.flush();
        return assignmentMapper.toDetail(assignment, assignment.acceptsSubmissionsAt(Instant.now()));
    }

    @Override
    public void delete(Long id) {
        assignments.delete(assignment(id));
    }

    @Override
    @Transactional(readOnly = true)
    public DownloadableFile downloadBrief(Long id) {
        Assignment assignment = requireVisible(assignment(id));
        StoredFile brief = assignment.getBrief();
        if (brief == null) {
            throw new ResourceNotFoundException("Assignment brief", id);
        }
        return new DownloadableFile(storage.loadAsResource(brief), brief.getOriginalFilename(),
                brief.getContentType(), brief.getSizeBytes());
    }

    // ---------- helpers ----------

    private AssignmentSummaryResponse toSummary(Assignment assignment, User me, Instant now) {
        Optional<Submission> mine = me.isAdmin()
                ? Optional.empty()
                : submissions.findMine(assignment.getId(), me.getId());
        return assignmentMapper.toSummary(assignment, assignment.acceptsSubmissionsAt(now),
                mine.map(Submission::getStatus).orElse(null),
                mine.map(Submission::getGrade).orElse(null));
    }

    private static void apply(Assignment assignment, AssignmentRequest request) {
        assignment.setInstructions(request.instructions());
        assignment.setDeadline(request.deadline());
        assignment.setMaxPoints(request.maxPoints());
        assignment.setAllowLate(request.allowLateOrDefault());
    }

    private Assignment assignment(Long id) {
        return assignments.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", id));
    }

    /** Students never see drafts, and never work belonging to another level. */
    private Assignment requireVisible(Assignment assignment) {
        User me = currentUser.get();
        if (me.isAdmin()) {
            return assignment;
        }
        if (assignment.getCourse().getLevel() != me.getLevel()) {
            throw new AccessDeniedException("This assignment belongs to another level");
        }
        if (!VISIBLE_TO_STUDENTS.contains(assignment.getStatus())) {
            throw new AccessDeniedException("This assignment is not published");
        }
        return assignment;
    }
}
