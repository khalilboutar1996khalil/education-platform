package com.example.education_platform.level.controller;

import com.example.education_platform.level.dto.request.CreateLevelRequest;
import com.example.education_platform.level.dto.request.UpdateLevelRequest;
import com.example.education_platform.level.dto.response.LevelResponse;
import com.example.education_platform.level.service.LevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/levels")
@RequiredArgsConstructor
@Tag(name = "Levels", description = "The class years students belong to and modules are written for")
public class LevelController {

    private final LevelService levelService;

    /** Public, because the registration and access-request forms need the list before any login. */
    @GetMapping
    @Operation(summary = "List levels: active ones for everybody, all of them for an admin")
    List<LevelResponse> list() {
        return levelService.list(isAdmin());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a level, e.g. code 1AS, name \"1ʳᵉ année secondaire\"")
    LevelResponse create(@Valid @RequestBody CreateLevelRequest request) {
        return levelService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rename or reorder a level; its reference never changes")
    LevelResponse update(@PathVariable Long id, @Valid @RequestBody UpdateLevelRequest request) {
        return levelService.update(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Close a level to new students, modules and codes; existing ones are untouched")
    LevelResponse deactivate(@PathVariable Long id) {
        return levelService.setActive(id, false);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reopen a level")
    LevelResponse activate(@PathVariable Long id) {
        return levelService.setActive(id, true);
    }

    private static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
