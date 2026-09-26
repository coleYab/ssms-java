package com.example.ssms.auth.service;

import com.example.ssms.auth.entity.PasswordHistory;
import com.example.ssms.auth.repository.PasswordHistoryRepository;
import com.example.ssms.common.constant.ErrorCode;
import com.example.ssms.common.dto.FieldErrorDetail;
import com.example.ssms.common.exception.ApiException;
import com.example.ssms.security.crypto.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class PasswordService {

    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "password1234!", "Password1234!", "Admin123456!", "Welcome1234!", "School12345!"
    );

    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SYMBOL = Pattern.compile("[^a-zA-Z0-9]");

    private final Argon2PasswordEncoder passwordEncoder;
    private final PasswordHistoryRepository passwordHistoryRepository;

    public PasswordService(Argon2PasswordEncoder passwordEncoder, PasswordHistoryRepository passwordHistoryRepository) {
        this.passwordEncoder = passwordEncoder;
        this.passwordHistoryRepository = passwordHistoryRepository;
    }

    public void validatePasswordPolicy(String password, String email, String name, UUID userId) {
        if (password == null || password.length() < 12) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Password must be at least 12 characters long.")
                    .withDetail(FieldErrorDetail.body("password", "PASSWORD_TOO_WEAK", "Password must be at least 12 characters long."));
        }

        if (!UPPER.matcher(password).find() || !LOWER.matcher(password).find()
                || !DIGIT.matcher(password).find() || !SYMBOL.matcher(password).find()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Password must contain uppercase, lowercase, digit, and symbol.")
                    .withDetail(FieldErrorDetail.body("password", "PASSWORD_TOO_WEAK", "Password must contain uppercase, lowercase, digit, and symbol."));
        }

        if (COMMON_PASSWORDS.contains(password)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Password is too common.")
                    .withDetail(FieldErrorDetail.body("password", "PASSWORD_TOO_WEAK", "Password is too common."));
        }

        if (email != null && email.contains("@")) {
            String localPart = email.substring(0, email.indexOf('@')).toLowerCase();
            if (localPart.length() >= 3 && password.toLowerCase().contains(localPart)) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "Password must not contain email local-part.")
                        .withDetail(FieldErrorDetail.body("password", "PASSWORD_TOO_WEAK", "Password must not contain email local-part."));
            }
        }

        if (name != null && name.length() >= 3 && password.toLowerCase().contains(name.toLowerCase())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Password must not contain user's name.")
                    .withDetail(FieldErrorDetail.body("password", "PASSWORD_TOO_WEAK", "Password must not contain user's name."));
        }

        if (userId != null) {
            List<PasswordHistory> histories = passwordHistoryRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId);
            for (PasswordHistory history : histories) {
                if (passwordEncoder.matches(password, history.getPasswordHash())) {
                    throw new ApiException(ErrorCode.AUTH_PASSWORD_REUSED);
                }
            }
        }
    }

    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public void recordPassword(UUID userId, String passwordHash) {
        PasswordHistory history = new PasswordHistory();
        history.setUserId(userId);
        history.setPasswordHash(passwordHash);
        passwordHistoryRepository.save(history);
    }
}
