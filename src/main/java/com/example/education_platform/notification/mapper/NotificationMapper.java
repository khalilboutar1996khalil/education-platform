package com.example.education_platform.notification.mapper;

import com.example.education_platform.notification.dto.response.ActivityResponse;
import com.example.education_platform.notification.dto.response.NotificationResponse;
import com.example.education_platform.notification.entity.ActivityEvent;
import com.example.education_platform.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface NotificationMapper {

    @Mapping(target = "read", expression = "java(notification.isRead())")
    NotificationResponse toResponse(Notification notification);

    @Mapping(target = "actorName", source = "actor.fullName")
    @Mapping(target = "courseCode", source = "course.code")
    ActivityResponse toActivity(ActivityEvent event);
}
