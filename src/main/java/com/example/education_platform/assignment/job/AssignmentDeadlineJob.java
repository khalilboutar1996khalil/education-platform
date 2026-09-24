package com.example.education_platform.assignment.job;

import com.example.education_platform.assignment.entity.Assignment;
import com.example.education_platform.assignment.entity.AssignmentStatus;
import com.example.education_platform.assignment.repository.AssignmentRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Closes assignments once their deadline has passed, unless late work is explicitly allowed. */
@Component
@RequiredArgsConstructor
public class AssignmentDeadlineJob {

    private static final Logger LOG = LoggerFactory.getLogger(AssignmentDeadlineJob.class);

    private final AssignmentRepository assignments;

    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT2M")
    @Transactional
    public void run() {
        int closed = closeOverdue(Instant.now());
        if (closed > 0) {
            LOG.info("Assignment deadline job: closed {} assignments", closed);
        }
    }

    public int closeOverdue(Instant now) {
        List<Assignment> overdue = assignments.findOverdue(AssignmentStatus.OPEN, now);
        // allowLate means the deadline marks lateness rather than shutting the door
        List<Assignment> toClose = overdue.stream().filter(a -> !a.isAllowLate()).toList();
        toClose.forEach(assignment -> assignment.setStatus(AssignmentStatus.CLOSED));
        return toClose.size();
    }
}
