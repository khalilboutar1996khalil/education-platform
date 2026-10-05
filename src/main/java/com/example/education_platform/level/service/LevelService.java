package com.example.education_platform.level.service;

import com.example.education_platform.level.dto.request.CreateLevelRequest;
import com.example.education_platform.level.dto.request.UpdateLevelRequest;
import com.example.education_platform.level.dto.response.LevelResponse;
import java.util.List;

public interface LevelService {

    /** An admin sees every level; anyone else (the registration form included) only active ones. */
    List<LevelResponse> list(boolean includeInactive);

    LevelResponse create(CreateLevelRequest request);

    LevelResponse update(Long id, UpdateLevelRequest request);

    /**
     * Closes a level to new students, modules and codes. Nothing already attached to it changes,
     * which is why there is no delete: students and modules keep pointing at it.
     */
    LevelResponse setActive(Long id, boolean active);
}
