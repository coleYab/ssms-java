package com.example.ssms.offering.controller;

import com.example.ssms.common.dto.ApiResponse;
import com.example.ssms.common.dto.LinksDto;
import com.example.ssms.common.dto.PaginationDto;
import com.example.ssms.offering.dto.OfferingDtos.*;
import com.example.ssms.offering.service.OfferingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/course-offerings")
@Tag(name = "Course Offerings", description = "Endpoints for course offerings and scheduling")
public class CourseOfferingController {

	private final OfferingService offeringService;

	public CourseOfferingController(OfferingService offeringService) {
		this.offeringService = offeringService;
	}

	@GetMapping
	@Operation(summary = "List course offerings.")
	public ResponseEntity<ApiResponse<List<OfferingResponse>>> listOfferings(@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int limit, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		Page<OfferingResponse> paged = offeringService.listOfferings(page, limit);
		PaginationDto pagination = PaginationDto.of(page, limit, paged.getTotalElements());
		LinksDto links = LinksDto.of("/api/v1/course-offerings", page, limit, paged.getTotalPages());
		return ResponseEntity.ok(ApiResponse.ok(paged.getContent(), requestId, pagination, links));
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Create offering.")
	public ResponseEntity<ApiResponse<OfferingResponse>> createOffering(
			@Valid @RequestBody CreateOfferingRequest request, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		OfferingResponse response = offeringService.createOffering(request);
		return ResponseEntity.created(URI.create("/api/v1/course-offerings/" + response.id()))
				.body(ApiResponse.ok(response, requestId));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get offering.")
	public ResponseEntity<ApiResponse<OfferingResponse>> getOffering(@PathVariable UUID id,
			HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		OfferingResponse response = offeringService.getOffering(id);
		return ResponseEntity.ok(ApiResponse.ok(response, requestId));
	}

	@PatchMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Update weekly periods or status.")
	public ResponseEntity<ApiResponse<OfferingResponse>> updateOffering(@PathVariable UUID id,
			@Valid @RequestBody UpdateOfferingRequest request, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		OfferingResponse response = offeringService.updateOffering(id, request);
		return ResponseEntity.ok(ApiResponse.ok(response, requestId));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Delete or cancel offering.")
	public void deleteOffering(@PathVariable UUID id) {
		offeringService.deleteOffering(id);
	}

	@GetMapping("/{id}/students")
	@Operation(summary = "Students enrolled via the class.")
	public ResponseEntity<ApiResponse<List<Object>>> getStudents(@PathVariable UUID id,
			HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		return ResponseEntity.ok(ApiResponse.ok(List.of(), requestId));
	}

	@GetMapping("/{id}/schedule")
	@Operation(summary = "Weekly slots.")
	public ResponseEntity<ApiResponse<List<ScheduleSlotDto>>> getSchedule(@PathVariable UUID id,
			HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		List<ScheduleSlotDto> response = offeringService.getSchedule(id);
		return ResponseEntity.ok(ApiResponse.ok(response, requestId));
	}

	@PutMapping("/{id}/schedule")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Replace the weekly slots.")
	public ResponseEntity<ApiResponse<List<ScheduleSlotDto>>> replaceSchedule(@PathVariable UUID id,
			@Valid @RequestBody ReplaceScheduleRequest request, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		List<ScheduleSlotDto> response = offeringService.replaceSchedule(id, request);
		return ResponseEntity.ok(ApiResponse.ok(response, requestId));
	}

	@PostMapping("/{id}/reassign-teacher")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Move offering to another teacher.")
	public ResponseEntity<ApiResponse<OfferingResponse>> reassignTeacher(@PathVariable UUID id,
			@Valid @RequestBody ReassignTeacherRequest request, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		OfferingResponse response = offeringService.reassignTeacher(id, request);
		return ResponseEntity.ok(ApiResponse.ok(response, requestId));
	}
}
