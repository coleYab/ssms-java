package com.example.ssms.security;

import com.example.ssms.auth.entity.User;
import com.example.ssms.auth.entity.UserSession;
import com.example.ssms.auth.repository.UserRepository;
import com.example.ssms.auth.repository.UserSessionRepository;
import com.example.ssms.common.constant.ErrorCode;
import com.example.ssms.common.dto.ApiErrorDetail;
import com.example.ssms.common.dto.ApiErrorResponse;
import com.example.ssms.security.jwt.JwtTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_WHEN_PASSWORD_CHANGE_REQUIRED = Set.of(
            "/api/v1/auth/change-password",
            "/api/v1/auth/logout",
            "/api/v1/auth/me"
    );

    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(
            JwtTokenService jwtTokenService,
            UserRepository userRepository,
            UserSessionRepository userSessionRepository,
            ObjectMapper objectMapper
    ) {
        this.jwtTokenService = jwtTokenService;
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = jwtTokenService.parseAndValidateToken(token);
            UUID userId = UUID.fromString(claims.getSubject());
            UUID sessionId = UUID.fromString(claims.get("sid", String.class));
            int tokenPermVersion = claims.get("permVersion", Integer.class);
            Instant tokenIssuedAt = claims.getIssuedAt().toInstant();

            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                writeError(response, request, ErrorCode.AUTH_TOKEN_INVALID, 401);
                return;
            }

            User user = userOpt.get();
            if (user.getDeletedAt() != null || user.getStatus() == AccountStatus.INACTIVE) {
                writeError(response, request, ErrorCode.AUTH_ACCOUNT_DISABLED, 403);
                return;
            }

            if (user.getStatus() == AccountStatus.LOCKED) {
                writeError(response, request, ErrorCode.AUTH_ACCOUNT_LOCKED, 403);
                return;
            }

            if (user.getPermVersion() != tokenPermVersion) {
                writeError(response, request, ErrorCode.AUTH_TOKEN_STALE, 401);
                return;
            }

            Optional<UserSession> sessionOpt = userSessionRepository.findById(sessionId);
            if (sessionOpt.isEmpty() || sessionOpt.get().isRevoked()) {
                writeError(response, request, ErrorCode.AUTH_SESSION_REVOKED, 401);
                return;
            }

            // Update session lastUsedAt
            UserSession session = sessionOpt.get();
            session.setLastUsedAt(Instant.now());
            userSessionRepository.save(session);

            UserPrincipal principal = new UserPrincipal(
                    user.getId(),
                    user.getEmail(),
                    user.getRole(),
                    user.getStatus(),
                    user.getPermVersion(),
                    sessionId,
                    user.isMustChangePassword(),
                    tokenIssuedAt,
                    user.getProfileType(),
                    user.getProfileId()
            );

            // Gate: mustChangePassword
            String path = request.getRequestURI();
            if (user.isMustChangePassword() && !ALLOWED_WHEN_PASSWORD_CHANGE_REQUIRED.contains(path)) {
                writeError(response, request, ErrorCode.AUTH_PASSWORD_CHANGE_REQUIRED, 403);
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            writeError(response, request, ErrorCode.AUTH_TOKEN_EXPIRED, 401);
        } catch (Exception e) {
            writeError(response, request, ErrorCode.AUTH_TOKEN_INVALID, 401);
        }
    }

    private void writeError(HttpServletResponse response, HttpServletRequest request, ErrorCode errorCode, int status) throws IOException {
        String requestId = (String) request.getAttribute("X-Request-ID");
        if (requestId == null) {
            requestId = request.getHeader("X-Request-ID");
        }
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }

        response.setStatus(status);
        response.setContentType("application/json; charset=utf-8");
        response.setHeader("X-Request-ID", requestId);

        ApiErrorDetail detail = new ApiErrorDetail(
                errorCode.name(),
                errorCode.getDefaultMessageTemplate(),
                status,
                null,
                null,
                requestId,
                Instant.now(),
                request.getRequestURI(),
                request.getMethod(),
                "https://docs.school.example.com/errors/" + errorCode.name()
        );

        response.getWriter().write(objectMapper.writeValueAsString(ApiErrorResponse.of(detail)));
    }
}
