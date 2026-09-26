package com.example.ssms.offering.controller;

import com.example.ssms.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/timetable")
@Tag(name = "Timetable", description = "Endpoints for school-wide schedule and conflict detection")
public class TimetableController {

	@GetMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "School-wide timetable.")
	public ResponseEntity<ApiResponse<List<Object>>> getTimetable(@RequestParam(required = false) UUID termId,
			@RequestParam(required = false) UUID teacherId, @RequestParam(required = false) UUID classId,
			@RequestParam(required = false) Integer dayOfWeek, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		return ResponseEntity.ok(ApiResponse.ok(List.of(), requestId));
	}

	@GetMapping("/conflicts")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Detect teacher and class overlaps in a term.")
	public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getConflicts(
			@RequestParam(required = false) UUID termId, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		return ResponseEntity.ok(ApiResponse.ok(List.of(), requestId));
	}
}
