package com.example.education_platform.blog.entity;

import com.example.education_platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A table rather than an enum: categories are content a teacher edits, not states the code
 * branches on, and adding one should not need a deployment.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "blog_categories")
public class BlogCategory extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    /** Hex accent used by the category pill in the UI. */
    @Column(length = 7)
    private String color;

    public BlogCategory(String name, String slug, String color) {
        this.name = name;
        this.slug = slug;
        this.color = color;
    }
}
