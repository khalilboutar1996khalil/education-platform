package com.example.education_platform.dashboard.dto.response;

import com.example.education_platform.user.entity.Level;

/** One slice of a per-level breakdown. Every active level is present, with a zero when empty. */
public record LevelCountResponse(Level level, String name, long count) {
}
