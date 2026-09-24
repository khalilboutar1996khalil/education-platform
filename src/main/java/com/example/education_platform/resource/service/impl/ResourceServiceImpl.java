package com.example.education_platform.resource.service.impl;

import com.example.education_platform.common.PageResponse;
import com.example.education_platform.common.exception.BusinessException;
import com.example.education_platform.common.exception.ResourceNotFoundException;
import com.example.education_platform.course.entity.Course;
import com.example.education_platform.course.repository.CourseRepository;
import com.example.education_platform.resource.dto.request.ResourceRequest;
import com.example.education_platform.resource.dto.response.ResourceResponse;
import com.example.education_platform.resource.entity.Resource;
import com.example.education_platform.resource.entity.ResourceType;
import com.example.education_platform.resource.mapper.ResourceMapper;
import com.example.education_platform.resource.repository.ResourceRepository;
import com.example.education_platform.resource.service.ResourceService;
import com.example.education_platform.security.CurrentUser;
import com.example.education_platform.storage.entity.StoredFile;
import com.example.education_platform.storage.service.DownloadableFile;
import com.example.education_platform.storage.service.StorageService;
import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.User;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resources;
    private final CourseRepository courses;
    private final StorageService storage;
    private final ResourceMapper resourceMapper;
    private final CurrentUser currentUser;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ResourceResponse> list(Long courseId, ResourceType type, String search,
                                               Pageable pageable) {
        User me = currentUser.get();
        Level level = me.isAdmin() ? null : me.getLevel();
        Page<Resource> page = resources.findVisible(level, courseId, type, likePattern(search), pageable);
        List<ResourceResponse> content = page.getContent().stream()
                .map(resourceMapper::toResponse).toList();
        return PageResponse.from(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceResponse getById(Long id) {
        return resourceMapper.toResponse(requireVisible(resource(id)));
    }

    @Override
    public ResourceResponse create(ResourceRequest request, MultipartFile file) {
        validateSource(request, file);

        Resource resource = new Resource(request.title(), request.description(), request.type(),
                courseOf(request.courseId()), request.level());
        if (request.type() == ResourceType.LINK) {
            resource.setExternalUrl(request.externalUrl());
        } else {
            resource.setFile(storage.store(file));
        }
        Resource saved = resources.save(resource);
        resources.flush();
        return resourceMapper.toResponse(saved);
    }

    /** Metadata only: replacing the bytes means a new resource, not an edit of this one. */
    @Override
    public ResourceResponse update(Long id, ResourceRequest request) {
        Resource resource = resource(id);
        if (request.type() != resource.getType()) {
            throw new BusinessException("The type cannot change; create a new resource instead");
        }
        resource.setTitle(request.title());
        resource.setDescription(request.description());
        resource.setCourse(courseOf(request.courseId()));
        resource.setLevel(request.level());
        if (resource.isLink()) {
            requireLink(request.externalUrl());
            resource.setExternalUrl(request.externalUrl());
        }
        return resourceMapper.toResponse(resource);
    }

    @Override
    public void delete(Long id) {
        Resource resource = resource(id);
        StoredFile file = resource.getFile();
        resources.delete(resource);
        resources.flush();
        if (file != null) {
            // Nothing cascades into stored_files, so the bytes would otherwise be left behind
            storage.delete(file);
        }
    }

    @Override
    public DownloadableFile download(Long id) {
        Resource resource = requireVisible(resource(id));
        if (resource.isLink()) {
            throw new BusinessException("This resource is a link; open its externalUrl instead");
        }
        // Read the metadata first: the counter's bulk update detaches everything behind it
        StoredFile file = resource.getFile();
        DownloadableFile download = new DownloadableFile(storage.loadAsResource(file),
                file.getOriginalFilename(), file.getContentType(), file.getSizeBytes());

        resources.incrementDownloadCount(id);
        return download;
    }

    // ---------- helpers ----------

    private static void validateSource(ResourceRequest request, MultipartFile file) {
        if (request.type() == ResourceType.LINK) {
            requireLink(request.externalUrl());
            if (file != null && !file.isEmpty()) {
                throw new BusinessException("A link resource carries no file");
            }
            return;
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException("This resource type needs a file");
        }
        if (request.externalUrl() != null && !request.externalUrl().isBlank()) {
            throw new BusinessException("Only a link resource carries an external URL");
        }
    }

    private static void requireLink(String externalUrl) {
        if (externalUrl == null || externalUrl.isBlank()) {
            throw new BusinessException("A link resource needs an external URL");
        }
        String lower = externalUrl.toLowerCase(Locale.ROOT);
        // Anything but http(s) in a link the platform hands out is an invitation to trouble
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            throw new BusinessException("An external URL must start with http:// or https://");
        }
    }

    private Course courseOf(Long courseId) {
        if (courseId == null) {
            return null;
        }
        return courses.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
    }

    private Resource resource(Long id) {
        return resources.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", id));
    }

    private Resource requireVisible(Resource resource) {
        User me = currentUser.get();
        if (!resource.isVisibleTo(me.isAdmin() ? null : me.getLevel())) {
            throw new AccessDeniedException("This resource is not shared with your level");
        }
        return resource;
    }

    private static String likePattern(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
