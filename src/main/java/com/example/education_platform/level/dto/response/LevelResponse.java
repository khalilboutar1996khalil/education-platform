package com.example.education_platform.level.dto.response;

public record LevelResponse(
        Long id,
        String code,
        String name,
        int position,
        boolean active) {
}
