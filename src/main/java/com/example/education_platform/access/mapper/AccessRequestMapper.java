package com.example.education_platform.access.mapper;

import com.example.education_platform.access.dto.response.AccessRequestResponse;
import com.example.education_platform.access.entity.AccessRequest;
import com.example.education_platform.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = UserMapper.class)
public interface AccessRequestMapper {

    @Mapping(target = "submittedAt", source = "createdAt")
    AccessRequestResponse toResponse(AccessRequest request);
}
