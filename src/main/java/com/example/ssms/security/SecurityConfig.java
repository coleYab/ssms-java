package com.example.ssms.security;

import com.example.ssms.common.constant.ErrorCode;
import com.example.ssms.common.dto.ApiErrorDetail;
import com.example.ssms.common.dto.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final ObjectMapper objectMapper;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.objectMapper = objectMapper;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable()).cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.headers(headers -> headers.contentTypeOptions(cto -> {
				}).cacheControl(cache -> {
				})).exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
					String requestId = (String) request.getAttribute("X-Request-ID");
					if (requestId == null) {
						requestId = request.getHeader("X-Request-ID");
					}
					if (requestId == null) {
						requestId = UUID.randomUUID().toString();
					}

					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					response.setContentType("application/json; charset=utf-8");
					response.setHeader("X-Request-ID", requestId);
					response.setHeader("WWW-Authenticate", "Bearer");

					ApiErrorDetail detail = new ApiErrorDetail(ErrorCode.AUTH_TOKEN_MISSING.name(),
							ErrorCode.AUTH_TOKEN_MISSING.getDefaultMessageTemplate(), 401, null, null, requestId,
							Instant.now(), request.getRequestURI(), request.getMethod(),
							"https://docs.school.example.com/errors/" + ErrorCode.AUTH_TOKEN_MISSING.name());
					response.getWriter().write(objectMapper.writeValueAsString(ApiErrorResponse.of(detail)));
				}).accessDeniedHandler((request, response, accessDeniedException) -> {
					String requestId = (String) request.getAttribute("X-Request-ID");
					if (requestId == null) {
						requestId = request.getHeader("X-Request-ID");
					}
					if (requestId == null) {
						requestId = UUID.randomUUID().toString();
					}

					response.setStatus(HttpServletResponse.SC_FORBIDDEN);
					response.setContentType("application/json; charset=utf-8");
					response.setHeader("X-Request-ID", requestId);

					ApiErrorDetail detail = new ApiErrorDetail(ErrorCode.AUTHZ_FORBIDDEN_ROLE.name(),
							ErrorCode.AUTHZ_FORBIDDEN_ROLE.getDefaultMessageTemplate(), 403, null, null, requestId,
							Instant.now(), request.getRequestURI(), request.getMethod(),
							"https://docs.school.example.com/errors/" + ErrorCode.AUTHZ_FORBIDDEN_ROLE.name());
					response.getWriter().write(objectMapper.writeValueAsString(ApiErrorResponse.of(detail)));
				}))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/forgot-password",
								"/api/v1/auth/reset-password", "/api/v1/health", "/api/v1/health/ready",
								"/.well-known/jwks.json", "/api/v1/openapi.json", "/api/v1/openapi.json/**",
								"/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
						.permitAll().anyRequest().authenticated())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOriginPatterns(List.of("*"));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Request-ID", "If-Match",
				"Idempotency-Key", "Accept-Language"));
		config.setExposedHeaders(List.of("X-Request-ID", "ETag", "Location", "X-RateLimit-Limit",
				"X-RateLimit-Remaining", "X-RateLimit-Reset", "Retry-After"));
		config.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
