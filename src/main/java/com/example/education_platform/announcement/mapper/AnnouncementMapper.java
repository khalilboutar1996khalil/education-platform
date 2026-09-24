package com.example.education_platform.announcement.mapper;

import com.example.education_platform.announcement.dto.response.AnnouncementResponse;
import com.example.education_platform.announcement.entity.Announcement;
import com.example.education_platform.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = UserMapper.class)
public interface AnnouncementMapper {

    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseCode", source = "course.code")
    AnnouncementResponse toResponse(Announcement announcement);
}
