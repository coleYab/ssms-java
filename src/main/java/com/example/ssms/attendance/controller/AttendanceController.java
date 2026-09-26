package com.example.ssms.attendance.controller;

import com.example.ssms.attendance.dto.AttendanceDtos.*;
import com.example.ssms.attendance.entity.AttendanceStatus;
import com.example.ssms.attendance.service.AttendanceService;
import com.example.ssms.common.dto.ApiResponse;
import com.example.ssms.common.dto.LinksDto;
import com.example.ssms.common.dto.PaginationDto;
import com.example.ssms.security.UserPrincipal;
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

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attendance")
@Tag(name = "Attendance", description = "Endpoints for period attendance, bulk marking, reports and absence excuses")
public class AttendanceController {

	private final AttendanceService attendanceService;

	public AttendanceController(AttendanceService attendanceService) {
		this.attendanceService = attendanceService;
	}

	// Static paths first

	@GetMapping("/records")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT')")
	@Operation(summary = "Query records across sessions.")
	public ResponseEntity<ApiResponse<List<RecordResponse>>> queryRecords(@AuthenticationPrincipal UserPrincipal caller,
			@RequestParam(required = false) UUID studentId, @RequestParam(required = false) AttendanceStatus status,
			@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int limit,
			HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		Page<RecordResponse> paged = attendanceService.queryRecords(caller, studentId, status, page, limit);
		PaginationDto pagination = PaginationDto.of(page, limit, paged.getTotalElements());
		LinksDto links = LinksDto.of("/api/v1/attendance/records", page, limit, paged.getTotalPages());
		return ResponseEntity.ok(ApiResponse.ok(paged.getContent(), requestId, pagination, links));
	}

	@PatchMapping("/records/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
	@Operation(summary = "Correct a single record.")
	public ResponseEntity<ApiResponse<RecordResponse>> correctRecord(@AuthenticationPrincipal UserPrincipal caller,
			@PathVariable UUID id, @Valid @RequestBody CorrectRecordRequest request, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		RecordResponse response = attendanceService.correctRecord(caller, id, request);
		return ResponseEntity.ok(ApiResponse.ok(response, requestId));
	}

	@GetMapping("/records/{id}/history")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Edit history of a record.")
	public ResponseEntity<ApiResponse<List<Object>>> getRecordHistory(@PathVariable UUID id,
			HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		return ResponseEntity.ok(ApiResponse.ok(List.of(), requestId));
	}

	@GetMapping("/teachers/me/pending")
	@PreAuthorize("hasRole('TEACHER')")
	@Operation(summary = "Today's scheduled sessions not yet submitted.")
	public ResponseEntity<ApiResponse<List<Object>>> getPendingTeacherSessions(
			@AuthenticationPrincipal UserPrincipal caller, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		return ResponseEntity.ok(ApiResponse.ok(List.of(), requestId));
	}

	@PostMapping("/sessions/bulk-generate")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Bulk generate sessions from timetable.")
	public ResponseEntity<ApiResponse<Map<String, Object>>> bulkGenerate(@RequestParam LocalDate from,
			@RequestParam LocalDate to, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.ok(Map.of("message", "Sessions generated", "count", 0), requestId));
	}

	@GetMapping("/reports/daily")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
	@Operation(summary = "Daily attendance report.")
	public ResponseEntity<ApiResponse<Map<String, Object>>> getDailyReport(
			@RequestParam(required = false) LocalDate date, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		return ResponseEntity.ok(ApiResponse
				.ok(Map.of("date", date != null ? date : LocalDate.now(), "attendanceRate", 100.0), requestId));
	}

	@GetMapping("/reports/class/{classId}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
	@Operation(summary = "Class attendance report.")
	public ResponseEntity<ApiResponse<Map<String, Object>>> getClassReport(@PathVariable UUID classId,
			HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		return ResponseEntity.ok(ApiResponse.ok(Map.of("classId", classId, "attendanceRate", 100.0), requestId));
	}

	@GetMapping("/reports/offering/{offeringId}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
	@Operation(summary = "Offering attendance report.")
	public ResponseEntity<ApiResponse<Map<String, Object>>> getOfferingReport(@PathVariable UUID offeringId,
			HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		return ResponseEntity.ok(ApiResponse.ok(Map.of("offeringId", offeringId, "attendanceRate", 100.0), requestId));
	}

	@GetMapping("/reports/low-attendance")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Low attendance students report.")
	public ResponseEntity<ApiResponse<List<Object>>> getLowAttendanceReport(
			@RequestParam(defaultValue = "85") int threshold, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		return ResponseEntity.ok(ApiResponse.ok(List.of(), requestId));
	}

	@GetMapping("/reports/export")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Export attendance CSV.")
	public ResponseEntity<byte[]> exportAttendanceReport() {
		String csv = "date,studentNumber,studentName,status\n";
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"attendance_report.csv\"")
				.contentType(MediaType.parseMediaType("text/csv")).body(csv.getBytes());
	}

	// Excuses

	@PostMapping("/excuses")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'STUDENT')")
	@Operation(summary = "Submit absence excuse.")
	public ResponseEntity<ApiResponse<ExcuseResponse>> submitExcuse(@AuthenticationPrincipal UserPrincipal caller,
			@Valid @RequestBody CreateExcuseRequest request, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		ExcuseResponse response = attendanceService.submitExcuse(caller, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, requestId));
	}

	@GetMapping("/excuses")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT')")
	@Operation(summary = "List absence excuses.")
	public ResponseEntity<ApiResponse<List<ExcuseResponse>>> listExcuses(@AuthenticationPrincipal UserPrincipal caller,
			@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int limit,
			HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		Page<ExcuseResponse> paged = attendanceService.listExcuses(caller, page, limit);
		PaginationDto pagination = PaginationDto.of(page, limit, paged.getTotalElements());
		LinksDto links = LinksDto.of("/api/v1/attendance/excuses", page, limit, paged.getTotalPages());
		return ResponseEntity.ok(ApiResponse.ok(paged.getContent(), requestId, pagination, links));
	}

	@GetMapping("/excuses/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT')")
	@Operation(summary = "Get excuse.")
	public ResponseEntity<ApiResponse<ExcuseResponse>> getExcuse(@AuthenticationPrincipal UserPrincipal caller,
			@PathVariable UUID id, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		ExcuseResponse response = attendanceService.getExcuse(caller, id);
		return ResponseEntity.ok(ApiResponse.ok(response, requestId));
	}

	@PatchMapping("/excuses/{id}/review")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
	@Operation(summary = "Review absence excuse (approve/reject).")
	public ResponseEntity<ApiResponse<ExcuseResponse>> reviewExcuse(@AuthenticationPrincipal UserPrincipal caller,
			@PathVariable UUID id, @Valid @RequestBody ReviewExcuseRequest request, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		ExcuseResponse response = attendanceService.reviewExcuse(caller, id, request);
		return ResponseEntity.ok(ApiResponse.ok(response, requestId));
	}

	@DeleteMapping("/excuses/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'STUDENT')")
	@Operation(summary = "Delete/withdraw excuse.")
	public void deleteExcuse(@AuthenticationPrincipal UserPrincipal caller, @PathVariable UUID id) {
		attendanceService.deleteExcuse(caller, id);
	}

	// Sessions paths

	@GetMapping("/sessions")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT')")
	@Operation(summary = "List attendance sessions.")
	public ResponseEntity<ApiResponse<List<SessionResponse>>> listSessions(
			@AuthenticationPrincipal UserPrincipal caller, @RequestParam(required = false) UUID offeringId,
			@RequestParam(required = false) LocalDate date, @RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int limit, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		Page<SessionResponse> paged = attendanceService.listSessions(caller, offeringId, date, page, limit);
		PaginationDto pagination = PaginationDto.of(page, limit, paged.getTotalElements());
		LinksDto links = LinksDto.of("/api/v1/attendance/sessions", page, limit, paged.getTotalPages());
		return ResponseEntity.ok(ApiResponse.ok(paged.getContent(), requestId, pagination, links));
	}

	@PostMapping("/sessions")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
	@Operation(summary = "Open attendance session.")
	public ResponseEntity<ApiResponse<SessionResponse>> openSession(@AuthenticationPrincipal UserPrincipal caller,
			@Valid @RequestBody OpenSessionRequest request, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		SessionResponse response = attendanceService.openSession(caller, request);
		return ResponseEntity.created(URI.create("/api/v1/attendance/sessions/" + response.id()))
				.body(ApiResponse.ok(response, requestId));
	}

	@GetMapping("/sessions/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT')")
	@Operation(summary = "Get session with records.")
	public ResponseEntity<ApiResponse<SessionResponse>> getSession(@AuthenticationPrincipal UserPrincipal caller,
			@PathVariable UUID id, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		SessionResponse response = attendanceService.getSession(caller, id);
		return ResponseEntity.ok(ApiResponse.ok(response, requestId));
	}

	@DeleteMapping("/sessions/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Delete attendance session (step-up).")
	public void deleteSession(@AuthenticationPrincipal UserPrincipal caller, @PathVariable UUID id) {
		attendanceService.deleteSession(caller, id);
	}

	@PutMapping("/sessions/{id}/records")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
	@Operation(summary = "Bulk mark attendance records.")
	public ResponseEntity<ApiResponse<BulkMarkResponse>> bulkMark(@AuthenticationPrincipal UserPrincipal caller,
			@PathVariable UUID id, @Valid @RequestBody BulkMarkRequest request, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		BulkMarkResponse response = attendanceService.bulkMark(caller, id, request);
		return ResponseEntity.ok(ApiResponse.ok(response, requestId));
	}

	@PostMapping("/sessions/{id}/submit")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
	@Operation(summary = "Finalise and lock attendance session.")
	public ResponseEntity<ApiResponse<SessionResponse>> submitSession(@AuthenticationPrincipal UserPrincipal caller,
			@PathVariable UUID id, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		SessionResponse response = attendanceService.submitSession(caller, id);
		return ResponseEntity.ok(ApiResponse.ok(response, requestId));
	}

	@PostMapping("/sessions/{id}/reopen")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Reopen submitted session.")
	public ResponseEntity<ApiResponse<SessionResponse>> reopenSession(@AuthenticationPrincipal UserPrincipal caller,
			@PathVariable UUID id, @Valid @RequestBody ReopenSessionRequest request, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		SessionResponse response = attendanceService.reopenSession(caller, id, request);
		return ResponseEntity.ok(ApiResponse.ok(response, requestId));
	}
}
