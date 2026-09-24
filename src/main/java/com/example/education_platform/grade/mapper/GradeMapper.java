package com.example.education_platform.grade.mapper;

import com.example.education_platform.grade.dto.response.GradeResponse;
import com.example.education_platform.grade.entity.Grade;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface GradeMapper {

    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseCode", source = "course.code")
    @Mapping(target = "outOfTwenty", expression = "java(grade.outOfTwenty())")
    @Mapping(target = "recordedAt", source = "createdAt")
    GradeResponse toResponse(Grade grade);

    List<GradeResponse> toResponses(List<Grade> grades);
}
