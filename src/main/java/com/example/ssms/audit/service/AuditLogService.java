package com.example.ssms.audit.service;

import com.example.ssms.audit.entity.AuditLog;
import com.example.ssms.audit.repository.AuditLogRepository;
import com.example.ssms.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Service
public class AuditLogService {

	private final AuditLogRepository auditLogRepository;

	public AuditLogService(AuditLogRepository auditLogRepository) {
		this.auditLogRepository = auditLogRepository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public AuditLog record(String action, String resourceType, UUID resourceId, String outcome, String changes,
			String reason) {
		AuditLog log = new AuditLog();
		log.setAction(action);
		log.setResourceType(resourceType);
		log.setResourceId(resourceId);
		log.setOutcome(outcome);
		log.setChanges(changes);
		log.setReason(reason);

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
			log.setActorId(principal.getId());
			log.setActorRole(principal.getRole().name());
			log.setActorEmail(principal.getUsername());
			log.setSessionId(principal.getSessionId());
		}

		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attributes != null) {
			HttpServletRequest request = attributes.getRequest();
			log.setIp(getClientIp(request));
			log.setUserAgent(request.getHeader("User-Agent"));
			log.setRequestId((String) request.getAttribute("X-Request-ID"));
			if (log.getRequestId() == null) {
				log.setRequestId(request.getHeader("X-Request-ID"));
			}
		}

		return auditLogRepository.save(log);
	}

	private String getClientIp(HttpServletRequest request) {
		String xf = request.getHeader("X-Forwarded-For");
		if (xf != null && !xf.isBlank()) {
			return xf.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	@Transactional(readOnly = true)
	public org.springframework.data.domain.Page<AuditLog> searchLogs(UUID userId, String action, String entityType,
			org.springframework.data.domain.Pageable pageable) {
		return auditLogRepository.findAll((root, query, cb) -> {
			java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
			if (userId != null) {
				predicates.add(cb.equal(root.get("actorId"), userId));
			}
			if (action != null && !action.isBlank()) {
				predicates.add(cb.like(cb.lower(root.get("action")), "%" + action.toLowerCase() + "%"));
			}
			if (entityType != null && !entityType.isBlank()) {
				predicates.add(cb.equal(cb.lower(root.get("resourceType")), entityType.toLowerCase()));
			}
			return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
		}, pageable);
	}

	@Transactional(readOnly = true)
	public AuditLog getLogById(UUID id) {
		return auditLogRepository.findById(id).orElseThrow(() -> new com.example.ssms.common.exception.ApiException(
				com.example.ssms.common.constant.ErrorCode.RESOURCE_NOT_FOUND, "Audit log not found."));
	}
}
