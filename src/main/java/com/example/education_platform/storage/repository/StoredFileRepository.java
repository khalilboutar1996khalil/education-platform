package com.example.education_platform.storage.repository;

import com.example.education_platform.storage.entity.StoredFile;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {

    /**
     * Files nothing points at. Every owner is checked explicitly, which is the cost of files being
     * a leaf that owners reference rather than a table that knows who owns it.
     */
    @Query("""
            select f from StoredFile f
            where f.createdAt < :before
              and not exists (select 1 from Assignment a where a.brief = f)
              and not exists (select 1 from Submission s join s.files sf where sf = f)
              and not exists (select 1 from Resource r where r.file = f)
              and not exists (select 1 from BlogPost p where p.cover = f)
            """)
    List<StoredFile> findOrphansOlderThan(@Param("before") Instant before);
}
