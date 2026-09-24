package com.example.education_platform.access.repository;

import com.example.education_platform.access.entity.AccessRequest;
import com.example.education_platform.access.entity.AccessRequestStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccessRequestRepository extends JpaRepository<AccessRequest, Long> {

    @EntityGraph(attributePaths = {"reviewedBy", "createdUser"})
    Optional<AccessRequest> findDetailById(Long id);

    /** Stops one address filing the same request repeatedly while it waits for an answer. */
    boolean existsByEmailIgnoreCaseAndStatus(String email, AccessRequestStatus status);

    @EntityGraph(attributePaths = {"reviewedBy", "createdUser"})
    @Query("""
            select r from AccessRequest r
            where :status is null or r.status = :status
            order by r.createdAt desc
            """)
    Page<AccessRequest> findByStatus(@Param("status") AccessRequestStatus status, Pageable pageable);
}
