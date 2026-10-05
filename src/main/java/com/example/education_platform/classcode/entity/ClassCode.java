package com.example.education_platform.classcode.entity;

import com.example.education_platform.common.BaseEntity;
import com.example.education_platform.user.entity.Level;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Locale;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The shared secret a teacher gives their class so students can register themselves without an
 * approval step. The code carries the level, so a student never chooses which year's material
 * they get — holding the 2ᵉ AS code cannot open 3ᵉ AS content.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "class_codes")
public class ClassCode extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 20)
    private Level level;

    /** Free text for the admin list, e.g. "2ᵉ AS informatique — 2026". */
    private String label;

    @Column(nullable = false)
    private boolean active = true;

    public ClassCode(String code, Level level, String label) {
        this.code = normalise(code);
        this.level = level;
        this.label = label;
    }

    public void deactivate() {
        this.active = false;
    }

    /** Codes are dictated out loud and typed by hand, so case and stray spaces cannot matter. */
    public static String normalise(String raw) {
        return raw == null ? null : raw.trim().toUpperCase(Locale.ROOT);
    }
}
