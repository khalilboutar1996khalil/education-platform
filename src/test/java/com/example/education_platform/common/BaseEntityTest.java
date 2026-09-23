package com.example.education_platform.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BaseEntityTest {

    @Test
    void softDeleteMarksDeletedAtAndRestoreClearsIt() {
        var entity = new TestEntity();
        assertThat(entity.isDeleted()).isFalse();

        entity.softDelete();
        assertThat(entity.isDeleted()).isTrue();

        entity.restore();
        assertThat(entity.isDeleted()).isFalse();
    }

    private static final class TestEntity extends BaseEntity {
    }
}
