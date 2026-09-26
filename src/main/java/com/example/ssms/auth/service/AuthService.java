package com.example.ssms.auth.service;

import com.example.ssms.audit.service.AuditLogService;
import com.example.ssms.auth.dto.AuthDtos.*;
import com.example.ssms.auth.entity.PasswordResetToken;
import com.example.ssms.auth.entity.User;
import com.example.ssms.auth.entity.UserSession;
import com.example.ssms.auth.repository.PasswordResetTokenRepository;
import com.example.ssms.auth.repository.UserRepository;
import com.example.ssms.auth.repository.UserSessionRepository;
import com.example.ssms.common.constant.ErrorCode;
import com.example.ssms.common.exception.ApiException;
import com.example.ssms.security.AccountStatus;
import com.example.ssms.security.UserPrincipal;
import com.example.ssms.security.jwt.JwtTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final UserSessionRepository userSessionRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final PasswordService passwordService;
	private final JwtTokenService jwtTokenService;
	private final AuditLogService auditLogService;
	private final SecureRandom secureRandom = new SecureRandom();

	@Value("${ssms.auth.max-failed-logins:5}")
	private int maxFailedLogins;

	@Value("${ssms.auth.lockout-minutes:15}")
	private int lockoutMinutes;

	@Value("${ssms.security.jwt.refresh-token-expiration-seconds:604800}")
	private long refreshTokenExpirationSeconds;

	@Value("${ssms.security.jwt.remember-me-expiration-seconds:2592000}")
	private long rememberMeExpirationSeconds;

	public AuthService(UserRepository userRepository, UserSessionRepository userSessionRepository,
			PasswordResetTokenRepository passwordResetTokenRepository, PasswordService passwordService,
			JwtTokenService jwtTokenService, AuditLogService auditLogService) {
		this.userRepository = userRepository;
		this.userSessionRepository = userSessionRepository;
		this.passwordResetTokenRepository = passwordResetTokenRepository;
		this.passwordService = passwordService;
		this.jwtTokenService = jwtTokenService;
		this.auditLogService = auditLogService;
	}

	@Transactional
	public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
		String email = request.email().toLowerCase().trim();
		Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);

		if (userOpt.isEmpty()) {
			auditLogService.record("AUTH_LOGIN_FAILED", "USER", null, "FAILED", null, "User not found");
			throw new ApiException(ErrorCode.AUTH_INVALID_CREDENTIALS);
		}

		User user = userOpt.get();

		// Check if locked
		if (user.getLockedUntil() != null) {
			if (Instant.now().isBefore(user.getLockedUntil())) {
				throw new ApiException(ErrorCode.AUTH_ACCOUNT_LOCKED,
						"Account is temporarily locked. Try again after " + user.getLockedUntil() + ".")
						.withContext("lockedUntil", user.getLockedUntil().toString());
			} else {
				user.setLockedUntil(null);
				user.setFailedLoginCount(0);
				if (user.getStatus() == AccountStatus.LOCKED) {
					user.setStatus(AccountStatus.ACTIVE);
				}
			}
		}

		if (user.getStatus() == AccountStatus.INACTIVE || user.getDeletedAt() != null) {
			throw new ApiException(ErrorCode.AUTH_ACCOUNT_DISABLED);
		}

		if (!passwordService.matches(request.password(), user.getPasswordHash())) {
			int attempts = user.getFailedLoginCount() + 1;
			user.setFailedLoginCount(attempts);
			if (attempts >= maxFailedLogins) {
				user.setStatus(AccountStatus.LOCKED);
				Instant lockedUntil = Instant.now().plusSeconds(lockoutMinutes * 60L);
				user.setLockedUntil(lockedUntil);
				userRepository.save(user);
				auditLogService.record("AUTH_ACCOUNT_LOCKED", "USER", user.getId(), "SUCCESS", null,
						"Max failed attempts");
				throw new ApiException(ErrorCode.AUTH_ACCOUNT_LOCKED,
						"Account is temporarily locked. Try again after " + lockedUntil + ".")
						.withContext("lockedUntil", lockedUntil.toString());
			}
			userRepository.save(user);
			auditLogService.record("AUTH_LOGIN_FAILED", "USER", user.getId(), "FAILED", null, "Invalid credentials");
			throw new ApiException(ErrorCode.AUTH_INVALID_CREDENTIALS);
		}

		// Login success: reset counters
		user.setFailedLoginCount(0);
		user.setLockedUntil(null);
		user.setLastLoginAt(Instant.now());
		if (user.getStatus() == AccountStatus.PENDING) {
			user.setStatus(AccountStatus.ACTIVE);
		}
		userRepository.save(user);

		// Evict oldest session if > 5 active
		List<UserSession> activeSessions = userSessionRepository
				.findByUserIdAndRevokedFalseOrderByLastUsedAtDesc(user.getId());
		if (activeSessions.size() >= 5) {
			for (int i = 4; i < activeSessions.size(); i++) {
				activeSessions.get(i).setRevoked(true);
				userSessionRepository.save(activeSessions.get(i));
			}
		}

		// Create new session
		boolean rememberMe = Boolean.TRUE.equals(request.rememberMe());
		long refreshTtl = rememberMe ? rememberMeExpirationSeconds : refreshTokenExpirationSeconds;
		String rawRefreshToken = "rt_" + generateRandomToken();
		String tokenHash = hashToken(rawRefreshToken);

		UserSession session = new UserSession();
		session.setUserId(user.getId());
		session.setRefreshTokenHash(tokenHash);
		session.setDeviceInfo(httpRequest.getHeader("User-Agent"));
		session.setIpAddress(getClientIp(httpRequest));
		session.setExpiresAt(Instant.now().plusSeconds(refreshTtl));
		session.setLastUsedAt(Instant.now());
		session = userSessionRepository.save(session);

		String accessToken = jwtTokenService.generateAccessToken(user.getId(), user.getRole().name(),
				user.getPermVersion(), session.getId());

		auditLogService.record("AUTH_LOGIN_SUCCESS", "USER", user.getId(), "SUCCESS", null, null);

		UserSummaryDto summary = toUserSummary(user);
		return new LoginResponse(accessToken, rawRefreshToken, "Bearer",
				jwtTokenService.getAccessTokenExpirationSeconds(), summary);
	}

	@Transactional
	public LoginResponse refresh(RefreshRequest request, HttpServletRequest httpRequest) {
		String tokenHash = hashToken(request.refreshToken());
		Optional<UserSession> sessionOpt = userSessionRepository.findByRefreshTokenHash(tokenHash);

		if (sessionOpt.isEmpty()) {
			throw new ApiException(ErrorCode.AUTH_REFRESH_INVALID);
		}

		UserSession session = sessionOpt.get();
		if (session.isRevoked() || Instant.now().isAfter(session.getExpiresAt())) {
			// Check potential reuse
			userSessionRepository.revokeAllByUserId(session.getUserId());
			auditLogService.record("AUTH_REFRESH_REUSED", "USER", session.getUserId(), "FAILED", null,
					"Revoked token used");
			throw new ApiException(ErrorCode.AUTH_REFRESH_REUSED);
		}

		User user = userRepository.findById(session.getUserId())
				.orElseThrow(() -> new ApiException(ErrorCode.AUTH_REFRESH_INVALID));

		if (user.getStatus() == AccountStatus.INACTIVE || user.getDeletedAt() != null) {
			session.setRevoked(true);
			userSessionRepository.save(session);
			throw new ApiException(ErrorCode.AUTH_ACCOUNT_DISABLED);
		}

		// Rotate refresh token
		String newRawRefreshToken = "rt_" + generateRandomToken();
		session.setRefreshTokenHash(hashToken(newRawRefreshToken));
		session.setLastUsedAt(Instant.now());
		userSessionRepository.save(session);

		String accessToken = jwtTokenService.generateAccessToken(user.getId(), user.getRole().name(),
				user.getPermVersion(), session.getId());

		return new LoginResponse(accessToken, newRawRefreshToken, "Bearer",
				jwtTokenService.getAccessTokenExpirationSeconds(), toUserSummary(user));
	}

	@Transactional
	public void logout(UUID sessionId) {
		if (sessionId != null) {
			userSessionRepository.findById(sessionId).ifPresent(s -> {
				s.setRevoked(true);
				userSessionRepository.save(s);
			});
		}
	}

	@Transactional
	public void logoutAll(UUID userId) {
		userSessionRepository.revokeAllByUserId(userId);
	}

	@Transactional
	public void forgotPassword(ForgotPasswordRequest request) {
		String email = request.email().toLowerCase().trim();
		userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
			String rawToken = generateRandomToken();
			PasswordResetToken resetToken = new PasswordResetToken();
			resetToken.setUserId(user.getId());
			resetToken.setTokenHash(hashToken(rawToken));
			resetToken.setExpiresAt(Instant.now().plusSeconds(30 * 60)); // 30 min
			passwordResetTokenRepository.save(resetToken);
		});
		// Always return 202 to avoid account enumeration
	}

	@Transactional
	public void resetPassword(ResetPasswordRequest request) {
		String tokenHash = hashToken(request.token());
		PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHashAndUsedFalse(tokenHash)
				.orElseThrow(() -> new ApiException(ErrorCode.AUTH_RESET_TOKEN_INVALID));

		if (Instant.now().isAfter(resetToken.getExpiresAt())) {
			throw new ApiException(ErrorCode.AUTH_RESET_TOKEN_INVALID);
		}

		User user = userRepository.findById(resetToken.getUserId())
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));

		passwordService.validatePasswordPolicy(request.newPassword(), user.getEmail(), null, user.getId());
		String newHash = passwordService.hash(request.newPassword());
		user.setPasswordHash(newHash);
		user.setMustChangePassword(false);
		userRepository.save(user);

		passwordService.recordPassword(user.getId(), newHash);
		resetToken.setUsed(true);
		passwordResetTokenRepository.save(resetToken);

		// Revoke all sessions
		userSessionRepository.revokeAllByUserId(user.getId());
	}

	@Transactional
	public void changePassword(UserPrincipal principal, ChangePasswordRequest request) {
		User user = userRepository.findById(principal.getId())
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));

		if (!passwordService.matches(request.currentPassword(), user.getPasswordHash())) {
			throw new ApiException(ErrorCode.AUTH_CURRENT_PASSWORD_INCORRECT);
		}

		passwordService.validatePasswordPolicy(request.newPassword(), user.getEmail(), null, user.getId());
		String newHash = passwordService.hash(request.newPassword());
		user.setPasswordHash(newHash);
		user.setMustChangePassword(false);
		userRepository.save(user);

		passwordService.recordPassword(user.getId(), newHash);

		// Revoke all sessions except current
		List<UserSession> sessions = userSessionRepository
				.findByUserIdAndRevokedFalseOrderByLastUsedAtDesc(user.getId());
		for (UserSession s : sessions) {
			if (!s.getId().equals(principal.getSessionId())) {
				s.setRevoked(true);
				userSessionRepository.save(s);
			}
		}
	}

	@Transactional(readOnly = true)
	public MeResponse getMe(UserPrincipal principal) {
		User user = userRepository.findById(principal.getId())
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));

		List<String> permissions = resolvePermissions(user.getRole().name());
		return new MeResponse(toUserSummary(user), null, permissions);
	}

	@Transactional
	public UserSummaryDto updateMe(UserPrincipal principal, UpdateMeRequest request) {
		User user = userRepository.findById(principal.getId())
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));

		if (request.phone() != null) {
			user.setPhone(request.phone());
		}
		if (request.preferredLanguage() != null) {
			user.setPreferredLanguage(request.preferredLanguage());
		}

		userRepository.save(user);
		return toUserSummary(user);
	}

	@Transactional(readOnly = true)
	public List<SessionDto> listSessions(UUID userId) {
		return userSessionRepository.findByUserIdAndRevokedFalseOrderByLastUsedAtDesc(userId).stream()
				.map(s -> new SessionDto(s.getId(), s.getIpAddress(), s.getDeviceInfo(), s.getLastUsedAt(),
						s.getExpiresAt()))
				.toList();
	}

	@Transactional
	public void revokeSession(UUID userId, UUID sessionId) {
		UserSession session = userSessionRepository.findById(sessionId)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Session not found."));

		if (!session.getUserId().equals(userId)) {
			throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Session not found.");
		}

		session.setRevoked(true);
		userSessionRepository.save(session);
	}

	public UserSummaryDto toUserSummary(User user) {
		return new UserSummaryDto(user.getId(), user.getEmail(), user.getRole(), user.getStatus(),
				user.isMustChangePassword(), user.getPhone(), user.getPreferredLanguage(), user.getCreatedAt(),
				user.getUpdatedAt(), user.getVersion());
	}

	private String generateRandomToken() {
		byte[] bytes = new byte[32];
		secureRandom.nextBytes(bytes);
		return java.util.HexFormat.of().formatHex(bytes);
	}

	private String hashToken(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return java.util.HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private String getClientIp(HttpServletRequest request) {
		String xf = request.getHeader("X-Forwarded-For");
		if (xf != null && !xf.isBlank()) {
			return xf.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	private List<String> resolvePermissions(String role) {
		return switch (role) {
			case "SUPER_ADMIN" -> List.of("*");
			case "ADMIN" ->
				List.of("academic:*", "class:*", "student:*", "teacher:*", "course:*", "offering:*", "attendance:*");
			case "TEACHER" -> List.of("class:read", "student:read", "course:read", "offering:read", "attendance:read",
					"attendance:mark");
			case "STUDENT" -> List.of("student:read:own", "class:read:own", "offering:read:own", "attendance:read:own",
					"excuse:create");
			default -> List.of();
		};
	}
}
