package com.example.education_platform.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseTest {

    @Test
    void fromMapsSpringPageToStableShape() {
        var page = new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 2), 5);

        PageResponse<String> response = PageResponse.from(page);

        assertThat(response.content()).containsExactly("a", "b");
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.last()).isFalse();
    }

    @Test
    void fromWithMappedContentKeepsPageMetadataButSwapsContent() {
        var entityPage = new PageImpl<>(List.of(1, 2), PageRequest.of(0, 2), 2);

        PageResponse<String> response = PageResponse.from(entityPage, List.of("one", "two"));

        assertThat(response.content()).containsExactly("one", "two");
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.last()).isTrue();
    }
}
