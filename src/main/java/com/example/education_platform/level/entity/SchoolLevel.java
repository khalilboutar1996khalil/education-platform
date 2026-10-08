package com.example.education_platform.level.entity;

import com.example.education_platform.common.BaseEntity;
import com.example.education_platform.user.entity.Level;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A class year students belong to and modules are written for, e.g. "7ᵉ année de base informatique".
 * The admin creates them; {@code code} is the reference every other table stores and never changes.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "school_levels")
public class SchoolLevel extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20, updatable = false)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean active = true;

    public SchoolLevel(String code, String name, int position) {
        this.code = Level.normalise(code);
        this.name = name;
        this.position = position;
    }

    public Level level() {
        return new Level(code);
    }
}
