package com.example.education_platform.classcode.mapper;

import com.example.education_platform.classcode.dto.response.ClassCodeResponse;
import com.example.education_platform.classcode.entity.ClassCode;
import org.mapstruct.Mapper;

@Mapper
public interface ClassCodeMapper {

    ClassCodeResponse toResponse(ClassCode classCode);
}
