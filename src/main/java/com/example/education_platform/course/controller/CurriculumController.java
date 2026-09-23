package com.example.education_platform.course.controller;

import com.example.education_platform.course.dto.request.ChapterRequest;
import com.example.education_platform.course.dto.request.LessonRequest;
import com.example.education_platform.course.dto.request.MovePositionRequest;
import com.example.education_platform.course.dto.response.ChapterResponse;
import com.example.education_platform.course.dto.response.CourseDetailResponse;
import com.example.education_platform.course.service.CurriculumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Curriculum", description = "Chapters and lessons inside a module (admin)")
public class CurriculumController {

    private final CurriculumService curriculumService;

    @PostMapping("/courses/{courseId}/chapters")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a chapter; omit position to append")
    CourseDetailResponse addChapter(@PathVariable Long courseId, @Valid @RequestBody ChapterRequest request) {
        return curriculumService.addChapter(courseId, request);
    }

    @PutMapping("/chapters/{chapterId}")
    @Operation(summary = "Update a chapter, optionally moving it")
    CourseDetailResponse updateChapter(@PathVariable Long chapterId, @Valid @RequestBody ChapterRequest request) {
        return curriculumService.updateChapter(chapterId, request);
    }

    @PatchMapping("/chapters/{chapterId}/position")
    @Operation(summary = "Move a chapter; the siblings are renumbered")
    CourseDetailResponse moveChapter(@PathVariable Long chapterId, @Valid @RequestBody MovePositionRequest request) {
        return curriculumService.moveChapter(chapterId, request.position());
    }

    @DeleteMapping("/chapters/{chapterId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a chapter with its lessons")
    void deleteChapter(@PathVariable Long chapterId) {
        curriculumService.deleteChapter(chapterId);
    }

    @PostMapping("/chapters/{chapterId}/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a lesson; omit position to append")
    ChapterResponse addLesson(@PathVariable Long chapterId, @Valid @RequestBody LessonRequest request) {
        return curriculumService.addLesson(chapterId, request);
    }

    @PutMapping("/lessons/{lessonId}")
    @Operation(summary = "Update a lesson, optionally moving it")
    ChapterResponse updateLesson(@PathVariable Long lessonId, @Valid @RequestBody LessonRequest request) {
        return curriculumService.updateLesson(lessonId, request);
    }

    @PatchMapping("/lessons/{lessonId}/position")
    @Operation(summary = "Move a lesson within its chapter")
    ChapterResponse moveLesson(@PathVariable Long lessonId, @Valid @RequestBody MovePositionRequest request) {
        return curriculumService.moveLesson(lessonId, request.position());
    }

    @DeleteMapping("/lessons/{lessonId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a lesson")
    void deleteLesson(@PathVariable Long lessonId) {
        curriculumService.deleteLesson(lessonId);
    }
}
