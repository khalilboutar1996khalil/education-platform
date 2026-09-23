package com.example.education_platform.user.entity;

import com.example.education_platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Locale;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Admins (teachers) and students share this table; {@code role} separates them. A student's
 * course access is derived by matching {@code level} against the course's level — there is no
 * enrolment table, because every student of a level follows every module of that level.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** Only set for students; admins see every level. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Level level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(nullable = false, length = 5)
    private String locale = "fr";

    @Column(nullable = false)
    private boolean notifyByEmail = true;

    public User(String fullName, String email, String passwordHash, Role role, Level level) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.level = level;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /** Avatar initials shown throughout the UI, e.g. "Amira Benali" becomes "AB". */
    public String getInitials() {
        String[] parts = fullName.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase(Locale.ROOT);
    }
}
