package com.example.education_platform.dashboard.controller;

import com.example.education_platform.dashboard.dto.response.AdminDashboardResponse;
import com.example.education_platform.dashboard.dto.response.StudentDashboardResponse;
import com.example.education_platform.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Figures are cached for a minute, so a burst of refreshes costs one round of aggregates. */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Headline figures, the weekly chart and what is due next")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Section-wide figures, submissions per day and the next deadlines")
    AdminDashboardResponse admin() {
        return dashboardService.adminDashboard();
    }

    @GetMapping("/me")
    @Operation(summary = "My progress, my average and what I still owe")
    StudentDashboardResponse me() {
        return dashboardService.myDashboard();
    }
}
