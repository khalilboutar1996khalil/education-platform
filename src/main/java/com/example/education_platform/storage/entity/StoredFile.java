package com.example.education_platform.storage.entity;

import com.example.education_platform.common.BaseEntity;
import com.example.education_platform.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One uploaded file, for the whole platform. It holds no link back to whatever it is attached to;
 * owners point at it instead, so a new kind of attachment never changes this table.
 *
 * <p>{@code storageKey} is generated here and is the only thing used to build a path. The client's
 * own filename is kept as metadata and never reaches the filesystem.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stored_files")
public class StoredFile extends BaseEntity {

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false, unique = true, length = 200)
    private String storageKey;

    /** SHA-256, for integrity checks and for spotting a re-upload of the same file. */
    @Column(nullable = false, length = 64)
    private String checksum;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_id")
    private User uploadedBy;

    public StoredFile(String originalFilename, String contentType, long sizeBytes,
                      String storageKey, String checksum, User uploadedBy) {
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
        this.checksum = checksum;
        this.uploadedBy = uploadedBy;
    }
}
