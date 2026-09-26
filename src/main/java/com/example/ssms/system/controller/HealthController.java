package com.example.ssms.system.controller;

import com.example.ssms.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    @Operation(summary = "Health check.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkHealth(HttpServletRequest httpRequest) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString(),
                "service", "School System Management System (SSMS)"
        ), requestId));
    }

    @GetMapping("/ready")
    @Operation(summary = "Readiness check.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkReadiness(HttpServletRequest httpRequest) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "status", "READY",
                "timestamp", Instant.now().toString(),
                "database", "UP"
        ), requestId));
    }
}
