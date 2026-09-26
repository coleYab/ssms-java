package com.example.ssms.course.controller;

import com.example.ssms.common.dto.ApiResponse;
import com.example.ssms.common.dto.LinksDto;
import com.example.ssms.common.dto.PaginationDto;
import com.example.ssms.course.dto.CourseDtos.*;
import com.example.ssms.course.service.CourseService;
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
@RequestMapping("/api/v1/courses")
@Tag(name = "Courses", description = "Endpoints for course catalogue and curriculum")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    @Operation(summary = "List courses.")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> listCourses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        Page<CourseResponse> paged = courseService.listCourses(page, limit);
        PaginationDto pagination = PaginationDto.of(page, limit, paged.getTotalElements());
        LinksDto links = LinksDto.of("/api/v1/courses", page, limit, paged.getTotalPages());
        return ResponseEntity.ok(ApiResponse.ok(paged.getContent(), requestId, pagination, links));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Create course.")
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(
            @Valid @RequestBody CreateCourseRequest request,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        CourseResponse response = courseService.createCourse(request);
        return ResponseEntity
                .created(URI.create("/api/v1/courses/" + response.id()))
                .body(ApiResponse.ok(response, requestId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get course.")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourse(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        CourseResponse response = courseService.getCourse(id);
        return ResponseEntity.ok(ApiResponse.ok(response, requestId));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Update course.")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCourseRequest request,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        CourseResponse response = courseService.updateCourse(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, requestId));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Delete course.")
    public void deleteCourse(@PathVariable UUID id) {
        courseService.deleteCourse(id);
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Set ARCHIVED.")
    public ResponseEntity<ApiResponse<CourseResponse>> archiveCourse(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        CourseResponse response = courseService.archiveCourse(id);
        return ResponseEntity.ok(ApiResponse.ok(response, requestId));
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Set ACTIVE.")
    public ResponseEntity<ApiResponse<CourseResponse>> unarchiveCourse(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        CourseResponse response = courseService.unarchiveCourse(id);
        return ResponseEntity.ok(ApiResponse.ok(response, requestId));
    }

    @GetMapping("/{id}/offerings")
    @Operation(summary = "Offerings of the course across classes/terms.")
    public ResponseEntity<ApiResponse<List<Object>>> getOfferings(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        return ResponseEntity.ok(ApiResponse.ok(List.of(), requestId));
    }
}
