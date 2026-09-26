package com.example.ssms.user.controller;

import com.example.ssms.common.dto.ApiResponse;
import com.example.ssms.common.dto.LinksDto;
import com.example.ssms.common.dto.PaginationDto;
import com.example.ssms.security.AccountStatus;
import com.example.ssms.security.Role;
import com.example.ssms.security.UserPrincipal;
import com.example.ssms.user.dto.UserDtos.*;
import com.example.ssms.user.service.UserService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Cross-role account administration")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "List accounts.")
	public ResponseEntity<ApiResponse<List<UserDetailsResponse>>> listUsers(@RequestParam(required = false) Role role,
			@RequestParam(required = false) AccountStatus status, @RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int limit, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		Page<UserDetailsResponse> paged = userService.listUsers(role, status, page, limit);
		PaginationDto pagination = PaginationDto.of(page, limit, paged.getTotalElements());
		LinksDto links = LinksDto.of("/api/v1/users", page, limit, paged.getTotalPages());
		return ResponseEntity.ok(ApiResponse.ok(paged.getContent(), requestId, pagination, links));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Get account.")
	public ResponseEntity<ApiResponse<UserDetailsResponse>> getUser(@PathVariable UUID id,
			HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		UserDetailsResponse response = userService.getUser(id);
		return ResponseEntity.ok(ApiResponse.ok(response, requestId));
	}

	@PatchMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Update email/phone of an account.")
	public ResponseEntity<ApiResponse<UserDetailsResponse>> updateUser(@AuthenticationPrincipal UserPrincipal caller,
			@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		UserDetailsResponse response = userService.updateUser(caller, id, request);
		return ResponseEntity.ok(ApiResponse.ok(response, requestId));
	}

	@PostMapping("/{id}/deactivate")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Set INACTIVE, revoke sessions.")
	public ResponseEntity<ApiResponse<UserDetailsResponse>> deactivateUser(
			@AuthenticationPrincipal UserPrincipal caller, @PathVariable UUID id,
			@Valid @RequestBody DeactivateUserRequest request, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		UserDetailsResponse response = userService.deactivateUser(caller, id, request.reason());
		return ResponseEntity.ok(ApiResponse.ok(response, requestId));
	}

	@PostMapping("/{id}/activate")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Set ACTIVE.")
	public ResponseEntity<ApiResponse<UserDetailsResponse>> activateUser(@AuthenticationPrincipal UserPrincipal caller,
			@PathVariable UUID id, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		UserDetailsResponse response = userService.activateUser(caller, id);
		return ResponseEntity.ok(ApiResponse.ok(response, requestId));
	}

	@PostMapping("/{id}/reset-password")
	@ResponseStatus(HttpStatus.ACCEPTED)
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Issue temporary password / reset link.")
	public void resetPassword(@AuthenticationPrincipal UserPrincipal caller, @PathVariable UUID id) {
		userService.resetPassword(caller, id);
	}

	@PostMapping("/{id}/unlock")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Clear lockout and failed-login counter.")
	public ResponseEntity<ApiResponse<UserDetailsResponse>> unlockUser(@AuthenticationPrincipal UserPrincipal caller,
			@PathVariable UUID id, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		UserDetailsResponse response = userService.unlockUser(caller, id);
		return ResponseEntity.ok(ApiResponse.ok(response, requestId));
	}

	@PatchMapping("/{id}/role")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	@Operation(summary = "Change role (SA step-up).")
	public ResponseEntity<ApiResponse<UserDetailsResponse>> changeRole(@AuthenticationPrincipal UserPrincipal caller,
			@PathVariable UUID id, @Valid @RequestBody ChangeRoleRequest request, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		UserDetailsResponse response = userService.changeRole(caller, id, request.role());
		return ResponseEntity.ok(ApiResponse.ok(response, requestId));
	}

	@DeleteMapping("/{id}/permanent")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	@Operation(summary = "Hard delete a soft-deleted account (GDPR erasure).")
	public void permanentDelete(@AuthenticationPrincipal UserPrincipal caller, @PathVariable UUID id) {
		userService.permanentDelete(caller, id);
	}

	@PostMapping("/{id}/restore")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	@Operation(summary = "Restore a soft-deleted account.")
	public ResponseEntity<ApiResponse<UserDetailsResponse>> restoreUser(@AuthenticationPrincipal UserPrincipal caller,
			@PathVariable UUID id, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		UserDetailsResponse response = userService.restoreUser(caller, id);
		return ResponseEntity.ok(ApiResponse.ok(response, requestId));
	}
}
