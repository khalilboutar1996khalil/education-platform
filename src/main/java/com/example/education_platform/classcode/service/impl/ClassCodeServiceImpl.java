package com.example.education_platform.classcode.service.impl;

import com.example.education_platform.classcode.dto.request.CreateClassCodeRequest;
import com.example.education_platform.classcode.dto.response.ClassCodeResponse;
import com.example.education_platform.classcode.entity.ClassCode;
import com.example.education_platform.classcode.mapper.ClassCodeMapper;
import com.example.education_platform.classcode.repository.ClassCodeRepository;
import com.example.education_platform.classcode.service.ClassCodeService;
import com.example.education_platform.common.exception.ConflictException;
import com.example.education_platform.common.exception.ResourceNotFoundException;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClassCodeServiceImpl implements ClassCodeService {

    private final ClassCodeRepository classCodes;
    private final ClassCodeMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<ClassCodeResponse> list() {
        return classCodes.findAll().stream()
                .sorted(Comparator.comparing(ClassCode::isActive).reversed()
                        .thenComparing(ClassCode::getCode))
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ClassCodeResponse create(CreateClassCodeRequest request) {
        String code = ClassCode.normalise(request.code());
        if (classCodes.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("Ce code de classe existe déjà");
        }
        return mapper.toResponse(classCodes.save(new ClassCode(code, request.level(), request.label())));
    }

    @Override
    @Transactional
    public ClassCodeResponse deactivate(Long id) {
        ClassCode classCode = classCodes.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class code", id));
        classCode.deactivate();
        return mapper.toResponse(classCode);
    }
}
