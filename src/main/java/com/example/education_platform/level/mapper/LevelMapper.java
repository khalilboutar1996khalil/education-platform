package com.example.education_platform.level.mapper;

import com.example.education_platform.level.dto.response.LevelResponse;
import com.example.education_platform.level.entity.SchoolLevel;
import org.mapstruct.Mapper;

@Mapper
public interface LevelMapper {

    LevelResponse toResponse(SchoolLevel level);
}
