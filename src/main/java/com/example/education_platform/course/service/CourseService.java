package com.example.education_platform.course.service;

import com.example.education_platform.common.PageResponse;
import com.example.education_platform.course.dto.request.CourseRequest;
import com.example.education_platform.course.dto.response.CourseDetailResponse;
import com.example.education_platform.course.dto.response.CourseSummaryResponse;
import com.example.education_platform.user.entity.Level;
import org.springframework.data.domain.Pageable;

public interface CourseService {

    /** A student always sees their own level; {@code level} only narrows an admin's view. */
    PageResponse<CourseSummaryResponse> list(Level level, Pageable pageable);

    CourseDetailResponse getDetail(Long id);

    CourseDetailResponse create(CourseRequest request);

    CourseDetailResponse update(Long id, CourseRequest request);

    void delete(Long id);
}
