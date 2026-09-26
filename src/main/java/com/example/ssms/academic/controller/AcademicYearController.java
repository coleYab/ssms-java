package com.example.ssms.academic.controller;

import com.example.ssms.academic.dto.AcademicDtos.*;
import com.example.ssms.academic.service.AcademicService;
import com.example.ssms.common.dto.ApiResponse;
import com.example.ssms.common.dto.LinksDto;
import com.example.ssms.common.dto.PaginationDto;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/academic-years")
@Tag(name = "Academic Years", description = "Endpoints for academic calendar and year management")
public class AcademicYearController {

    private final AcademicService academicService;

    public AcademicYearController(AcademicService academicService) {
        this.academicService = academicService;
    }

    @GetMapping
    @Operation(summary = "List years.")
    public ResponseEntity<ApiResponse<List<AcademicYearResponse>>> listAcademicYears(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        Page<AcademicYearResponse> paged = academicService.listAcademicYears(page, limit);
        PaginationDto pagination = PaginationDto.of(page, limit, paged.getTotalElements());
        LinksDto links = LinksDto.of("/api/v1/academic-years", page, limit, paged.getTotalPages());
        return ResponseEntity.ok(ApiResponse.ok(paged.getContent(), requestId, pagination, links));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Create year (PLANNED).")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> createAcademicYear(
            @Valid @RequestBody CreateAcademicYearRequest request,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        AcademicYearResponse response = academicService.createAcademicYear(request);
        return ResponseEntity
                .created(URI.create("/api/v1/academic-years/" + response.id()))
                .body(ApiResponse.ok(response, requestId));
    }

    @GetMapping("/current")
    @Operation(summary = "The single ACTIVE year with its terms.")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> getCurrentAcademicYear(HttpServletRequest httpRequest) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        AcademicYearResponse response = academicService.getCurrentAcademicYear();
        return ResponseEntity.ok(ApiResponse.ok(response, requestId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get year (include=terms).")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> getAcademicYear(
            @PathVariable UUID id,
            @RequestParam(required = false) String include,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        boolean includeTerms = include != null && include.contains("terms");
        AcademicYearResponse response = academicService.getAcademicYear(id, includeTerms);
        return ResponseEntity.ok(ApiResponse.ok(response, requestId));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Update name/dates.")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> updateAcademicYear(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAcademicYearRequest request,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        AcademicYearResponse response = academicService.updateAcademicYear(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, requestId));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Delete only if PLANNED and has no classes.")
    public void deleteAcademicYear(@PathVariable UUID id) {
        academicService.deleteAcademicYear(id);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Make active.")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> activateAcademicYear(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        AcademicYearResponse response = academicService.activateAcademicYear(id);
        return ResponseEntity.ok(ApiResponse.ok(response, requestId));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Lock year (step-up).")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> closeAcademicYear(
            @AuthenticationPrincipal UserPrincipal caller,
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        AcademicYearResponse response = academicService.closeAcademicYear(caller, id);
        return ResponseEntity.ok(ApiResponse.ok(response, requestId));
    }

    @GetMapping("/{id}/terms")
    @Operation(summary = "List terms of a year.")
    public ResponseEntity<ApiResponse<List<TermResponse>>> listTerms(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        List<TermResponse> response = academicService.listTerms(id);
        return ResponseEntity.ok(ApiResponse.ok(response, requestId));
    }

    @PostMapping("/{id}/terms")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Create term.")
    public ResponseEntity<ApiResponse<TermResponse>> createTerm(
            @PathVariable UUID id,
            @Valid @RequestBody CreateTermRequest request,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        TermResponse response = academicService.createTerm(id, request);
        return ResponseEntity
                .created(URI.create("/api/v1/terms/" + response.id()))
                .body(ApiResponse.ok(response, requestId));
    }
}
