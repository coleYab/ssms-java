package com.example.ssms.common.filter;

import com.example.ssms.common.constant.ErrorCode;
import com.example.ssms.common.dto.ApiErrorDetail;
import com.example.ssms.common.dto.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {

	private final ObjectMapper objectMapper;
	private final ConcurrentHashMap<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

	public RateLimitFilter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	private static class RateLimitBucket {
		final int limit;
		final long windowMillis;
		final AtomicInteger count = new AtomicInteger(0);
		volatile long resetTime;

		RateLimitBucket(int limit, long windowMillis) {
			this.limit = limit;
			this.windowMillis = windowMillis;
			this.resetTime = System.currentTimeMillis() + windowMillis;
		}

		synchronized boolean tryConsume() {
			long now = System.currentTimeMillis();
			if (now > resetTime) {
				count.set(0);
				resetTime = now + windowMillis;
			}
			return count.incrementAndGet() <= limit;
		}

		long getSecondsUntilReset() {
			long remaining = resetTime - System.currentTimeMillis();
			return Math.max(1, remaining / 1000);
		}

		int getRemaining() {
			return Math.max(0, limit - count.get());
		}
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String uri = request.getRequestURI();
		String ip = getClientIp(request);

		String bucketKey;
		int limit;
		long windowMillis;

		if (uri.startsWith("/api/v1/auth/login") || uri.startsWith("/api/v1/auth/forgot-password")
				|| uri.startsWith("/api/v1/auth/reset-password")) {
			bucketKey = "auth:" + ip;
			limit = 10;
			windowMillis = 15 * 60 * 1000; // 15 min
		} else if (uri.startsWith("/api/v1/health")) {
			bucketKey = "health:" + ip;
			limit = 60;
			windowMillis = 60 * 1000; // 1 min
		} else if (uri.contains("/bulk-import") || uri.contains("/export")) {
			bucketKey = "bulk:" + ip;
			limit = 10;
			windowMillis = 60 * 60 * 1000; // 1 hour
		} else {
			bucketKey = "general:" + ip;
			limit = 600;
			windowMillis = 60 * 1000; // 1 min
		}

		RateLimitBucket bucket = buckets.computeIfAbsent(bucketKey, k -> new RateLimitBucket(limit, windowMillis));
		response.setHeader("X-RateLimit-Limit", String.valueOf(bucket.limit));
		response.setHeader("X-RateLimit-Remaining", String.valueOf(bucket.getRemaining()));
		response.setHeader("X-RateLimit-Reset",
				String.valueOf(System.currentTimeMillis() / 1000 + bucket.getSecondsUntilReset()));

		if (!bucket.tryConsume()) {
			long retryAfter = bucket.getSecondsUntilReset();
			response.setStatus(429);
			response.setContentType("application/json; charset=utf-8");
			response.setHeader("Retry-After", String.valueOf(retryAfter));

			String requestId = (String) request.getAttribute("X-Request-ID");
			if (requestId == null) {
				requestId = UUID.randomUUID().toString();
			}

			ApiErrorDetail detail = new ApiErrorDetail(ErrorCode.RATE_LIMIT_EXCEEDED.name(),
					String.format("Too many requests. Try again in %d seconds.", retryAfter), 429, null,
					Map.of("retryAfterSeconds", retryAfter), requestId, Instant.now(), uri, request.getMethod(),
					"https://docs.school.example.com/errors/" + ErrorCode.RATE_LIMIT_EXCEEDED.name());

			response.getWriter().write(objectMapper.writeValueAsString(ApiErrorResponse.of(detail)));
			return;
		}

		filterChain.doFilter(request, response);
	}

	private String getClientIp(HttpServletRequest request) {
		String xf = request.getHeader("X-Forwarded-For");
		if (xf != null && !xf.isBlank()) {
			return xf.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
