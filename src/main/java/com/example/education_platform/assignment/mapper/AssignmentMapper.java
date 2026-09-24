package com.example.education_platform.assignment.mapper;

import com.example.education_platform.assignment.dto.response.AssignmentDetailResponse;
import com.example.education_platform.assignment.dto.response.AssignmentSummaryResponse;
import com.example.education_platform.assignment.dto.response.SubmissionResponse;
import com.example.education_platform.assignment.entity.Assignment;
import com.example.education_platform.assignment.entity.Submission;
import com.example.education_platform.assignment.entity.SubmissionStatus;
import com.example.education_platform.storage.mapper.StoredFileMapper;
import com.example.education_platform.user.mapper.UserMapper;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = {StoredFileMapper.class, UserMapper.class})
public interface AssignmentMapper {

    @Mapping(target = "courseId", source = "assignment.course.id")
    @Mapping(target = "courseCode", source = "assignment.course.code")
    AssignmentSummaryResponse toSummary(Assignment assignment, boolean acceptingSubmissions,
                                        SubmissionStatus mySubmissionStatus, BigDecimal myGrade);

    @Mapping(target = "courseId", source = "assignment.course.id")
    @Mapping(target = "courseCode", source = "assignment.course.code")
    AssignmentDetailResponse toDetail(Assignment assignment, boolean acceptingSubmissions);

    @Mapping(target = "assignmentId", source = "assignment.id")
    @Mapping(target = "assignmentTitle", source = "assignment.title")
    SubmissionResponse toSubmission(Submission submission);
}
