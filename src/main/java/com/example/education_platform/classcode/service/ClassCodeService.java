package com.example.education_platform.classcode.service;

import com.example.education_platform.classcode.dto.request.CreateClassCodeRequest;
import com.example.education_platform.classcode.dto.response.ClassCodeResponse;
import java.util.List;

public interface ClassCodeService {

    /** Every code, active first, so the admin can read out the current one at a glance. */
    List<ClassCodeResponse> list();

    ClassCodeResponse create(CreateClassCodeRequest request);

    /**
     * Retires a code without deleting it. Students who already registered with it keep their
     * accounts — the code only ever gates the moment of registration.
     */
    ClassCodeResponse deactivate(Long id);
}
