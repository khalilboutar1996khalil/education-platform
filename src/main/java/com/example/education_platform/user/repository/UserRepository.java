package com.example.education_platform.user.repository;

import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.entity.UserStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Every filter is optional; a null one drops out of the predicate rather than matching nothing. */
    @Query("""
            select u from User u
            where (:role is null or u.role = :role)
              and (:level is null or u.level = :level)
              and (:status is null or u.status = :status)
              and (:search is null
                   or lower(u.fullName) like :search
                   or lower(u.email) like :search)
            """)
    Page<User> search(@Param("role") Role role,
                      @Param("level") Level level,
                      @Param("status") UserStatus status,
                      @Param("search") String search,
                      Pageable pageable);
}
