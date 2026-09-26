package com.example.ssms.admin.controller;

import com.example.ssms.admin.dto.AdminDtos.*;
import com.example.ssms.admin.service.AdminService;
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
@RequestMapping("/api/v1/admins")
@Tag(name = "Administrator Accounts", description = "Endpoints for administrator lifecycle governed by SUPER_ADMIN")
public class AdminController {

	private final AdminService adminService;

	public AdminController(AdminService adminService) {
		this.adminService = adminService;
	}

	@PostMapping
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	@Operation(summary = "Create an ADMIN account.")
	public ResponseEntity<ApiResponse<AdminResponse>> createAdmin(@AuthenticationPrincipal UserPrincipal caller,
			@Valid @RequestBody CreateAdminRequest request, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		AdminResponse response = adminService.createAdmin(caller, request);
		return ResponseEntity.created(URI.create("/api/v1/admins/" + response.id()))
				.body(ApiResponse.ok(response, requestId));
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "List admins.")
	public ResponseEntity<ApiResponse<List<AdminResponse>>> listAdmins(@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int limit, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		Page<AdminResponse> paged = adminService.listAdmins(page, limit);
		PaginationDto pagination = PaginationDto.of(page, limit, paged.getTotalElements());
		LinksDto links = LinksDto.of("/api/v1/admins", page, limit, paged.getTotalPages());
		return ResponseEntity.ok(ApiResponse.ok(paged.getContent(), requestId, pagination, links));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Get one admin.")
	public ResponseEntity<ApiResponse<AdminResponse>> getAdmin(@PathVariable UUID id, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		AdminResponse response = adminService.getAdmin(id);
		return ResponseEntity.ok(ApiResponse.ok(response, requestId));
	}

	@PatchMapping("/{id}")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	@Operation(summary = "Update admin profile.")
	public ResponseEntity<ApiResponse<AdminResponse>> updateAdmin(@AuthenticationPrincipal UserPrincipal caller,
			@PathVariable UUID id, @Valid @RequestBody UpdateAdminRequest request, HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		AdminResponse response = adminService.updateAdmin(caller, id, request);
		return ResponseEntity.ok(ApiResponse.ok(response, requestId));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	@Operation(summary = "Soft delete admin.")
	public void deleteAdmin(@AuthenticationPrincipal UserPrincipal caller, @PathVariable UUID id) {
		adminService.deleteAdmin(caller, id);
	}

	@PostMapping("/{id}/revoke-sessions")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	@Operation(summary = "Force sign-out of all admin sessions.")
	public void revokeSessions(@PathVariable UUID id) {
		adminService.revokeSessions(id);
	}
}
