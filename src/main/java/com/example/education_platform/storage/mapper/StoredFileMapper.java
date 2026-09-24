package com.example.education_platform.storage.mapper;

import com.example.education_platform.storage.dto.response.StoredFileResponse;
import com.example.education_platform.storage.entity.StoredFile;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper
public interface StoredFileMapper {

    StoredFileResponse toResponse(StoredFile file);

    List<StoredFileResponse> toResponses(Iterable<StoredFile> files);
}
