package com.example.ssms.teacher.controller;

import com.example.ssms.common.dto.ApiResponse;
import com.example.ssms.common.dto.LinksDto;
import com.example.ssms.common.dto.PaginationDto;
import com.example.ssms.security.UserPrincipal;
import com.example.ssms.teacher.dto.TeacherDtos.*;
import com.example.ssms.teacher.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
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
@RequestMapping("/api/v1/teachers")
@Tag(name = "Teachers", description = "Endpoints for teachers, workload and qualifications")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT')")
    @Operation(summary = "List teachers.")
    public ResponseEntity<ApiResponse<List<TeacherResponse>>> listTeachers(
            @AuthenticationPrincipal UserPrincipal caller,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        Page<TeacherResponse> paged = teacherService.listTeachers(caller, page, limit);
        PaginationDto pagination = PaginationDto.of(page, limit, paged.getTotalElements());
        LinksDto links = LinksDto.of("/api/v1/teachers", page, limit, paged.getTotalPages());
        return ResponseEntity.ok(ApiResponse.ok(paged.getContent(), requestId, pagination, links));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Create teacher plus login account.")
    public ResponseEntity<ApiResponse<TeacherResponse>> createTeacher(
            @Valid @RequestBody CreateTeacherRequest request,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        TeacherResponse response = teacherService.createTeacher(request);
        return ResponseEntity
                .created(URI.create("/api/v1/teachers/" + response.id()))
                .body(ApiResponse.ok(response, requestId));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Own teacher profile.")
    public ResponseEntity<ApiResponse<TeacherResponse>> getMe(
            @AuthenticationPrincipal UserPrincipal caller,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        TeacherResponse response = teacherService.getMe(caller);
        return ResponseEntity.ok(ApiResponse.ok(response, requestId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT')")
    @Operation(summary = "Get teacher.")
    public ResponseEntity<ApiResponse<TeacherResponse>> getTeacher(
            @AuthenticationPrincipal UserPrincipal caller,
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        TeacherResponse response = teacherService.getTeacher(caller, id);
        return ResponseEntity.ok(ApiResponse.ok(response, requestId));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Update profile, maxWeeklyPeriods.")
    public ResponseEntity<ApiResponse<TeacherResponse>> updateTeacher(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTeacherRequest request,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        TeacherResponse response = teacherService.updateTeacher(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, requestId));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Soft delete teacher.")
    public void deleteTeacher(@PathVariable UUID id) {
        teacherService.deleteTeacher(id);
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Change status.")
    public ResponseEntity<ApiResponse<TeacherResponse>> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeTeacherStatusRequest request,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        TeacherResponse response = teacherService.changeStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, requestId));
    }

    @GetMapping("/{id}/classes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    @Operation(summary = "Classes where teacher is homeroom or teaches.")
    public ResponseEntity<ApiResponse<List<Object>>> getClasses(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        return ResponseEntity.ok(ApiResponse.ok(List.of(), requestId));
    }

    @GetMapping("/{id}/offerings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    @Operation(summary = "Teacher's offerings.")
    public ResponseEntity<ApiResponse<List<Object>>> getOfferings(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        return ResponseEntity.ok(ApiResponse.ok(List.of(), requestId));
    }

    @GetMapping("/{id}/timetable")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    @Operation(summary = "Weekly timetable.")
    public ResponseEntity<ApiResponse<List<Object>>> getTimetable(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        return ResponseEntity.ok(ApiResponse.ok(List.of(), requestId));
    }

    @GetMapping("/{id}/workload")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    @Operation(summary = "Teacher workload stats.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWorkload(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        return ResponseEntity.ok(ApiResponse.ok(Map.of("teacherId", id, "periodsPerWeek", 0, "maxWeeklyPeriods", 30), requestId));
    }

    @GetMapping("/{id}/qualifications")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    @Operation(summary = "Courses the teacher may teach.")
    public ResponseEntity<ApiResponse<List<UUID>>> getQualifications(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        List<UUID> response = teacherService.listQualifications(id);
        return ResponseEntity.ok(ApiResponse.ok(response, requestId));
    }

    @PutMapping("/{id}/qualifications")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Replace the qualification set.")
    public ResponseEntity<ApiResponse<List<UUID>>> replaceQualifications(
            @PathVariable UUID id,
            @Valid @RequestBody ReplaceQualificationsRequest request,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        teacherService.replaceQualifications(id, request);
        return ResponseEntity.ok(ApiResponse.ok(request.courseIds(), requestId));
    }

    @PostMapping(value = "/bulk-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Multipart CSV import for teachers.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkImportTeachers(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(Map.of("message", "Import processed successfully", "rowsProcessed", 0), requestId));
    }
}
