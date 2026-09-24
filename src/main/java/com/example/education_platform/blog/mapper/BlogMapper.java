package com.example.education_platform.blog.mapper;

import com.example.education_platform.blog.dto.response.BlogCategoryResponse;
import com.example.education_platform.blog.dto.response.BlogPostResponse;
import com.example.education_platform.blog.entity.BlogCategory;
import com.example.education_platform.blog.entity.BlogPost;
import com.example.education_platform.storage.mapper.StoredFileMapper;
import com.example.education_platform.user.mapper.UserMapper;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(uses = {StoredFileMapper.class, UserMapper.class})
public interface BlogMapper {

    BlogPostResponse toResponse(BlogPost post);

    BlogCategoryResponse toCategory(BlogCategory category);

    List<BlogCategoryResponse> toCategories(List<BlogCategory> categories);
}
