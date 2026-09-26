package com.example.ssms.grade.service;

import com.example.ssms.audit.service.AuditLogService;
import com.example.ssms.common.constant.ErrorCode;
import com.example.ssms.common.exception.ApiException;
import com.example.ssms.grade.dto.GradeDtos.*;
import com.example.ssms.grade.entity.GradeLevel;
import com.example.ssms.grade.repository.GradeLevelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GradeService {

	private final GradeLevelRepository gradeLevelRepository;
	private final AuditLogService auditLogService;

	public GradeService(GradeLevelRepository gradeLevelRepository, AuditLogService auditLogService) {
		this.gradeLevelRepository = gradeLevelRepository;
		this.auditLogService = auditLogService;
	}

	@Transactional(readOnly = true)
	public List<GradeLevelResponse> listGradeLevels() {
		return gradeLevelRepository.findAllByOrderBySequenceAsc().stream().map(this::mapToResponse).toList();
	}

	@Transactional
	public GradeLevelResponse createGradeLevel(CreateGradeLevelRequest request) {
		if (gradeLevelRepository.findByNameIgnoreCase(request.name().trim()).isPresent()) {
			throw new ApiException(ErrorCode.CONFLICT_DUPLICATE, "A grade level with this name already exists.")
					.withContext("conflictingField", "name");
		}
		if (gradeLevelRepository.findByCodeIgnoreCase(request.code().trim()).isPresent()) {
			throw new ApiException(ErrorCode.CONFLICT_DUPLICATE, "A grade level with this code already exists.")
					.withContext("conflictingField", "code");
		}
		if (gradeLevelRepository.findBySequence(request.sequence()).isPresent()) {
			throw new ApiException(ErrorCode.CONFLICT_DUPLICATE, "A grade level with this sequence already exists.")
					.withContext("conflictingField", "sequence");
		}

		GradeLevel level = new GradeLevel();
		level.setName(request.name().trim());
		level.setCode(request.code().trim().toUpperCase());
		level.setSequence(request.sequence());
		level.setDescription(request.description());
		level = gradeLevelRepository.save(level);

		auditLogService.record("GRADE_LEVEL_CREATE", "GRADE_LEVEL", level.getId(), "SUCCESS", null, null);
		return mapToResponse(level);
	}

	@Transactional(readOnly = true)
	public GradeLevelResponse getGradeLevel(UUID id) {
		GradeLevel level = gradeLevelRepository.findById(id)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Grade level not found."));
		return mapToResponse(level);
	}

	@Transactional
	public GradeLevelResponse updateGradeLevel(UUID id, UpdateGradeLevelRequest request) {
		GradeLevel level = gradeLevelRepository.findById(id)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Grade level not found."));

		if (request.name() != null && !request.name().trim().equalsIgnoreCase(level.getName())) {
			if (gradeLevelRepository.findByNameIgnoreCase(request.name().trim()).isPresent()) {
				throw new ApiException(ErrorCode.CONFLICT_DUPLICATE, "A grade level with this name already exists.");
			}
			level.setName(request.name().trim());
		}

		if (request.code() != null && !request.code().trim().equalsIgnoreCase(level.getCode())) {
			if (gradeLevelRepository.findByCodeIgnoreCase(request.code().trim()).isPresent()) {
				throw new ApiException(ErrorCode.CONFLICT_DUPLICATE, "A grade level with this code already exists.");
			}
			level.setCode(request.code().trim().toUpperCase());
		}

		if (request.sequence() != null && request.sequence() != level.getSequence()) {
			if (gradeLevelRepository.findBySequence(request.sequence()).isPresent()) {
				throw new ApiException(ErrorCode.CONFLICT_DUPLICATE,
						"A grade level with this sequence already exists.");
			}
			level.setSequence(request.sequence());
		}

		if (request.description() != null) {
			level.setDescription(request.description());
		}

		gradeLevelRepository.save(level);
		auditLogService.record("GRADE_LEVEL_UPDATE", "GRADE_LEVEL", level.getId(), "SUCCESS", null, null);
		return mapToResponse(level);
	}

	@Transactional
	public void deleteGradeLevel(UUID id) {
		GradeLevel level = gradeLevelRepository.findById(id)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Grade level not found."));

		gradeLevelRepository.delete(level);
		auditLogService.record("GRADE_LEVEL_DELETE", "GRADE_LEVEL", id, "SUCCESS", null, null);
	}

	private GradeLevelResponse mapToResponse(GradeLevel level) {
		return new GradeLevelResponse(level.getId(), level.getName(), level.getCode(), level.getSequence(),
				level.getDescription(), level.getCreatedAt(), level.getUpdatedAt(), level.getVersion());
	}
}
