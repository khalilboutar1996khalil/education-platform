package com.example.education_platform.dashboard.service;

import com.example.education_platform.dashboard.dto.response.AdminDashboardResponse;
import com.example.education_platform.dashboard.dto.response.StudentDashboardResponse;

public interface DashboardService {

    AdminDashboardResponse adminDashboard();

    StudentDashboardResponse myDashboard();
}
