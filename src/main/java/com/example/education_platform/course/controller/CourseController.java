package com.example.education_platform.course.controller;

import com.example.education_platform.common.PageResponse;
import com.example.education_platform.course.dto.request.CourseRequest;
import com.example.education_platform.course.dto.response.CourseDetailResponse;
import com.example.education_platform.course.dto.response.CourseSummaryResponse;
import com.example.education_platform.course.service.CourseService;
import com.example.education_platform.user.entity.Level;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Modules", description = "Teaching modules, their chapters and lessons")
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    @Operation(summary = "List modules; a student only ever sees their own level")
    PageResponse<CourseSummaryResponse> list(
            @RequestParam(required = false) Level level,
            @ParameterObject @PageableDefault(size = 20, sort = "code", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return courseService.list(level, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "One module with its chapters and lessons, flagged with what the student finished")
    CourseDetailResponse getDetail(@PathVariable Long id) {
        return courseService.getDetail(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a module")
    CourseDetailResponse create(@Valid @RequestBody CourseRequest request) {
        return courseService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a module")
    CourseDetailResponse update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return courseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a module with its chapters and lessons")
    void delete(@PathVariable Long id) {
        courseService.delete(id);
    }
}
