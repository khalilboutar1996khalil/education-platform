package com.example.education_platform.assignment.service.impl;

import com.example.education_platform.assignment.dto.request.GradeRequest;
import com.example.education_platform.assignment.dto.request.SubmissionDraftRequest;
import com.example.education_platform.assignment.dto.response.SubmissionResponse;
import com.example.education_platform.assignment.entity.Assignment;
import com.example.education_platform.assignment.entity.Submission;
import com.example.education_platform.assignment.entity.SubmissionStatus;
import com.example.education_platform.assignment.mapper.AssignmentMapper;
import com.example.education_platform.assignment.repository.AssignmentRepository;
import com.example.education_platform.assignment.repository.SubmissionRepository;
import com.example.education_platform.assignment.service.AssignmentService.DownloadableFile;
import com.example.education_platform.assignment.service.SubmissionService;
import com.example.education_platform.common.PageResponse;
import com.example.education_platform.common.exception.BusinessException;
import com.example.education_platform.common.exception.ResourceNotFoundException;
import com.example.education_platform.security.CurrentUser;
import com.example.education_platform.storage.entity.StoredFile;
import com.example.education_platform.storage.service.StorageService;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.repository.UserRepository;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
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
public class SubmissionServiceImpl implements SubmissionService {

    private static final EnumSet<SubmissionStatus> AWAITING_MARKING =
            EnumSet.of(SubmissionStatus.SUBMITTED, SubmissionStatus.LATE);

    private final SubmissionRepository submissions;
    private final AssignmentRepository assignments;
    private final UserRepository users;
    private final StorageService storage;
    private final AssignmentMapper assignmentMapper;
    private final CurrentUser currentUser;

    // ---------- the student's side ----------

    @Override
    public SubmissionResponse saveDraft(Long assignmentId, SubmissionDraftRequest request) {
        User me = requireStudent();
        Assignment assignment = openAssignment(assignmentId, me);
        Submission submission = editableSubmission(assignment, me);

        submission.setComment(request.comment());
        submission.setPartner(resolvePartner(assignment, me, request.partnerId()));
        submissions.flush();
        return assignmentMapper.toSubmission(submission);
    }

    @Override
    public SubmissionResponse addFile(Long assignmentId, MultipartFile file) {
        User me = requireStudent();
        Assignment assignment = openAssignment(assignmentId, me);
        Submission submission = editableSubmission(assignment, me);

        submission.getFiles().add(storage.store(file));
        submissions.flush();
        return assignmentMapper.toSubmission(submission);
    }

    @Override
    public SubmissionResponse removeFile(Long submissionId, Long fileId) {
        User me = requireStudent();
        Submission submission = mine(submissionId, me);
        if (!submission.isEditable()) {
            throw new BusinessException("A submission that has been handed in can no longer be changed");
        }

        StoredFile file = submission.getFiles().stream()
                .filter(candidate -> candidate.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("File", fileId));
        submission.getFiles().remove(file);
        storage.delete(file);
        submissions.flush();
        return assignmentMapper.toSubmission(submission);
    }

    @Override
    public SubmissionResponse submit(Long assignmentId) {
        User me = requireStudent();
        Assignment assignment = openAssignment(assignmentId, me);
        Submission submission = submissions.findMine(assignmentId, me.getId())
                .orElseThrow(() -> new BusinessException("There is nothing to hand in yet"));

        if (!submission.isEditable()) {
            throw new BusinessException("This work has already been handed in");
        }
        if (submission.getFiles().isEmpty()) {
            throw new BusinessException("Attach at least one file before handing in");
        }

        Instant now = Instant.now();
        submission.submit(now, assignment.isPastDeadline(now));
        submissions.flush();
        return assignmentMapper.toSubmission(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionResponse getMine(Long assignmentId) {
        User me = requireStudent();
        return assignmentMapper.toSubmission(submissions.findMine(assignmentId, me.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Submission for assignment", assignmentId)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubmissionResponse> listMine(Pageable pageable) {
        User me = requireStudent();
        Page<Submission> page = submissions.findAllMine(me.getId(), pageable);
        return PageResponse.from(page, map(page));
    }

    // ---------- the teacher's side ----------

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubmissionResponse> listForAssignment(Long assignmentId, boolean onlyPending,
                                                              Pageable pageable) {
        var statuses = onlyPending ? AWAITING_MARKING : EnumSet.allOf(SubmissionStatus.class);
        Page<Submission> page = submissions.findByAssignmentIdAndStatusIn(assignmentId, statuses, pageable);
        return PageResponse.from(page, map(page));
    }

    @Override
    public SubmissionResponse grade(Long submissionId, GradeRequest request) {
        Submission submission = submissions.findDetailById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", submissionId));

        if (submission.getStatus() == SubmissionStatus.DRAFT) {
            throw new BusinessException("This work has not been handed in yet");
        }
        if (request.grade().compareTo(submission.getAssignment().getMaxPoints()) > 0) {
            throw new BusinessException("The grade cannot exceed "
                    + submission.getAssignment().getMaxPoints() + " points");
        }

        submission.applyGrade(request.grade(), request.feedback(), currentUser.get(), Instant.now());
        submissions.flush();
        return assignmentMapper.toSubmission(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public DownloadableFile downloadFile(Long submissionId, Long fileId) {
        User me = currentUser.get();
        Submission submission = submissions.findDetailById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", submissionId));
        if (!me.isAdmin() && !submission.involves(me.getId())) {
            throw new AccessDeniedException("This submission belongs to another student");
        }

        StoredFile file = submission.getFiles().stream()
                .filter(candidate -> candidate.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("File", fileId));
        return new DownloadableFile(storage.loadAsResource(file), file.getOriginalFilename(),
                file.getContentType(), file.getSizeBytes());
    }

    // ---------- guards ----------

    private User requireStudent() {
        User me = currentUser.get();
        if (me.isAdmin()) {
            throw new AccessDeniedException("Admins do not hand in work");
        }
        return me;
    }

    private Assignment openAssignment(Long assignmentId, User student) {
        Assignment assignment = assignments.findDetailById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", assignmentId));
        if (assignment.getCourse().getLevel() != student.getLevel()) {
            throw new AccessDeniedException("This assignment belongs to another level");
        }
        if (!assignment.acceptsSubmissionsAt(Instant.now())) {
            throw new BusinessException("This assignment is no longer accepting work");
        }
        return assignment;
    }

    /** Finds the student's submission, creating the draft on first use. */
    private Submission editableSubmission(Assignment assignment, User student) {
        Submission submission = submissions.findMine(assignment.getId(), student.getId())
                .orElseGet(() -> submissions.save(new Submission(assignment, student, null)));
        if (!submission.isEditable()) {
            throw new BusinessException("A submission that has been handed in can no longer be changed");
        }
        return submission;
    }

    private Submission mine(Long submissionId, User student) {
        Submission submission = submissions.findDetailById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", submissionId));
        if (!submission.involves(student.getId())) {
            throw new AccessDeniedException("This submission belongs to another student");
        }
        return submission;
    }

    private User resolvePartner(Assignment assignment, User student, Long partnerId) {
        if (partnerId == null) {
            return null;
        }
        if (!assignment.isPairWork()) {
            throw new BusinessException("This assignment is individual work");
        }
        if (partnerId.equals(student.getId())) {
            throw new BusinessException("A student cannot be their own partner");
        }

        User partner = users.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", partnerId));
        if (partner.isAdmin() || partner.getLevel() != student.getLevel()) {
            throw new BusinessException("The partner must be a student of the same level");
        }
        // Otherwise one student could end up inside two submissions for the same assignment
        if (submissions.existsForUser(assignment.getId(), partnerId)) {
            throw new BusinessException("That student already has a submission for this assignment");
        }
        return partner;
    }

    private List<SubmissionResponse> map(Page<Submission> page) {
        return page.getContent().stream().map(assignmentMapper::toSubmission).toList();
    }
}
