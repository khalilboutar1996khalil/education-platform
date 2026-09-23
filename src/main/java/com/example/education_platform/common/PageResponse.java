package com.example.education_platform.common;

import java.util.List;
import org.springframework.data.domain.Page;

/** Stable pagination shape returned by every list endpoint, independent of Spring's {@link Page}. */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return from(page, page.getContent());
    }

    /** Use when the page's content was mapped to a different type (e.g. entity -&gt; DTO). */
    public static <T, R> PageResponse<R> from(Page<T> page, List<R> mappedContent) {
        return new PageResponse<>(
                mappedContent,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }
}
