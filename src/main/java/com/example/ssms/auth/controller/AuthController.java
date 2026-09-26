package com.example.ssms.auth.controller;

import com.example.ssms.auth.dto.AuthDtos.*;
import com.example.ssms.auth.service.AuthService;
import com.example.ssms.common.dto.ApiResponse;
import com.example.ssms.security.UserPrincipal;
import com.example.ssms.security.jwt.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@Tag(name = "Authentication & Sessions", description = "Endpoints for login, token refresh, sessions and password management")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenService jwtTokenService;

    public AuthController(AuthService authService, JwtTokenService jwtTokenService) {
        this.authService = authService;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/api/v1/auth/login")
    @Operation(summary = "Email and password login. Returns tokens.")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        LoginResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(response, requestId));
    }

    @PostMapping("/api/v1/auth/refresh")
    @Operation(summary = "Rotate refresh token, issue new access token.")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @Valid @RequestBody RefreshRequest request,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        LoginResponse response = authService.refresh(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(response, requestId));
    }

    @PostMapping("/api/v1/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke the current session.")
    public void logout(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal != null) {
            authService.logout(principal.getSessionId());
        }
    }

    @PostMapping("/api/v1/auth/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke every session of the caller.")
    public void logoutAll(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal != null) {
            authService.logoutAll(principal.getId());
        }
    }

    @PostMapping("/api/v1/auth/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Send reset link (always 202).")
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
    }

    @PostMapping("/api/v1/auth/reset-password")
    @Operation(summary = "Set new password using reset token.")
    public ResponseEntity<ApiResponse<Map<String, String>>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Password reset successfully."), requestId));
    }

    @PostMapping("/api/v1/auth/change-password")
    @Operation(summary = "Change own password; revokes other sessions.")
    public ResponseEntity<ApiResponse<Map<String, String>>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        authService.changePassword(principal, request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Password changed successfully."), requestId));
    }

    @GetMapping("/api/v1/auth/me")
    @Operation(summary = "Own account plus linked profile and effective permissions.")
    public ResponseEntity<ApiResponse<MeResponse>> getMe(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        MeResponse response = authService.getMe(principal);
        return ResponseEntity.ok(ApiResponse.ok(response, requestId));
    }

    @PatchMapping("/api/v1/auth/me")
    @Operation(summary = "Update own phone, preferredLanguage.")
    public ResponseEntity<ApiResponse<UserSummaryDto>> updateMe(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateMeRequest request,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        UserSummaryDto response = authService.updateMe(principal, request);
        return ResponseEntity.ok(ApiResponse.ok(response, requestId));
    }

    @GetMapping("/api/v1/auth/sessions")
    @Operation(summary = "List own active sessions.")
    public ResponseEntity<ApiResponse<List<SessionDto>>> listSessions(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        String requestId = (String) httpRequest.getAttribute("X-Request-ID");
        List<SessionDto> sessions = authService.listSessions(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(sessions, requestId));
    }

    @DeleteMapping("/api/v1/auth/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke one own session.")
    public void revokeSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId
    ) {
        authService.revokeSession(principal.getId(), sessionId);
    }

    @GetMapping("/.well-known/jwks.json")
    @Operation(summary = "JWKS public key set for JWT verification")
    public Map<String, Object> jwks() {
        return jwtTokenService.getJwks();
    }
}
