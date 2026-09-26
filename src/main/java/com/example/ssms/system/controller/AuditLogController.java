package com.example.ssms.system.controller;

import com.example.ssms.audit.entity.AuditLog;
import com.example.ssms.audit.service.AuditLogService;
import com.example.ssms.common.dto.ApiResponse;
import com.example.ssms.common.dto.LinksDto;
import com.example.ssms.common.dto.PaginationDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

	private final AuditLogService auditLogService;

	public AuditLogController(AuditLogService auditLogService) {
		this.auditLogService = auditLogService;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Search audit trail.")
	public ResponseEntity<ApiResponse<List<AuditLog>>> getAuditLogs(@RequestParam(required = false) UUID userId,
			@RequestParam(required = false) String action, @RequestParam(required = false) String entityType,
			@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int limit,
			HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		int pageIndex = Math.max(0, page - 1);
		Page<AuditLog> auditPage = auditLogService.searchLogs(userId, action, entityType,
				PageRequest.of(pageIndex, limit, Sort.by(Sort.Direction.DESC, "timestamp")));

		PaginationDto pagination = PaginationDto.of(page, limit, auditPage.getTotalElements());
		LinksDto links = LinksDto.of("/api/v1/audit-logs", page, limit, auditPage.getTotalPages());
		return ResponseEntity.ok(ApiResponse.ok(auditPage.getContent(), requestId, pagination, links));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Get one audit log entry.")
	public ResponseEntity<ApiResponse<AuditLog>> getAuditLogById(@PathVariable UUID id,
			HttpServletRequest httpRequest) {
		String requestId = (String) httpRequest.getAttribute("X-Request-ID");
		AuditLog log = auditLogService.getLogById(id);
		return ResponseEntity.ok(ApiResponse.ok(log, requestId));
	}
}
