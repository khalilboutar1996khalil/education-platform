package com.example.education_platform.storage.job;

import com.example.education_platform.storage.entity.StoredFile;
import com.example.education_platform.storage.repository.StoredFileRepository;
import com.example.education_platform.storage.service.StorageService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Removes files nothing points at any more.
 *
 * <p>Nothing cascades into {@code stored_files} by design — owners point at files, not the other
 * way round — so deleting an assignment or a submission leaves its bytes behind. This is the
 * sweeper that debt always implied.
 *
 * <p>Only files older than a grace period are considered, because a file uploaded seconds ago may
 * simply not have been attached to anything yet.
 */
@Component
@RequiredArgsConstructor
public class OrphanedFileSweeper {

    private static final Logger LOG = LoggerFactory.getLogger(OrphanedFileSweeper.class);
    private static final Duration GRACE = Duration.ofHours(6);

    private final StoredFileRepository storedFiles;
    private final StorageService storage;

    /** Nightly: an orphan costs disk, not correctness, so there is no hurry. */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void run() {
        int swept = sweep(Instant.now());
        if (swept > 0) {
            LOG.info("Orphaned file sweeper: removed {} files", swept);
        }
    }

    public int sweep(Instant now) {
        List<StoredFile> orphans = storedFiles.findOrphansOlderThan(now.minus(GRACE));
        for (StoredFile orphan : orphans) {
            storage.delete(orphan);
            storedFiles.delete(orphan);
        }
        return orphans.size();
    }
}
