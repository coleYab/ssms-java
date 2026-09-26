package com.example.ssms.academic.controller;

import com.example.ssms.academic.dto.AcademicDtos.*;
import com.example.ssms.academic.service.AcademicService;
import com.example.ssms.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/terms")
@Tag(name = "Terms", description = "Endpoints for academic terms")
public class TermController {

    private final AcademicService academicService;

    public TermController(AcademicService academicService) {
        this.academicService = academicService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get term.")
    public ResponseEntity<ApiResponse<TermResponse>> getTerm(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        TermResponse response = academicService.getTerm(id);
        return ResponseEntity.ok(ApiResponse.ok(response, requestId));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Update term.")
    public ResponseEntity<ApiResponse<TermResponse>> updateTerm(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTermRequest request,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        TermResponse response = academicService.updateTerm(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, requestId));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Delete term if no offerings.")
    public void deleteTerm(@PathVariable UUID id) {
        academicService.deleteTerm(id);
    }
}
