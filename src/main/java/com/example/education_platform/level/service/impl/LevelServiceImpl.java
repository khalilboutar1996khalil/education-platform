package com.example.education_platform.level.service.impl;

import com.example.education_platform.common.config.CacheConfig;
import com.example.education_platform.common.exception.ConflictException;
import com.example.education_platform.common.exception.ResourceNotFoundException;
import com.example.education_platform.level.dto.request.CreateLevelRequest;
import com.example.education_platform.level.dto.request.UpdateLevelRequest;
import com.example.education_platform.level.dto.response.LevelResponse;
import com.example.education_platform.level.entity.SchoolLevel;
import com.example.education_platform.level.mapper.LevelMapper;
import com.example.education_platform.level.repository.SchoolLevelRepository;
import com.example.education_platform.level.service.LevelService;
import com.example.education_platform.user.entity.Level;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LevelServiceImpl implements LevelService {

    /** New levels go after the last one unless the admin says otherwise. */
    private static final int POSITION_STEP = 10;

    private final SchoolLevelRepository levels;
    private final LevelMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<LevelResponse> list(boolean includeInactive) {
        List<SchoolLevel> found = includeInactive
                ? levels.findAllByOrderByPositionAscCodeAsc()
                : levels.findByActiveTrueOrderByPositionAscCodeAsc();
        return found.stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.DASHBOARD_CACHE, allEntries = true)
    public LevelResponse create(CreateLevelRequest request) {
        String code = Level.normalise(request.code());
        if (levels.existsByCode(code)) {
            throw new ConflictException("Cette référence de niveau existe déjà");
        }
        int position = request.position() != null ? request.position() : nextPosition();
        return mapper.toResponse(levels.save(new SchoolLevel(code, request.name().trim(), position)));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.DASHBOARD_CACHE, allEntries = true)
    public LevelResponse update(Long id, UpdateLevelRequest request) {
        SchoolLevel level = find(id);
        level.setName(request.name().trim());
        level.setPosition(request.position());
        return mapper.toResponse(level);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.DASHBOARD_CACHE, allEntries = true)
    public LevelResponse setActive(Long id, boolean active) {
        SchoolLevel level = find(id);
        level.setActive(active);
        return mapper.toResponse(level);
    }

    private SchoolLevel find(Long id) {
        return levels.findById(id).orElseThrow(() -> new ResourceNotFoundException("Level", id));
    }

    private int nextPosition() {
        return levels.findAllByOrderByPositionAscCodeAsc().stream()
                .mapToInt(SchoolLevel::getPosition)
                .max()
                .orElse(0) + POSITION_STEP;
    }
}
