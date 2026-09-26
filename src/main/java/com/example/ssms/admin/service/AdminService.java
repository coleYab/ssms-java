package com.example.ssms.admin.service;

import com.example.ssms.admin.dto.AdminDtos.*;
import com.example.ssms.admin.entity.AdminProfile;
import com.example.ssms.admin.repository.AdminProfileRepository;
import com.example.ssms.audit.service.AuditLogService;
import com.example.ssms.auth.entity.User;
import com.example.ssms.auth.repository.UserRepository;
import com.example.ssms.auth.repository.UserSessionRepository;
import com.example.ssms.auth.service.PasswordService;
import com.example.ssms.common.constant.ErrorCode;
import com.example.ssms.common.exception.ApiException;
import com.example.ssms.security.AccountStatus;
import com.example.ssms.security.ProfileType;
import com.example.ssms.security.Role;
import com.example.ssms.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AdminService {

	private final UserRepository userRepository;
	private final AdminProfileRepository adminProfileRepository;
	private final UserSessionRepository userSessionRepository;
	private final PasswordService passwordService;
	private final AuditLogService auditLogService;

	public AdminService(UserRepository userRepository, AdminProfileRepository adminProfileRepository,
			UserSessionRepository userSessionRepository, PasswordService passwordService,
			AuditLogService auditLogService) {
		this.userRepository = userRepository;
		this.adminProfileRepository = adminProfileRepository;
		this.userSessionRepository = userSessionRepository;
		this.passwordService = passwordService;
		this.auditLogService = auditLogService;
	}

	@Transactional
	public AdminResponse createAdmin(UserPrincipal caller, CreateAdminRequest request) {
		verifyStepUp(caller);

		String email = request.email().toLowerCase().trim();
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new ApiException(ErrorCode.CONFLICT_DUPLICATE, "A user with this email already exists.")
					.withContext("conflictingField", "email");
		}

		String tempPassword = "TempPassword1234!";
		String hash = passwordService.hash(tempPassword);

		User user = new User();
		user.setEmail(email);
		user.setPasswordHash(hash);
		user.setRole(Role.ADMIN);
		user.setStatus(AccountStatus.PENDING);
		user.setMustChangePassword(true);
		user.setPhone(request.phone());
		user.setProfileType(ProfileType.ADMIN);
		user = userRepository.save(user);

		AdminProfile profile = new AdminProfile();
		profile.setUserId(user.getId());
		profile.setFirstName(request.firstName());
		profile.setLastName(request.lastName());
		profile.setDepartment(request.department());
		profile.setJobTitle(request.jobTitle());
		profile = adminProfileRepository.save(profile);

		user.setProfileId(profile.getId());
		userRepository.save(user);

		auditLogService.record("ADMIN_CREATE", "ADMIN", profile.getId(), "SUCCESS", null, "Admin created");

		return mapToResponse(user, profile);
	}

	@Transactional(readOnly = true)
	public Page<AdminResponse> listAdmins(int page, int limit) {
		PageRequest pageRequest = PageRequest.of(Math.max(0, page - 1), Math.min(100, Math.max(1, limit)));
		return adminProfileRepository.findAll(pageRequest).map(profile -> {
			User user = userRepository.findById(profile.getUserId()).orElse(null);
			return mapToResponse(user, profile);
		});
	}

	@Transactional(readOnly = true)
	public AdminResponse getAdmin(UUID id) {
		AdminProfile profile = adminProfileRepository.findById(id)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Admin not found."));
		User user = userRepository.findById(profile.getUserId()).orElse(null);
		return mapToResponse(user, profile);
	}

	@Transactional
	public AdminResponse updateAdmin(UserPrincipal caller, UUID id, UpdateAdminRequest request) {
		verifyStepUp(caller);

		AdminProfile profile = adminProfileRepository.findById(id)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Admin not found."));

		User user = userRepository.findById(profile.getUserId())
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));

		if (user.getRole() == Role.SUPER_ADMIN && caller.getRole() != Role.SUPER_ADMIN) {
			throw new ApiException(ErrorCode.AUTHZ_PRIVILEGED_TARGET);
		}

		if (request.firstName() != null)
			profile.setFirstName(request.firstName());
		if (request.lastName() != null)
			profile.setLastName(request.lastName());
		if (request.department() != null)
			profile.setDepartment(request.department());
		if (request.jobTitle() != null)
			profile.setJobTitle(request.jobTitle());
		if (request.phone() != null) {
			user.setPhone(request.phone());
		}
		if (request.email() != null) {
			String newEmail = request.email().toLowerCase().trim();
			if (!newEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmailIgnoreCase(newEmail)) {
				throw new ApiException(ErrorCode.CONFLICT_DUPLICATE, "A user with this email already exists.")
						.withContext("conflictingField", "email");
			}
			user.setEmail(newEmail);
		}

		adminProfileRepository.save(profile);
		userRepository.save(user);

		auditLogService.record("ADMIN_UPDATE", "ADMIN", profile.getId(), "SUCCESS", null, null);

		return mapToResponse(user, profile);
	}

	@Transactional
	public void deleteAdmin(UserPrincipal caller, UUID id) {
		verifyStepUp(caller);

		AdminProfile profile = adminProfileRepository.findById(id)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Admin not found."));

		User user = userRepository.findById(profile.getUserId())
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));

		if (caller.getId().equals(user.getId())) {
			throw new ApiException(ErrorCode.AUTHZ_SELF_MODIFICATION_FORBIDDEN);
		}

		if (user.getRole() == Role.SUPER_ADMIN) {
			long superAdminCount = userRepository.countActiveUsersByRole(Role.SUPER_ADMIN);
			if (superAdminCount <= 1) {
				throw new ApiException(ErrorCode.BUSINESS_LAST_SUPER_ADMIN);
			}
		}

		user.setDeletedAt(Instant.now());
		user.setStatus(AccountStatus.INACTIVE);
		userRepository.save(user);

		profile.setDeletedAt(Instant.now());
		adminProfileRepository.save(profile);

		userSessionRepository.revokeAllByUserId(user.getId());

		auditLogService.record("ADMIN_DELETE", "ADMIN", profile.getId(), "SUCCESS", null, "Soft deleted admin");
	}

	@Transactional
	public void revokeSessions(UUID id) {
		AdminProfile profile = adminProfileRepository.findById(id)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Admin not found."));
		userSessionRepository.revokeAllByUserId(profile.getUserId());
	}

	private void verifyStepUp(UserPrincipal caller) {
		if (caller != null && !caller.isStepUpValid(600)) { // 10 minutes
			throw new ApiException(ErrorCode.AUTH_REAUTH_REQUIRED);
		}
	}

	private AdminResponse mapToResponse(User user, AdminProfile profile) {
		return new AdminResponse(profile.getId(), profile.getFirstName(), profile.getLastName(),
				user != null ? user.getEmail() : null, user != null ? user.getPhone() : null, profile.getDepartment(),
				profile.getJobTitle(), user != null ? user.getRole() : Role.ADMIN,
				user != null ? user.getStatus() : AccountStatus.ACTIVE, user != null && user.isMustChangePassword(),
				profile.getCreatedAt(), profile.getUpdatedAt(), profile.getVersion());
	}
}
