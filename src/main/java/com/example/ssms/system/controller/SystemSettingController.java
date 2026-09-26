package com.example.ssms.system.controller;

import com.example.ssms.common.dto.ApiResponse;
import com.example.ssms.system.service.SystemService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/system/settings")
public class SystemSettingController {

    private final SystemService systemService;

    public SystemSettingController(SystemService systemService) {
        this.systemService = systemService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Read system settings.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSettings(HttpServletRequest httpRequest) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        return ResponseEntity.ok(ApiResponse.ok(systemService.getAllSettings(), requestId));
    }

    @PatchMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update system settings.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateSettings(
            @RequestBody Map<String, Object> updates,
            HttpServletRequest httpRequest) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        return ResponseEntity.ok(ApiResponse.ok(systemService.updateSettings(updates), requestId));
    }
}
