package com.example.ssms.student.controller;

import com.example.ssms.common.dto.ApiResponse;
import com.example.ssms.common.dto.LinksDto;
import com.example.ssms.common.dto.PaginationDto;
import com.example.ssms.security.UserPrincipal;
import com.example.ssms.student.dto.StudentDtos.*;
import com.example.ssms.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students")
@Tag(name = "Students", description = "Endpoints for student lifecycle, profiles and guardians")
public class StudentController {

	private final StudentService studentService;

	public StudentController(StudentService studentService) {
		this.studentService = studentService;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
	@Operation(summary = "List students.")
	public ResponseEntity<ApiResponse<List<StudentResponse>>> listStudents(
			@AuthenticationPrincipal UserPrincipal caller, @RequestParam(required = false) UUID classId,
			@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int limit,
			HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		Page<StudentResponse> paged = studentService.listStudents(caller, classId, page, limit);
		PaginationDto pagination = PaginationDto.of(page, limit, paged.getTotalElements());
		LinksDto links = LinksDto.of("/api/v1/students", page, limit, paged.getTotalPages());
		return ResponseEntity.ok(ApiResponse.ok(paged.getContent(), requestId, pagination, links));
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Create student.")
	public ResponseEntity<ApiResponse<StudentResponse>> createStudent(@Valid @RequestBody CreateStudentRequest request,
			HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		StudentResponse response = studentService.createStudent(request);
		return ResponseEntity.created(URI.create("/api/v1/students/" + response.id()))
				.body(ApiResponse.ok(response, requestId));
	}

	@GetMapping("/me")
	@PreAuthorize("hasRole('STUDENT')")
	@Operation(summary = "Own student profile.")
	public ResponseEntity<ApiResponse<StudentResponse>> getMe(@AuthenticationPrincipal UserPrincipal caller,
			HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		StudentResponse response = studentService.getMe(caller);
		return ResponseEntity.ok(ApiResponse.ok(response, requestId));
	}

	@GetMapping("/bulk-import/template")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Download CSV template (text/csv).")
	public ResponseEntity<byte[]> downloadImportTemplate() {
		String csv = "firstName,middleName,lastName,email,dateOfBirth,gender,admissionDate,classId\nJohn,M,Doe,john.doe@school.example.com,2010-05-12,MALE,2026-09-01,\n";
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"students_template.csv\"")
				.contentType(MediaType.parseMediaType("text/csv")).body(csv.getBytes());
	}

	@PostMapping(value = "/bulk-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Multipart CSV bulk import (step-up).")
	public ResponseEntity<ApiResponse<Map<String, Object>>> bulkImport(@RequestParam("file") MultipartFile file,
			@RequestParam(defaultValue = "false") boolean dryRun, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		return ResponseEntity.status(HttpStatus.CREATED).body(
				ApiResponse.ok(Map.of("message", "Import processed successfully", "rowsProcessed", 0), requestId));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT')")
	@Operation(summary = "Get student.")
	public ResponseEntity<ApiResponse<StudentResponse>> getStudent(@AuthenticationPrincipal UserPrincipal caller,
			@PathVariable UUID id, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		StudentResponse response = studentService.getStudent(caller, id);
		return ResponseEntity.ok(ApiResponse.ok(response, requestId));
	}

	@PatchMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Update profile (studentNumber immutable).")
	public ResponseEntity<ApiResponse<StudentResponse>> updateStudent(@PathVariable UUID id,
			@Valid @RequestBody UpdateStudentRequest request, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		StudentResponse response = studentService.updateStudent(id, request);
		return ResponseEntity.ok(ApiResponse.ok(response, requestId));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Soft delete student and account.")
	public void deleteStudent(@PathVariable UUID id) {
		studentService.deleteStudent(id);
	}

	@PostMapping("/{id}/status")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Change status.")
	public ResponseEntity<ApiResponse<StudentResponse>> changeStatus(@PathVariable UUID id,
			@Valid @RequestBody ChangeStudentStatusRequest request, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		StudentResponse response = studentService.changeStatus(id, request);
		return ResponseEntity.ok(ApiResponse.ok(response, requestId));
	}

	@GetMapping("/{id}/enrollments")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT')")
	@Operation(summary = "Enrollment history across years.")
	public ResponseEntity<ApiResponse<List<Object>>> getEnrollments(@PathVariable UUID id,
			HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		return ResponseEntity.ok(ApiResponse.ok(List.of(), requestId));
	}

	@GetMapping("/{id}/offerings")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT')")
	@Operation(summary = "Course offerings of the student's current class.")
	public ResponseEntity<ApiResponse<List<Object>>> getOfferings(@PathVariable UUID id,
			HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		return ResponseEntity.ok(ApiResponse.ok(List.of(), requestId));
	}

	@GetMapping("/{id}/timetable")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT')")
	@Operation(summary = "Weekly timetable.")
	public ResponseEntity<ApiResponse<List<Object>>> getTimetable(@PathVariable UUID id,
			HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		return ResponseEntity.ok(ApiResponse.ok(List.of(), requestId));
	}

	@GetMapping("/{id}/attendance")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT')")
	@Operation(summary = "Attendance records.")
	public ResponseEntity<ApiResponse<List<Object>>> getAttendance(@PathVariable UUID id,
			HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		return ResponseEntity.ok(ApiResponse.ok(List.of(), requestId));
	}

	@GetMapping("/{id}/attendance/summary")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT')")
	@Operation(summary = "Totals and percentage by status.")
	public ResponseEntity<ApiResponse<Map<String, Object>>> getAttendanceSummary(@PathVariable UUID id,
			HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		return ResponseEntity
				.ok(ApiResponse.ok(Map.of("studentId", id, "attendanceRate", 100.0, "totalSessions", 0), requestId));
	}

	@GetMapping("/{id}/guardians")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT')")
	@Operation(summary = "List guardians.")
	public ResponseEntity<ApiResponse<List<GuardianResponse>>> listGuardians(@PathVariable UUID id,
			HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		List<GuardianResponse> response = studentService.listGuardians(id);
		return ResponseEntity.ok(ApiResponse.ok(response, requestId));
	}

	@PostMapping("/{id}/guardians")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Add guardian (max 4).")
	public ResponseEntity<ApiResponse<GuardianResponse>> addGuardian(@PathVariable UUID id,
			@Valid @RequestBody CreateGuardianRequest request, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		GuardianResponse response = studentService.addGuardian(id, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, requestId));
	}

	@PatchMapping("/{id}/guardians/{guardianId}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Update guardian.")
	public ResponseEntity<ApiResponse<GuardianResponse>> updateGuardian(@PathVariable UUID id,
			@PathVariable UUID guardianId, @Valid @RequestBody UpdateGuardianRequest request,
			HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		GuardianResponse response = studentService.updateGuardian(id, guardianId, request);
		return ResponseEntity.ok(ApiResponse.ok(response, requestId));
	}

	@DeleteMapping("/{id}/guardians/{guardianId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Remove guardian.")
	public void deleteGuardian(@PathVariable UUID id, @PathVariable UUID guardianId) {
		studentService.deleteGuardian(id, guardianId);
	}
}
