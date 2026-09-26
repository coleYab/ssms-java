package com.example.ssms.grade.controller;

import com.example.ssms.common.dto.ApiResponse;
import com.example.ssms.grade.dto.GradeDtos.*;
import com.example.ssms.grade.service.GradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/grade-levels")
@Tag(name = "Grade Levels", description = "Endpoints for academic grade levels")
public class GradeLevelController {

    private final GradeService gradeService;

    public GradeLevelController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @GetMapping
    @Operation(summary = "List grade levels ordered by sequence.")
    public ResponseEntity<ApiResponse<List<GradeLevelResponse>>> listGradeLevels(HttpServletRequest httpRequest) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        List<GradeLevelResponse> response = gradeService.listGradeLevels();
        return ResponseEntity.ok(ApiResponse.ok(response, requestId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Create grade level.")
    public ResponseEntity<ApiResponse<GradeLevelResponse>> createGradeLevel(
            @Valid @RequestBody CreateGradeLevelRequest request,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        GradeLevelResponse response = gradeService.createGradeLevel(request);
        return ResponseEntity
                .created(URI.create("/api/v1/grade-levels/" + response.id()))
                .body(ApiResponse.ok(response, requestId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one grade level.")
    public ResponseEntity<ApiResponse<GradeLevelResponse>> getGradeLevel(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        GradeLevelResponse response = gradeService.getGradeLevel(id);
        return ResponseEntity.ok(ApiResponse.ok(response, requestId));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Update grade level.")
    public ResponseEntity<ApiResponse<GradeLevelResponse>> updateGradeLevel(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGradeLevelRequest request,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        GradeLevelResponse response = gradeService.updateGradeLevel(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, requestId));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Delete grade level if no classes or courses use it.")
    public void deleteGradeLevel(@PathVariable UUID id) {
        gradeService.deleteGradeLevel(id);
    }
}
