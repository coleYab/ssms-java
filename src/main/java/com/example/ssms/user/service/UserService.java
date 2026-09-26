package com.example.ssms.user.service;

import com.example.ssms.audit.service.AuditLogService;
import com.example.ssms.auth.entity.User;
import com.example.ssms.auth.repository.UserRepository;
import com.example.ssms.auth.repository.UserSessionRepository;
import com.example.ssms.auth.service.PasswordService;
import com.example.ssms.common.constant.ErrorCode;
import com.example.ssms.common.exception.ApiException;
import com.example.ssms.security.AccountStatus;
import com.example.ssms.security.Role;
import com.example.ssms.security.UserPrincipal;
import com.example.ssms.user.dto.UserDtos.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordService passwordService;
    private final AuditLogService auditLogService;

    public UserService(
            UserRepository userRepository,
            UserSessionRepository userSessionRepository,
            PasswordService passwordService,
            AuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordService = passwordService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public Page<UserDetailsResponse> listUsers(Role role, AccountStatus status, int page, int limit) {
        PageRequest pageRequest = PageRequest.of(Math.max(0, page - 1), Math.min(100, Math.max(1, limit)));
        return userRepository.findAll(pageRequest).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public UserDetailsResponse getUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));
        return mapToResponse(user);
    }

    @Transactional
    public UserDetailsResponse updateUser(UserPrincipal caller, UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));

        if (caller.getRole() == Role.ADMIN && (user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN)) {
            throw new ApiException(ErrorCode.AUTHZ_PRIVILEGED_TARGET);
        }

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

        userRepository.save(user);
        auditLogService.record("USER_UPDATE", "USER", user.getId(), "SUCCESS", null, null);
        return mapToResponse(user);
    }

    @Transactional
    public UserDetailsResponse deactivateUser(UserPrincipal caller, UUID id, String reason) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));

        if (caller.getId().equals(user.getId())) {
            throw new ApiException(ErrorCode.AUTHZ_SELF_MODIFICATION_FORBIDDEN);
        }

        if (caller.getRole() == Role.ADMIN && (user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN)) {
            throw new ApiException(ErrorCode.AUTHZ_PRIVILEGED_TARGET);
        }

        if (user.getRole() == Role.SUPER_ADMIN) {
            long count = userRepository.countActiveUsersByRole(Role.SUPER_ADMIN);
            if (count <= 1) {
                throw new ApiException(ErrorCode.BUSINESS_LAST_SUPER_ADMIN);
            }
        }

        if (user.getStatus() == AccountStatus.INACTIVE) {
            throw new ApiException(ErrorCode.CONFLICT_INVALID_STATE, "User is already inactive.");
        }

        user.setStatus(AccountStatus.INACTIVE);
        userSessionRepository.revokeAllByUserId(user.getId());
        userRepository.save(user);

        auditLogService.record("USER_DEACTIVATE", "USER", user.getId(), "SUCCESS", null, reason);
        return mapToResponse(user);
    }

    @Transactional
    public UserDetailsResponse activateUser(UserPrincipal caller, UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));

        if (caller.getRole() == Role.ADMIN && (user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN)) {
            throw new ApiException(ErrorCode.AUTHZ_PRIVILEGED_TARGET);
        }

        if (user.getStatus() == AccountStatus.ACTIVE) {
            throw new ApiException(ErrorCode.CONFLICT_INVALID_STATE, "User is already active.");
        }

        user.setStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        auditLogService.record("USER_ACTIVATE", "USER", user.getId(), "SUCCESS", null, null);
        return mapToResponse(user);
    }

    @Transactional
    public void resetPassword(UserPrincipal caller, UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));

        if (caller.getId().equals(user.getId())) {
            throw new ApiException(ErrorCode.AUTHZ_SELF_MODIFICATION_FORBIDDEN);
        }

        if (caller.getRole() == Role.ADMIN && (user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN)) {
            throw new ApiException(ErrorCode.AUTHZ_PRIVILEGED_TARGET);
        }

        String tempPass = "ResetPass1234!";
        user.setPasswordHash(passwordService.hash(tempPass));
        user.setMustChangePassword(true);
        userRepository.save(user);

        userSessionRepository.revokeAllByUserId(user.getId());
        auditLogService.record("USER_RESET_PASSWORD", "USER", user.getId(), "SUCCESS", null, "Admin reset");
    }

    @Transactional
    public UserDetailsResponse unlockUser(UserPrincipal caller, UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));

        if (user.getStatus() != AccountStatus.LOCKED && user.getLockedUntil() == null) {
            throw new ApiException(ErrorCode.CONFLICT_INVALID_STATE, "User is not locked.");
        }

        user.setStatus(AccountStatus.ACTIVE);
        user.setLockedUntil(null);
        user.setFailedLoginCount(0);
        userRepository.save(user);

        auditLogService.record("USER_UNLOCK", "USER", user.getId(), "SUCCESS", null, null);
        return mapToResponse(user);
    }

    @Transactional
    public UserDetailsResponse changeRole(UserPrincipal caller, UUID id, Role newRole) {
        verifyStepUp(caller);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));

        if (caller.getId().equals(user.getId())) {
            throw new ApiException(ErrorCode.AUTHZ_SELF_MODIFICATION_FORBIDDEN);
        }

        if (user.getRole() == Role.SUPER_ADMIN && newRole != Role.SUPER_ADMIN) {
            long count = userRepository.countActiveUsersByRole(Role.SUPER_ADMIN);
            if (count <= 1) {
                throw new ApiException(ErrorCode.BUSINESS_LAST_SUPER_ADMIN);
            }
        }

        user.setRole(newRole);
        user.setPermVersion(user.getPermVersion() + 1); // Stales existing tokens!
        userRepository.save(user);

        auditLogService.record("USER_ROLE_CHANGE", "USER", user.getId(), "SUCCESS", null, "Role changed to " + newRole);
        return mapToResponse(user);
    }

    @Transactional
    public void permanentDelete(UserPrincipal caller, UUID id) {
        verifyStepUp(caller);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));

        if (user.getDeletedAt() == null) {
            throw new ApiException(ErrorCode.CONFLICT_INVALID_STATE, "Account must be soft-deleted before permanent erasure.");
        }

        userSessionRepository.revokeAllByUserId(user.getId());
        userRepository.delete(user);

        auditLogService.record("USER_HARD_DELETE", "USER", id, "SUCCESS", null, "GDPR erasure");
    }

    @Transactional
    public UserDetailsResponse restoreUser(UserPrincipal caller, UUID id) {
        verifyStepUp(caller);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_GONE, "User has been permanently removed."));

        if (user.getDeletedAt() == null) {
            throw new ApiException(ErrorCode.CONFLICT_INVALID_STATE, "User is not deleted.");
        }

        user.setDeletedAt(null);
        user.setStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        auditLogService.record("USER_RESTORE", "USER", user.getId(), "SUCCESS", null, "Account restored");
        return mapToResponse(user);
    }

    private void verifyStepUp(UserPrincipal caller) {
        if (caller != null && !caller.isStepUpValid(600)) {
            throw new ApiException(ErrorCode.AUTH_REAUTH_REQUIRED);
        }
    }

    private UserDetailsResponse mapToResponse(User user) {
        return new UserDetailsResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getPhone(),
                user.getPreferredLanguage(),
                user.isMustChangePassword(),
                user.getLastLoginAt(),
                user.getLockedUntil(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getDeletedAt(),
                user.getVersion()
        );
    }
}
