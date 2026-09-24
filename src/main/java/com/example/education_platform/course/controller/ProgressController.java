package com.example.education_platform.course.controller;

import com.example.education_platform.course.service.ProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** PUT rather than POST: marking a lesson done is idempotent, not a new resource each time. */
@RestController
@RequestMapping("/api/v1/lessons/{lessonId}/completion")
@RequiredArgsConstructor
@Tag(name = "Progress", description = "A student marking lessons as finished")
public class ProgressController {

    private final ProgressService progressService;

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Mark a lesson as finished")
    void markDone(@PathVariable Long lessonId) {
        progressService.markDone(lessonId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Mark a lesson as unfinished again")
    void unmarkDone(@PathVariable Long lessonId) {
        progressService.unmarkDone(lessonId);
    }
}
