package com.example.ssms.schoolclass.controller;

import com.example.ssms.common.dto.ApiResponse;
import com.example.ssms.common.dto.LinksDto;
import com.example.ssms.common.dto.PaginationDto;
import com.example.ssms.schoolclass.dto.ClassDtos.*;
import com.example.ssms.schoolclass.service.ClassService;
import com.example.ssms.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/classes")
@Tag(name = "Classes", description = "Endpoints for class cohorts, enrollment and rosters")
public class ClassController {

    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT')")
    @Operation(summary = "List classes.")
    public ResponseEntity<ApiResponse<List<ClassResponse>>> listClasses(
            @AuthenticationPrincipal UserPrincipal caller,
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(required = false) UUID gradeLevelId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        Page<ClassResponse> paged = classService.listClasses(caller, academicYearId, gradeLevelId, page, limit);
        PaginationDto pagination = PaginationDto.of(page, limit, paged.getTotalElements());
        LinksDto links = LinksDto.of("/api/v1/classes", page, limit, paged.getTotalPages());
        return ResponseEntity.ok(ApiResponse.ok(paged.getContent(), requestId, pagination, links));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Create class.")
    public ResponseEntity<ApiResponse<ClassResponse>> createClass(
            @Valid @RequestBody CreateClassRequest request,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        ClassResponse response = classService.createClass(request);
        return ResponseEntity
                .created(URI.create("/api/v1/classes/" + response.id()))
                .body(ApiResponse.ok(response, requestId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT')")
    @Operation(summary = "Get class.")
    public ResponseEntity<ApiResponse<ClassResponse>> getClass(
            @AuthenticationPrincipal UserPrincipal caller,
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        ClassResponse response = classService.getClass(caller, id);
        return ResponseEntity.ok(ApiResponse.ok(response, requestId));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Update class.")
    public ResponseEntity<ApiResponse<ClassResponse>> updateClass(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateClassRequest request,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        ClassResponse response = classService.updateClass(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, requestId));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Delete class.")
    public void deleteClass(@PathVariable UUID id) {
        classService.deleteClass(id);
    }

    @GetMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    @Operation(summary = "Roster of enrolled students.")
    public ResponseEntity<ApiResponse<List<Object>>> getRoster(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        return ResponseEntity.ok(ApiResponse.ok(List.of(), requestId));
    }

    @PostMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Enroll one or many students (max 100).")
    public ResponseEntity<ApiResponse<BulkEnrollmentResult>> enrollStudents(
            @PathVariable UUID id,
            @Valid @RequestBody EnrollStudentsRequest request,
            @RequestParam(defaultValue = "false") boolean partial,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        BulkEnrollmentResult result = classService.enrollStudents(id, request.studentIds(), partial);
        HttpStatus status = partial && !result.failed().isEmpty() ? HttpStatus.MULTI_STATUS : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(ApiResponse.ok(result, requestId));
    }

    @DeleteMapping("/{id}/students/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Un-enroll student.")
    public void unenrollStudent(
            @PathVariable UUID id,
            @PathVariable UUID studentId
    ) {
        classService.unenrollStudent(id, studentId);
    }

    @PostMapping("/{id}/students/transfer")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Move students to another class.")
    public ResponseEntity<ApiResponse<Map<String, String>>> transferStudents(
            @PathVariable UUID id,
            @Valid @RequestBody TransferStudentsRequest request,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        classService.transferStudents(id, request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Students transferred successfully."), requestId));
    }

    @PutMapping("/{id}/homeroom-teacher")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Assign homeroom teacher.")
    public ResponseEntity<ApiResponse<ClassResponse>> assignHomeroomTeacher(
            @PathVariable UUID id,
            @Valid @RequestBody AssignHomeroomTeacherRequest request,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        ClassResponse response = classService.assignHomeroomTeacher(id, request.teacherId());
        return ResponseEntity.ok(ApiResponse.ok(response, requestId));
    }

    @DeleteMapping("/{id}/homeroom-teacher")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Clear homeroom teacher.")
    public void clearHomeroomTeacher(@PathVariable UUID id) {
        classService.clearHomeroomTeacher(id);
    }

    @GetMapping("/{id}/offerings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT')")
    @Operation(summary = "Offerings of the class.")
    public ResponseEntity<ApiResponse<List<Object>>> getOfferings(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        return ResponseEntity.ok(ApiResponse.ok(List.of(), requestId));
    }

    @GetMapping("/{id}/timetable")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT')")
    @Operation(summary = "Weekly timetable.")
    public ResponseEntity<ApiResponse<List<Object>>> getTimetable(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        return ResponseEntity.ok(ApiResponse.ok(List.of(), requestId));
    }

    @GetMapping("/{id}/attendance/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    @Operation(summary = "Attendance rates for the class.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAttendanceSummary(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        return ResponseEntity.ok(ApiResponse.ok(Map.of("classId", id, "attendanceRate", 100.0), requestId));
    }

    @PostMapping("/{id}/promote")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Year-end promotion (step-up).")
    public ResponseEntity<ApiResponse<BulkEnrollmentResult>> promoteClass(
            @AuthenticationPrincipal UserPrincipal caller,
            @PathVariable UUID id,
            @Valid @RequestBody PromoteClassRequest request,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        BulkEnrollmentResult result = classService.promoteClass(caller, id, request);
        return ResponseEntity.ok(ApiResponse.ok(result, requestId));
    }
}
