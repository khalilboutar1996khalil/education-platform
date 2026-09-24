package com.example.education_platform.resource.mapper;

import com.example.education_platform.resource.dto.response.ResourceResponse;
import com.example.education_platform.resource.entity.Resource;
import com.example.education_platform.storage.mapper.StoredFileMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = StoredFileMapper.class)
public interface ResourceMapper {

    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseCode", source = "course.code")
    ResourceResponse toResponse(Resource resource);
}
