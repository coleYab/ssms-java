package com.example.ssms.system.controller;

import com.example.ssms.common.dto.ApiResponse;
import com.example.ssms.security.Role;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;

import java.util.*;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private static final Map<Role, List<String>> ROLE_PERMISSIONS = new LinkedHashMap<>();

    static {
        ROLE_PERMISSIONS.put(Role.SUPER_ADMIN, List.of(
                "system:manage", "users:read", "users:write", "users:delete",
                "admins:manage", "academic:manage", "classes:manage",
                "students:manage", "teachers:manage", "courses:manage",
                "attendance:manage", "audit:read"
        ));
        ROLE_PERMISSIONS.put(Role.ADMIN, List.of(
                "users:read", "users:write",
                "academic:manage", "classes:manage",
                "students:manage", "teachers:manage", "courses:manage",
                "attendance:manage", "audit:read"
        ));
        ROLE_PERMISSIONS.put(Role.TEACHER, List.of(
                "profile:read", "profile:write",
                "classes:read", "courses:read",
                "attendance:take", "attendance:edit", "attendance:read"
        ));
        ROLE_PERMISSIONS.put(Role.STUDENT, List.of(
                "profile:read", "classes:read",
                "courses:read", "attendance:read", "excuses:submit"
        ));
    }

    @GetMapping
    @Operation(summary = "List roles with rank and description.")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> listRoles(HttpServletRequest httpRequest) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        List<Map<String, String>> roles = Arrays.stream(Role.values())
                .map(r -> Map.of("name", r.name(), "authority", "ROLE_" + r.name()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(roles, requestId));
    }

    @GetMapping("/{role}/permissions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Permissions of a role.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRolePermissions(
            @PathVariable String role,
            HttpServletRequest httpRequest) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        Role r;
        try {
            r = Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            String sanitized = role.toUpperCase().startsWith("ROLE_") ? role.substring(5).toUpperCase() : role.toUpperCase();
            r = Role.valueOf(sanitized);
        }

        List<String> permissions = ROLE_PERMISSIONS.getOrDefault(r, Collections.emptyList());
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "role", r.name(),
                "permissions", permissions
        ), requestId));
    }
}
