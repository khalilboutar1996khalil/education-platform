package com.example.education_platform.classcode.controller;

import com.example.education_platform.classcode.dto.request.CreateClassCodeRequest;
import com.example.education_platform.classcode.dto.response.ClassCodeResponse;
import com.example.education_platform.classcode.service.ClassCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin only: these responses contain the codes in clear, which is what students register with. */
@RestController
@RequestMapping("/api/v1/class-codes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Class codes", description = "The codes students present to register themselves")
public class ClassCodeController {

    private final ClassCodeService classCodeService;

    @GetMapping
    @Operation(summary = "List every class code, active ones first")
    List<ClassCodeResponse> list() {
        return classCodeService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a class code, typically to rotate at the start of a school year")
    ClassCodeResponse create(@Valid @RequestBody CreateClassCodeRequest request) {
        return classCodeService.create(request);
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Retire a code; accounts already registered with it are unaffected")
    ClassCodeResponse deactivate(@PathVariable Long id) {
        return classCodeService.deactivate(id);
    }
}
