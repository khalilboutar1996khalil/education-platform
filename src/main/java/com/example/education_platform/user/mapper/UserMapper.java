package com.example.education_platform.user.mapper;

import com.example.education_platform.user.dto.response.UserResponse;
import com.example.education_platform.user.entity.User;
import org.mapstruct.Mapper;

/**
 * The build runs MapStruct with unmappedTargetPolicy=ERROR, so adding a field to
 * {@link UserResponse} without mapping it breaks the build instead of shipping a null.
 */
@Mapper
public interface UserMapper {

    UserResponse toResponse(User user);
}
