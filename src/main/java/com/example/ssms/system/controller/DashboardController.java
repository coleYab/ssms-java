package com.example.ssms.system.controller;

import com.example.ssms.common.dto.ApiResponse;
import com.example.ssms.security.UserPrincipal;
import com.example.ssms.system.service.SystemService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

	private final SystemService systemService;

	public DashboardController(SystemService systemService) {
		this.systemService = systemService;
	}

	@GetMapping("/stats")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Dashboard stats for admin.")
	public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminStats(HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		return ResponseEntity.ok(ApiResponse.ok(systemService.getAdminDashboardStats(), requestId));
	}

	@GetMapping("/teacher")
	@PreAuthorize("hasRole('TEACHER')")
	@Operation(summary = "Teacher dashboard overview.")
	public ResponseEntity<ApiResponse<Map<String, Object>>> getTeacherStats(
			@AuthenticationPrincipal UserPrincipal principal, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		return ResponseEntity.ok(ApiResponse.ok(systemService.getTeacherDashboardStats(principal.getId()), requestId));
	}

	@GetMapping("/student")
	@PreAuthorize("hasRole('STUDENT')")
	@Operation(summary = "Student dashboard overview.")
	public ResponseEntity<ApiResponse<Map<String, Object>>> getStudentStats(
			@AuthenticationPrincipal UserPrincipal principal, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		return ResponseEntity.ok(ApiResponse.ok(systemService.getStudentDashboardStats(principal.getId()), requestId));
	}
}
